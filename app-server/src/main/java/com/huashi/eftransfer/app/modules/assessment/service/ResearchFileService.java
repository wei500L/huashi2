package com.huashi.eftransfer.app.modules.assessment.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.common.audit.service.AuditLogService;
import com.huashi.eftransfer.app.modules.assessment.dto.AssessmentAttemptResponseRequest;
import com.huashi.eftransfer.app.modules.assessment.dto.ResearchFileInitiateRequest;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentAttemptAnswerEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentAttemptEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentParticipantEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentQuestionEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentSubmissionFileEntity;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentAttemptAnswerMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentQuestionMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentSubmissionFileMapper;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchAttachmentVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchFileInitiateVO;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.enums.AssessmentFileBindingStatus;
import com.huashi.eftransfer.shared.enums.AssessmentFileScanStatus;
import com.huashi.eftransfer.shared.enums.AssessmentFileStorageProvider;
import com.huashi.eftransfer.shared.enums.AssessmentQuestionType;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class ResearchFileService {

    private final ResearchAccessService accessService;
    private final ResearchAnalyticsProperties properties;
    private final PublicAssessmentService publicAssessmentService;
    private final AssessmentQuestionMapper questionMapper;
    private final AssessmentAttemptAnswerMapper answerMapper;
    private final AssessmentSubmissionFileMapper fileMapper;
    private final ObjectStorageService objectStorageService;
    private final AuditLogService auditLogService;
    private final SecureRandom secureRandom = new SecureRandom();

    public ResearchFileService(
            ResearchAccessService accessService,
            ResearchAnalyticsProperties properties,
            PublicAssessmentService publicAssessmentService,
            AssessmentQuestionMapper questionMapper,
            AssessmentAttemptAnswerMapper answerMapper,
            AssessmentSubmissionFileMapper fileMapper,
            ObjectStorageService objectStorageService,
            AuditLogService auditLogService
    ) {
        this.accessService = accessService;
        this.properties = properties;
        this.publicAssessmentService = publicAssessmentService;
        this.questionMapper = questionMapper;
        this.answerMapper = answerMapper;
        this.fileMapper = fileMapper;
        this.objectStorageService = objectStorageService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ResearchFileInitiateVO initiate(String releaseCode, String sessionToken, ResearchFileInitiateRequest request) {
        PublicAssessmentService.PublicSession session = publicAssessmentService.requirePublicSession(releaseCode, sessionToken, true);
        requireInProgress(session.attempt());
        AssessmentAttemptAnswerEntity answer = requireFileQuestion(session.attempt(), request.questionOrder());
        if (request.sizeBytes() > properties.getMaxFileBytes()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "Attachment exceeds the 20 MB file limit", 400);
        }
        String extension = AssessmentFileSignature.normalizeExtension(request.fileName());
        if (!AssessmentFileSignature.isAllowedExtension(extension)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "Attachment type is not allowed", 400);
        }
        assertQuestionCapacity(session.attempt().getId(), answer.getQuestionId(), 1);
        assertAttemptCapacity(session.attempt().getId(), request.sizeBytes());
        String token = newToken();
        AssessmentSubmissionFileEntity file = new AssessmentSubmissionFileEntity();
        file.setAttemptId(session.attempt().getId());
        file.setAnswerId(answer.getId());
        file.setQuestionId(answer.getQuestionId());
        file.setParticipantId(session.participant().getId());
        file.setUploadToken(token);
        file.setOriginalFileName(sanitizeFileName(request.fileName()));
        file.setStorageProvider(AssessmentFileStorageProvider.LOCAL.name());
        file.setObjectKey("pending/" + token);
        file.setMimeType(request.contentType() == null ? "application/octet-stream" : request.contentType());
        file.setFileExtension(extension);
        file.setSizeBytes(request.sizeBytes());
        file.setSha256("0".repeat(64));
        file.setScanStatus(AssessmentFileScanStatus.PENDING.name());
        file.setBindingStatus(AssessmentFileBindingStatus.TEMPORARY.name());
        file.setUploadedAt(LocalDateTime.now());
        fileMapper.insert(file);
        return new ResearchFileInitiateVO(token, file.getId(), properties.getMaxFileBytes(), properties.getMaxFilesPerQuestion());
    }

    @Transactional
    public ResearchAttachmentVO uploadContent(String releaseCode, String sessionToken, String uploadToken, byte[] content, String declaredType) {
        PublicAssessmentService.PublicSession session = publicAssessmentService.requirePublicSession(releaseCode, sessionToken, true);
        requireInProgress(session.attempt());
        AssessmentSubmissionFileEntity file = requireOwnedTemporary(session, uploadToken);
        if (content == null || content.length == 0) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "Attachment content is required", 400);
        }
        if (content.length > properties.getMaxFileBytes()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "Attachment exceeds the 20 MB file limit", 400);
        }
        byte[] header = content.length > 16 ? java.util.Arrays.copyOf(content, 16) : content;
        if (AssessmentFileSignature.isExecutableOrScript(header)) {
            file.setScanStatus(AssessmentFileScanStatus.INFECTED.name());
            fileMapper.updateById(file);
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "Attachment failed the security scan", 400);
        }
        String detected = AssessmentFileSignature.detectMime(file.getFileExtension(), header, declaredType);
        if (detected == null) {
            file.setScanStatus(AssessmentFileScanStatus.FAILED.name());
            fileMapper.updateById(file);
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "Attachment signature does not match the declared type", 400);
        }
        String digest = sha256(content);
        String objectKey = "research/" + session.attempt().getPublishId() + "/" + session.attempt().getId()
                + "/" + file.getQuestionId() + "/" + file.getUploadToken();
        objectStorageService.put(objectKey, new ByteArrayInputStream(content), content.length, detected);
        file.setObjectKey(objectKey);
        file.setMimeType(detected);
        file.setSizeBytes((long) content.length);
        file.setSha256(digest);
        file.setScanStatus(AssessmentFileScanStatus.CLEAN.name());
        file.setUploadedAt(LocalDateTime.now());
        fileMapper.updateById(file);
        return toPublicAttachment(file);
    }

    @Transactional
    public void deleteTemporary(String releaseCode, String sessionToken, String uploadToken) {
        PublicAssessmentService.PublicSession session = publicAssessmentService.requirePublicSession(releaseCode, sessionToken, true);
        requireInProgress(session.attempt());
        AssessmentSubmissionFileEntity file = requireOwnedTemporary(session, uploadToken);
        if (file.getObjectKey() != null && !file.getObjectKey().startsWith("pending/")) {
            objectStorageService.delete(file.getObjectKey());
        }
        file.setBindingStatus(AssessmentFileBindingStatus.DELETED.name());
        file.setDeletedAt(LocalDateTime.now());
        fileMapper.updateById(file);
    }

    @Transactional
    public void bindTokens(AssessmentAttemptEntity attempt, AssessmentParticipantEntity participant, List<AssessmentAttemptResponseRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }
        List<AssessmentAttemptAnswerEntity> answers = answerMapper.selectList(Wrappers.<AssessmentAttemptAnswerEntity>lambdaQuery()
                .eq(AssessmentAttemptAnswerEntity::getAttemptId, attempt.getId()));
        Map<Integer, AssessmentAttemptAnswerEntity> byOrder = answers.stream()
                .collect(java.util.stream.Collectors.toMap(AssessmentAttemptAnswerEntity::getQuestionOrder, item -> item, (left, right) -> left));
        for (AssessmentAttemptResponseRequest request : requests) {
            if (request.attachmentTokens() == null) {
                continue;
            }
            AssessmentAttemptAnswerEntity answer = byOrder.get(request.questionOrder());
            if (answer == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "Assessment question was not found", 404);
            }
            if (!AssessmentQuestionType.FILE_UPLOAD.name().equalsIgnoreCase(answer.getQuestionType())) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR, "Attachments are only allowed on FILE_UPLOAD questions", 400);
            }
            bindQuestion(attempt, participant, answer, request.attachmentTokens());
        }
        refreshFileAnswers(attempt.getId(), answers);
    }

    @Transactional(readOnly = true)
    public ResearchAttachmentVO metadata(Long fileId) {
        ResearchAccessService.ResearchFileAccess access = accessService.requireAccessibleResearchFile(fileId);
        return toTeacherAttachment(access.file());
    }

    @Transactional(readOnly = true)
    public ResponseEntity<StreamingResponseBody> download(Long fileId, boolean preview) {
        ResearchAccessService.ResearchFileAccess access = accessService.requireAccessibleResearchFile(fileId);
        AssessmentSubmissionFileEntity file = access.file();
        if (!AssessmentFileScanStatus.CLEAN.name().equalsIgnoreCase(file.getScanStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "Attachment is not available until the security scan succeeds", 409);
        }
        if (preview && !isPreviewable(file.getMimeType())) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "Only PDF and images can be previewed", 400);
        }
        auditLogService.record("RESEARCH_ATTACHMENT_DOWNLOADED", "ASSESSMENT_SUBMISSION_FILE", String.valueOf(file.getId()),
                Map.of("preview", preview, "publishId", access.attemptAccess().attempt().getPublishId()), "SUCCESS");
        StreamingResponseBody body = output -> {
            try (InputStream input = objectStorageService.open(file.getObjectKey())) {
                input.transferTo(output);
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to stream research attachment", exception);
            }
        };
        String disposition = (preview ? "inline" : "attachment")
                + "; filename=\"" + sanitizeFileName(file.getOriginalFileName()).replace("\"", "") + "\"";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .contentType(MediaType.parseMediaType(file.getMimeType()))
                .body(body);
    }

    @Scheduled(fixedDelayString = "PT30M")
    public void cleanupOrphans() {
        LocalDateTime deadline = LocalDateTime.now().minus(properties.getOrphanAfter());
        for (Long id : fileMapper.selectOrphanCandidateIds(deadline, 100)) {
            AssessmentSubmissionFileEntity file = fileMapper.selectById(id);
            if (file == null || !AssessmentFileBindingStatus.TEMPORARY.name().equals(file.getBindingStatus())) {
                continue;
            }
            if (file.getObjectKey() != null && !file.getObjectKey().startsWith("pending/")) {
                objectStorageService.delete(file.getObjectKey());
            }
            file.setBindingStatus(AssessmentFileBindingStatus.ORPHANED.name());
            file.setDeletedAt(LocalDateTime.now());
            fileMapper.updateById(file);
        }
    }

    public List<ResearchAttachmentVO> listPublicAttachments(Long attemptId, Long questionId) {
        return fileMapper.selectList(Wrappers.<AssessmentSubmissionFileEntity>lambdaQuery()
                        .eq(AssessmentSubmissionFileEntity::getAttemptId, attemptId)
                        .eq(AssessmentSubmissionFileEntity::getQuestionId, questionId)
                        .in(AssessmentSubmissionFileEntity::getBindingStatus,
                                AssessmentFileBindingStatus.TEMPORARY.name(), AssessmentFileBindingStatus.BOUND.name()))
                .stream()
                .map(this::toPublicAttachment)
                .toList();
    }

    private void bindQuestion(
            AssessmentAttemptEntity attempt,
            AssessmentParticipantEntity participant,
            AssessmentAttemptAnswerEntity answer,
            List<String> tokens
    ) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String token : tokens) {
            if (token != null && !token.isBlank()) {
                unique.add(token.trim());
            }
        }
        if (unique.size() > properties.getMaxFilesPerQuestion()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "A question accepts at most 5 attachments", 400);
        }
        List<AssessmentSubmissionFileEntity> existing = fileMapper.selectList(Wrappers.<AssessmentSubmissionFileEntity>lambdaQuery()
                .eq(AssessmentSubmissionFileEntity::getAttemptId, attempt.getId())
                .eq(AssessmentSubmissionFileEntity::getQuestionId, answer.getQuestionId())
                .in(AssessmentSubmissionFileEntity::getBindingStatus,
                        AssessmentFileBindingStatus.TEMPORARY.name(), AssessmentFileBindingStatus.BOUND.name()));
        Set<String> keep = unique;
        for (AssessmentSubmissionFileEntity file : existing) {
            if (!keep.contains(file.getUploadToken())) {
                file.setBindingStatus(AssessmentFileBindingStatus.DELETED.name());
                file.setDeletedAt(LocalDateTime.now());
                fileMapper.updateById(file);
            }
        }
        long boundBytes = 0;
        int boundCount = 0;
        for (String token : unique) {
            AssessmentSubmissionFileEntity file = fileMapper.selectOne(Wrappers.<AssessmentSubmissionFileEntity>lambdaQuery()
                    .eq(AssessmentSubmissionFileEntity::getUploadToken, token)
                    .last("LIMIT 1"));
            if (file == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "Attachment token was not found", 404);
            }
            if (!Objects.equals(file.getAttemptId(), attempt.getId())) {
                throw new BusinessException(ResultCode.FORBIDDEN, "Attachment does not belong to this attempt", 403);
            }
            if (!Objects.equals(file.getQuestionId(), answer.getQuestionId())) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR, "Attachment belongs to another question", 400);
            }
            if (AssessmentFileBindingStatus.DELETED.name().equals(file.getBindingStatus())) {
                throw new BusinessException(ResultCode.CONFLICT, "Attachment has already been removed", 409);
            }
            if (!AssessmentFileScanStatus.CLEAN.name().equals(file.getScanStatus())) {
                throw new BusinessException(ResultCode.CONFLICT, "Attachment cannot be bound before the scan succeeds", 409);
            }
            file.setAnswerId(answer.getId());
            file.setParticipantId(participant == null ? file.getParticipantId() : participant.getId());
            file.setBindingStatus(AssessmentFileBindingStatus.BOUND.name());
            file.setBoundAt(LocalDateTime.now());
            fileMapper.updateById(file);
            boundBytes += file.getSizeBytes() == null ? 0 : file.getSizeBytes();
            boundCount++;
        }
        if (boundCount > properties.getMaxFilesPerQuestion()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "A question accepts at most 5 attachments", 400);
        }
        assertAttemptCapacity(attempt.getId(), 0);
        if (boundBytes > properties.getMaxAttemptBytes()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "Attachments exceed the 50 MB attempt limit", 400);
        }
    }

    private void refreshFileAnswers(Long attemptId, List<AssessmentAttemptAnswerEntity> answers) {
        for (AssessmentAttemptAnswerEntity answer : answers) {
            if (!AssessmentQuestionType.FILE_UPLOAD.name().equalsIgnoreCase(answer.getQuestionType())) {
                continue;
            }
            long bound = fileMapper.selectCount(Wrappers.<AssessmentSubmissionFileEntity>lambdaQuery()
                    .eq(AssessmentSubmissionFileEntity::getAttemptId, attemptId)
                    .eq(AssessmentSubmissionFileEntity::getQuestionId, answer.getQuestionId())
                    .eq(AssessmentSubmissionFileEntity::getBindingStatus, AssessmentFileBindingStatus.BOUND.name()));
            answer.setAnswered(bound > 0);
            answer.setCorrect(null);
            answer.setScoreAwarded(null);
            answerMapper.updateResponseSnapshot(answer);
        }
    }

    private AssessmentAttemptAnswerEntity requireFileQuestion(AssessmentAttemptEntity attempt, Integer questionOrder) {
        AssessmentAttemptAnswerEntity answer = answerMapper.selectOne(Wrappers.<AssessmentAttemptAnswerEntity>lambdaQuery()
                .eq(AssessmentAttemptAnswerEntity::getAttemptId, attempt.getId())
                .eq(AssessmentAttemptAnswerEntity::getQuestionOrder, questionOrder)
                .last("LIMIT 1"));
        if (answer == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Assessment question was not found", 404);
        }
        if (!AssessmentQuestionType.FILE_UPLOAD.name().equalsIgnoreCase(answer.getQuestionType())) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "Attachments are only allowed on FILE_UPLOAD questions", 400);
        }
        AssessmentQuestionEntity question = questionMapper.selectById(answer.getQuestionId());
        if (question == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Assessment question was not found", 404);
        }
        return answer;
    }

    private AssessmentSubmissionFileEntity requireOwnedTemporary(PublicAssessmentService.PublicSession session, String uploadToken) {
        AssessmentSubmissionFileEntity file = fileMapper.selectOne(Wrappers.<AssessmentSubmissionFileEntity>lambdaQuery()
                .eq(AssessmentSubmissionFileEntity::getUploadToken, uploadToken)
                .last("LIMIT 1"));
        if (file == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Attachment token was not found", 404);
        }
        if (!Objects.equals(file.getAttemptId(), session.attempt().getId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "Attachment does not belong to this attempt", 403);
        }
        if (AssessmentFileBindingStatus.DELETED.name().equals(file.getBindingStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "Attachment has already been removed", 409);
        }
        return file;
    }

    private void assertQuestionCapacity(Long attemptId, Long questionId, int incoming) {
        Long current = fileMapper.selectCount(Wrappers.<AssessmentSubmissionFileEntity>lambdaQuery()
                .eq(AssessmentSubmissionFileEntity::getAttemptId, attemptId)
                .eq(AssessmentSubmissionFileEntity::getQuestionId, questionId)
                .in(AssessmentSubmissionFileEntity::getBindingStatus,
                        AssessmentFileBindingStatus.TEMPORARY.name(), AssessmentFileBindingStatus.BOUND.name()));
        if ((current == null ? 0 : current) + incoming > properties.getMaxFilesPerQuestion()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "A question accepts at most 5 attachments", 400);
        }
    }

    private void assertAttemptCapacity(Long attemptId, long incomingBytes) {
        List<AssessmentSubmissionFileEntity> files = fileMapper.selectList(Wrappers.<AssessmentSubmissionFileEntity>lambdaQuery()
                .eq(AssessmentSubmissionFileEntity::getAttemptId, attemptId)
                .in(AssessmentSubmissionFileEntity::getBindingStatus,
                        AssessmentFileBindingStatus.TEMPORARY.name(), AssessmentFileBindingStatus.BOUND.name()));
        long total = files.stream().mapToLong(file -> file.getSizeBytes() == null ? 0 : file.getSizeBytes()).sum() + incomingBytes;
        if (total > properties.getMaxAttemptBytes()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "Attachments exceed the 50 MB attempt limit", 400);
        }
    }

    private void requireInProgress(AssessmentAttemptEntity attempt) {
        if (!"IN_PROGRESS".equalsIgnoreCase(attempt.getStatus())) {
            throw new BusinessException(ResultCode.ATTEMPT_SUBMITTED, "Submitted attempts cannot change attachments", 409);
        }
    }

    private ResearchAttachmentVO toPublicAttachment(AssessmentSubmissionFileEntity file) {
        return new ResearchAttachmentVO(
                file.getId(),
                file.getUploadToken(),
                file.getOriginalFileName(),
                file.getMimeType(),
                file.getFileExtension(),
                file.getSizeBytes(),
                file.getScanStatus(),
                file.getBindingStatus(),
                file.getUploadedAt(),
                false
        );
    }

    private ResearchAttachmentVO toTeacherAttachment(AssessmentSubmissionFileEntity file) {
        boolean downloadable = AssessmentFileScanStatus.CLEAN.name().equalsIgnoreCase(file.getScanStatus());
        return new ResearchAttachmentVO(
                file.getId(),
                null,
                file.getOriginalFileName(),
                file.getMimeType(),
                file.getFileExtension(),
                file.getSizeBytes(),
                file.getScanStatus(),
                file.getBindingStatus(),
                file.getUploadedAt(),
                downloadable
        );
    }

    private boolean isPreviewable(String mimeType) {
        return mimeType != null && (mimeType.equals("application/pdf") || mimeType.startsWith("image/"));
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "attachment";
        }
        return fileName.replaceAll("[\\\\/\\r\\n]", "_");
    }

    private String newToken() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (Exception exception) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }
}
