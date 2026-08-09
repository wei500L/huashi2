package com.huashi.eftransfer.app.modules.assessment.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.common.util.SecurityUtils;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentPaperEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentParticipantAccessEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentParticipantEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentParticipationCodeEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentPublicReleaseEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentPublishEntity;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentPaperMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentParticipantAccessMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentParticipantMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentParticipationCodeMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentPublicReleaseMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentPublishMapper;
import com.huashi.eftransfer.app.modules.assessment.support.AssessmentParticipantAccessCipher;
import com.huashi.eftransfer.app.modules.assessment.support.AssessmentParticipantCodeCodec;
import com.huashi.eftransfer.app.modules.assessment.vo.ParticipationCodeBatchCreatedVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ParticipationCodeBatchSummaryVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ParticipationCodeItemVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ParticipationCodeRevokeResultVO;
import com.huashi.eftransfer.app.modules.assessment.vo.PublicAssessmentReleaseSummaryVO;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.enums.AssessmentDeliveryMode;
import com.huashi.eftransfer.shared.enums.AssessmentPaperPurpose;
import com.huashi.eftransfer.shared.exception.BusinessException;
import com.huashi.eftransfer.shared.page.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class AssessmentPublicReleaseManagementService {
    private static final Set<String> CODE_STATUSES = Set.of("UNUSED", "IN_PROGRESS", "SUBMITTED", "REVOKED");
    private static final String LEGACY_BATCH = "legacy";
    private static final int INSERT_BATCH_SIZE = 500;

    private final AssessmentPublicReleaseMapper publicReleaseMapper;
    private final AssessmentPublishMapper publishMapper;
    private final AssessmentPaperMapper paperMapper;
    private final AssessmentParticipationCodeMapper participationCodeMapper;
    private final AssessmentParticipantMapper participantMapper;
    private final AssessmentParticipantAccessMapper accessMapper;
    private final AssessmentParticipantCodeCodec codeCodec;
    private final AssessmentParticipantAccessCipher accessCipher;

    public AssessmentPublicReleaseManagementService(
            AssessmentPublicReleaseMapper publicReleaseMapper,
            AssessmentPublishMapper publishMapper,
            AssessmentPaperMapper paperMapper,
            AssessmentParticipationCodeMapper participationCodeMapper,
            AssessmentParticipantMapper participantMapper,
            AssessmentParticipantAccessMapper accessMapper,
            AssessmentParticipantCodeCodec codeCodec,
            AssessmentParticipantAccessCipher accessCipher
    ) {
        this.publicReleaseMapper = publicReleaseMapper;
        this.publishMapper = publishMapper;
        this.paperMapper = paperMapper;
        this.participationCodeMapper = participationCodeMapper;
        this.participantMapper = participantMapper;
        this.accessMapper = accessMapper;
        this.codeCodec = codeCodec;
        this.accessCipher = accessCipher;
    }

    public List<PublicAssessmentReleaseSummaryVO> listReleases() {
        Long userId = currentUserId();
        boolean admin = isAdmin();
        return publicReleaseMapper.selectList(Wrappers.<AssessmentPublicReleaseEntity>lambdaQuery()
                        .orderByDesc(AssessmentPublicReleaseEntity::getId))
                .stream()
                .map(release -> loadBundle(release, userId, admin))
                .filter(Objects::nonNull)
                .map(this::toSummary)
                .toList();
    }

    public PageResult<ParticipationCodeItemVO> listCodes(
            Long publishId,
            String status,
            String batchId,
            int pageNo,
            int pageSize
    ) {
        ReleaseBundle bundle = requireRelease(publishId);
        int safePage = Math.max(1, pageNo);
        int safeSize = Math.max(1, Math.min(100, pageSize));
        String normalizedStatus = normalizeStatus(status);
        String normalizedBatchId = batchId == null || batchId.isBlank() ? null : batchId.trim();
        boolean legacyBatch = LEGACY_BATCH.equalsIgnoreCase(normalizedBatchId);
        if (legacyBatch) normalizedBatchId = null;
        long total = participationCodeMapper.countForManagement(
                bundle.release().getId(), normalizedStatus, normalizedBatchId, legacyBatch);
        long offset = (long) (safePage - 1) * safeSize;
        List<AssessmentParticipationCodeEntity> page = offset >= total ? List.of()
                : participationCodeMapper.selectManagementPage(bundle.release().getId(), normalizedStatus,
                normalizedBatchId, legacyBatch, safeSize, offset);
        Map<Long, AssessmentParticipantAccessEntity> latestAccess = latestCodeAccess(page);
        List<ParticipationCodeItemVO> records = page.stream().map(code -> {
            AssessmentParticipantAccessEntity access = latestAccess.get(code.getId());
            String ip = access == null ? null : accessCipher.decrypt(
                    access.getIpCiphertext(), access.getIpIv(), access.getIpKeyVersion());
            return new ParticipationCodeItemVO(code.getId(), code.getCodeHint(), code.getStatus(),
                    code.getExportBatchId(), code.getExportedAt(), code.getFirstVerifiedAt(),
                    code.getLastVerifiedAt(), code.getSubmittedAt(), ip);
        }).toList();
        return new PageResult<>(total, safePage, safeSize, records);
    }

    @Transactional
    public ParticipationCodeBatchCreatedVO createBatch(Long publishId, int count) {
        if (count < 1 || count > 5000) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR,
                    "Participation-code batch count must be between 1 and 5000", 400);
        }
        ReleaseBundle bundle = requireRelease(publishId);
        String batchId = UUID.randomUUID().toString();
        LocalDateTime generatedAt = LocalDateTime.now();
        LinkedHashSet<String> plainCodes = new LinkedHashSet<>();
        while (plainCodes.size() < count) plainCodes.add(codeCodec.generate());
        List<AssessmentParticipationCodeEntity> codes = new ArrayList<>(plainCodes.size());
        for (String plainCode : plainCodes) {
            AssessmentParticipationCodeEntity code = new AssessmentParticipationCodeEntity();
            code.setPublicReleaseId(bundle.release().getId());
            code.setCodeDigest(codeCodec.digest(plainCode));
            code.setCodeHint(plainCode.substring(plainCode.length() - 4));
            code.setStatus("UNUSED");
            code.setExportBatchId(batchId);
            code.setExportedAt(generatedAt);
            codes.add(code);
        }
        for (int start = 0; start < codes.size(); start += INSERT_BATCH_SIZE) {
            participationCodeMapper.insertBatch(codes.subList(start, Math.min(codes.size(), start + INSERT_BATCH_SIZE)));
        }
        if (publicReleaseMapper.incrementCodeCount(bundle.release().getId(), count) != 1) {
            throw new BusinessException(ResultCode.CONFLICT, "Public assessment release changed while generating codes", 409);
        }
        return new ParticipationCodeBatchCreatedVO(batchId, generatedAt, List.copyOf(plainCodes));
    }

    @Transactional
    public ParticipationCodeRevokeResultVO revokeCode(Long publishId, Long codeId) {
        ReleaseBundle bundle = requireRelease(publishId);
        AssessmentParticipationCodeEntity code = participationCodeMapper.selectById(codeId);
        if (code == null || !Objects.equals(code.getPublicReleaseId(), bundle.release().getId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Participation code was not found", 404);
        }
        if (!"UNUSED".equals(code.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "Only unused participation codes can be revoked", 409);
        }
        if (participationCodeMapper.revokeUnused(bundle.release().getId(), codeId) != 1) {
            throw new BusinessException(ResultCode.CONFLICT, "Participation code is no longer unused", 409);
        }
        return new ParticipationCodeRevokeResultVO(1);
    }

    @Transactional
    public ParticipationCodeRevokeResultVO revokeBatch(Long publishId, String batchId) {
        ReleaseBundle bundle = requireRelease(publishId);
        if (batchId == null || batchId.isBlank()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "Batch id is required", 400);
        }
        int updated = participationCodeMapper.update(null,
                Wrappers.<AssessmentParticipationCodeEntity>lambdaUpdate()
                        .set(AssessmentParticipationCodeEntity::getStatus, "REVOKED")
                        .eq(AssessmentParticipationCodeEntity::getPublicReleaseId, bundle.release().getId())
                        .eq(AssessmentParticipationCodeEntity::getExportBatchId, batchId.trim())
                        .eq(AssessmentParticipationCodeEntity::getStatus, "UNUSED"));
        return new ParticipationCodeRevokeResultVO(updated);
    }

    @Transactional
    public PublicAssessmentReleaseSummaryVO updateQrEntry(Long publishId, boolean enabled) {
        ReleaseBundle bundle = requireRelease(publishId);
        bundle.release().setQrEntryEnabled(enabled);
        publicReleaseMapper.updateById(bundle.release());
        return toSummary(bundle);
    }

    private PublicAssessmentReleaseSummaryVO toSummary(ReleaseBundle bundle) {
        List<AssessmentParticipationCodeEntity> codes = participationCodeMapper.selectList(
                Wrappers.<AssessmentParticipationCodeEntity>lambdaQuery()
                        .eq(AssessmentParticipationCodeEntity::getPublicReleaseId, bundle.release().getId())
                        .orderByDesc(AssessmentParticipationCodeEntity::getId));
        Map<String, List<AssessmentParticipationCodeEntity>> grouped = new LinkedHashMap<>();
        for (AssessmentParticipationCodeEntity code : codes) {
            grouped.computeIfAbsent(code.getExportBatchId() == null ? LEGACY_BATCH : code.getExportBatchId(), ignored -> new ArrayList<>())
                    .add(code);
        }
        List<ParticipationCodeBatchSummaryVO> batches = grouped.entrySet().stream().map(entry -> {
            List<AssessmentParticipationCodeEntity> items = entry.getValue();
            LocalDateTime generatedAt = items.stream().map(AssessmentParticipationCodeEntity::getExportedAt)
                    .filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(null);
            return new ParticipationCodeBatchSummaryVO(entry.getKey(), generatedAt, items.size(),
                    countStatus(items, "UNUSED"), countStatus(items, "IN_PROGRESS"),
                    countStatus(items, "SUBMITTED"), countStatus(items, "REVOKED"));
        }).sorted(Comparator.comparing(ParticipationCodeBatchSummaryVO::generatedAt,
                Comparator.nullsLast(Comparator.reverseOrder()))).toList();
        long qrParticipants = participantMapper.selectCount(Wrappers.<AssessmentParticipantEntity>lambdaQuery()
                .eq(AssessmentParticipantEntity::getPublishId, bundle.publish().getId())
                .eq(AssessmentParticipantEntity::getParticipantType, "PUBLIC_QR"));
        return new PublicAssessmentReleaseSummaryVO(bundle.publish().getId(), bundle.paper().getId(),
                bundle.paper().getPaperCode(), bundle.publish().getPaperTitleSnapshot(), bundle.release().getReleaseCode(),
                bundle.release().getStatus(), bundle.publish().getPublishedAt(),
                Boolean.TRUE.equals(bundle.release().getQrEntryEnabled()), codes.size(),
                countStatus(codes, "UNUSED"), countStatus(codes, "IN_PROGRESS"),
                countStatus(codes, "SUBMITTED"), countStatus(codes, "REVOKED"), qrParticipants, batches);
    }

    private Map<Long, AssessmentParticipantAccessEntity> latestCodeAccess(List<AssessmentParticipationCodeEntity> codes) {
        if (codes.isEmpty()) return Map.of();
        List<Long> ids = codes.stream().map(AssessmentParticipationCodeEntity::getId).toList();
        Map<Long, AssessmentParticipantAccessEntity> result = new LinkedHashMap<>();
        accessMapper.selectList(Wrappers.<AssessmentParticipantAccessEntity>lambdaQuery()
                        .in(AssessmentParticipantAccessEntity::getParticipationCodeId, ids)
                        .orderByDesc(AssessmentParticipantAccessEntity::getAccessedAt)
                        .orderByDesc(AssessmentParticipantAccessEntity::getId))
                .forEach(access -> result.putIfAbsent(access.getParticipationCodeId(), access));
        return result;
    }

    private long countStatus(List<AssessmentParticipationCodeEntity> codes, String status) {
        return codes.stream().filter(code -> status.equals(code.getStatus())).count();
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) return null;
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!CODE_STATUSES.contains(normalized)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "Unsupported participation-code status", 400);
        }
        return normalized;
    }

    private ReleaseBundle requireRelease(Long publishId) {
        AssessmentPublishEntity publish = publishMapper.selectById(publishId);
        if (publish == null || !AssessmentDeliveryMode.PUBLIC_CODE.name().equals(publish.getDeliveryMode())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Public assessment release was not found", 404);
        }
        AssessmentPaperEntity paper = paperMapper.selectById(publish.getPaperId());
        if (paper == null || AssessmentPaperPurpose.fromCode(paper.getPaperPurpose()) != AssessmentPaperPurpose.RESEARCH_SURVEY) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Public assessment release was not found", 404);
        }
        if (!isAdmin() && !Objects.equals(paper.getOwnerUserId(), currentUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "You do not have access to this public release", 403);
        }
        AssessmentPublicReleaseEntity release = publicReleaseMapper.selectOne(
                Wrappers.<AssessmentPublicReleaseEntity>lambdaQuery()
                        .eq(AssessmentPublicReleaseEntity::getPublishId, publishId).last("LIMIT 1"));
        if (release == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Public assessment release was not found", 404);
        }
        return new ReleaseBundle(release, publish, paper);
    }

    private ReleaseBundle loadBundle(AssessmentPublicReleaseEntity release, Long userId, boolean admin) {
        AssessmentPublishEntity publish = publishMapper.selectById(release.getPublishId());
        if (publish == null || !AssessmentDeliveryMode.PUBLIC_CODE.name().equals(publish.getDeliveryMode())) return null;
        AssessmentPaperEntity paper = paperMapper.selectById(publish.getPaperId());
        if (paper == null || AssessmentPaperPurpose.fromCode(paper.getPaperPurpose()) != AssessmentPaperPurpose.RESEARCH_SURVEY) return null;
        if (!admin && !Objects.equals(paper.getOwnerUserId(), userId)) return null;
        return new ReleaseBundle(release, publish, paper);
    }

    private Long currentUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "Authentication required", 401));
    }

    private boolean isAdmin() {
        return SecurityUtils.getCurrentPrincipal().map(principal -> principal.roles().contains("ADMIN")).orElse(false);
    }

    private record ReleaseBundle(
            AssessmentPublicReleaseEntity release,
            AssessmentPublishEntity publish,
            AssessmentPaperEntity paper
    ) { }
}
