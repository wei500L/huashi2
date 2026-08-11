package com.huashi.eftransfer.app.modules.assessment.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.modules.assessment.dto.PublicAssessmentVerifyRequest;
import com.huashi.eftransfer.app.modules.assessment.dto.PublicAssessmentQrEntryRequest;
import com.huashi.eftransfer.app.modules.assessment.dto.PublicAssessmentTimingRequest;
import com.huashi.eftransfer.app.modules.assessment.dto.AssessmentAttemptResponseRequest;
import com.huashi.eftransfer.app.modules.assessment.dto.SaveAssessmentResponsesRequest;
import com.huashi.eftransfer.app.modules.assessment.dto.SubmitAssessmentAttemptRequest;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentAiAnalysisEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentAttemptAnswerEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentAttemptEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentMetricSnapshotEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentParticipantEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentParticipantAccessEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentParticipantSessionEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentParticipationCodeEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentPublicReleaseEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentPublishEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentQuestionEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentTimingEventEntity;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentAiAnalysisMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentAttemptAnswerMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentAttemptMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentMetricSnapshotMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentParticipantMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentParticipantAccessMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentParticipantSessionMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentParticipationCodeMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentPublicReleaseMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentPublishMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentQuestionMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentTimingEventMapper;
import com.huashi.eftransfer.app.modules.assessment.support.AssessmentJsonCodec;
import com.huashi.eftransfer.app.modules.assessment.support.AssessmentClientIpNormalizer;
import com.huashi.eftransfer.app.modules.assessment.support.AssessmentParticipantAccessCipher;
import com.huashi.eftransfer.app.modules.assessment.support.AssessmentParticipantCodeCodec;
import com.huashi.eftransfer.app.modules.assessment.support.AssessmentParticipantProfileCipher;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentOptionVO;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentAttemptProgressVO;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentAttemptResultQuestionVO;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentAttemptSubmitVO;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentDimensionMetricVO;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentAiAnalysisVO;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentMetricSnapshotVO;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentReactionTimeMetricVO;
import com.huashi.eftransfer.app.modules.assessment.vo.PublicAssessmentAttemptVO;
import com.huashi.eftransfer.app.modules.assessment.vo.PublicAssessmentMetadataVO;
import com.huashi.eftransfer.app.modules.assessment.vo.PublicAssessmentResultVO;
import com.huashi.eftransfer.app.modules.assessment.vo.PublicAssessmentQuestionVO;
import com.huashi.eftransfer.app.modules.assessment.vo.PublicAssessmentSessionVO;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.enums.AssessmentAttemptStatus;
import com.huashi.eftransfer.shared.enums.AssessmentPublishStatus;
import com.huashi.eftransfer.shared.enums.AssessmentQuestionType;
import com.huashi.eftransfer.shared.enums.AssessmentSubmitReason;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PublicAssessmentService {

    private static final Logger log = LoggerFactory.getLogger(PublicAssessmentService.class);
    private static final int VERIFY_LIMIT = 10;
    private static final Duration VERIFY_WINDOW = Duration.ofMinutes(10);
    private static final Duration MAX_SESSION_TTL = Duration.ofHours(12);

    private final AssessmentPublicReleaseMapper publicReleaseMapper;
    private final AssessmentParticipationCodeMapper participationCodeMapper;
    private final AssessmentParticipantMapper participantMapper;
    private final AssessmentParticipantAccessMapper participantAccessMapper;
    private final AssessmentParticipantSessionMapper participantSessionMapper;
    private final AssessmentPublishMapper publishMapper;
    private final AssessmentQuestionMapper questionMapper;
    private final AssessmentAttemptMapper attemptMapper;
    private final AssessmentAttemptAnswerMapper answerMapper;
    private final AssessmentTimingEventMapper timingEventMapper;
    private final AssessmentMetricSnapshotMapper metricSnapshotMapper;
    private final AssessmentAiAnalysisMapper aiAnalysisMapper;
    private final AssessmentParticipantCodeCodec codeCodec;
    private final AssessmentJsonCodec jsonCodec;
    private final AssessmentParticipantProfileCipher profileCipher;
    private final AssessmentParticipantAccessCipher accessCipher;
    private final Duration configuredSessionTtl;
    private final AssessmentTimeoutProperties timeoutProperties;
    private final JdbcTemplate jdbcTemplate;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, Deque<LocalDateTime>> verificationAttempts = new ConcurrentHashMap<>();

    public PublicAssessmentService(
            AssessmentPublicReleaseMapper publicReleaseMapper,
            AssessmentParticipationCodeMapper participationCodeMapper,
            AssessmentParticipantMapper participantMapper,
            AssessmentParticipantAccessMapper participantAccessMapper,
            AssessmentParticipantSessionMapper participantSessionMapper,
            AssessmentPublishMapper publishMapper,
            AssessmentQuestionMapper questionMapper,
            AssessmentAttemptMapper attemptMapper,
            AssessmentAttemptAnswerMapper answerMapper,
            AssessmentTimingEventMapper timingEventMapper,
            AssessmentMetricSnapshotMapper metricSnapshotMapper,
            AssessmentAiAnalysisMapper aiAnalysisMapper,
            AssessmentParticipantCodeCodec codeCodec,
            AssessmentJsonCodec jsonCodec,
            AssessmentParticipantProfileCipher profileCipher,
            AssessmentParticipantAccessCipher accessCipher,
            AssessmentTimeoutProperties timeoutProperties,
            JdbcTemplate jdbcTemplate,
            @Value("${app.assessment.public-delivery.session-ttl:PT12H}") Duration configuredSessionTtl
    ) {
        this.publicReleaseMapper = publicReleaseMapper;
        this.participationCodeMapper = participationCodeMapper;
        this.participantMapper = participantMapper;
        this.participantAccessMapper = participantAccessMapper;
        this.participantSessionMapper = participantSessionMapper;
        this.publishMapper = publishMapper;
        this.questionMapper = questionMapper;
        this.attemptMapper = attemptMapper;
        this.answerMapper = answerMapper;
        this.timingEventMapper = timingEventMapper;
        this.metricSnapshotMapper = metricSnapshotMapper;
        this.aiAnalysisMapper = aiAnalysisMapper;
        this.codeCodec = codeCodec;
        this.jsonCodec = jsonCodec;
        this.profileCipher = profileCipher;
        this.accessCipher = accessCipher;
        this.timeoutProperties = timeoutProperties;
        this.jdbcTemplate = jdbcTemplate;
        this.configuredSessionTtl = configuredSessionTtl == null || configuredSessionTtl.isNegative()
                ? MAX_SESSION_TTL : configuredSessionTtl.compareTo(MAX_SESSION_TTL) > 0 ? MAX_SESSION_TTL : configuredSessionTtl;
    }

    public PublicAssessmentMetadataVO metadata(String releaseCode) {
        ReleaseBundle bundle = requireRelease(releaseCode);
        return new PublicAssessmentMetadataVO(bundle.release().getReleaseCode(), bundle.publish().getPaperTitleSnapshot(),
                bundle.publish().getPaperDescriptionSnapshot(), bundle.publish().getInstructionsText(),
                bundle.publish().getDurationMinutes(), bundle.publish().getQuestionCountSnapshot(), bundle.release().getStatus(),
                bundle.publish().getStartsAt(), bundle.publish().getDueAt(),
                Boolean.TRUE.equals(bundle.release().getQrEntryEnabled()));
    }

    @Transactional
    public VerifiedSession verify(String releaseCode, PublicAssessmentVerifyRequest request, String remoteAddress) {
        enforceRateLimit(remoteAddress);
        ReleaseBundle bundle = requireRelease(releaseCode);
        requireOpen(bundle.publish(), LocalDateTime.now());
        String digest;
        try {
            digest = codeCodec.digest(request.participationCode());
        } catch (IllegalArgumentException exception) {
            throw invalidCode();
        }
        AssessmentParticipationCodeEntity participationCode = participationCodeMapper.selectByReleaseAndDigestForUpdate(
                bundle.release().getId(), digest);
        if (participationCode == null) {
            throw invalidCode();
        }
        if ("REVOKED".equals(participationCode.getStatus())) {
            throw invalidCode();
        }
        AssessmentParticipantEntity participant = participantMapper.selectOne(
                Wrappers.<AssessmentParticipantEntity>lambdaQuery()
                        .eq(AssessmentParticipantEntity::getPublishId, bundle.publish().getId())
                        .eq(AssessmentParticipantEntity::getParticipationCodeId, participationCode.getId())
                        .last("LIMIT 1"));
        boolean resumed = participant != null;
        if (participant == null) {
            participant = new AssessmentParticipantEntity();
            participant.setPublishId(bundle.publish().getId());
            participant.setParticipantType("PUBLIC_CODE");
            participant.setParticipationCodeId(participationCode.getId());
            participantMapper.insert(participant);
        }
        if (request.basicInfo() != null && !request.basicInfo().isEmpty()) {
            AssessmentParticipantProfileCipher.EncryptedProfile encrypted = profileCipher.encrypt(request.basicInfo());
            participant.setSensitiveProfileCiphertext(encrypted.ciphertext());
            participant.setSensitiveProfileIv(encrypted.iv());
            participant.setSensitiveProfileKeyVersion(encrypted.keyVersion());
            participant.setConsentedAt(LocalDateTime.now());
            participantMapper.updateById(participant);
        }
        AssessmentAttemptEntity attempt = participant.getAttemptId() == null ? null : attemptMapper.selectById(participant.getAttemptId());
        if (attempt == null) {
            attempt = createAttempt(bundle.publish(), participant);
            participant.setAttemptId(attempt.getId());
            participantMapper.updateById(participant);
        }
        LocalDateTime now = LocalDateTime.now();
        participationCode.setFirstVerifiedAt(participationCode.getFirstVerifiedAt() == null ? now : participationCode.getFirstVerifiedAt());
        participationCode.setLastVerifiedAt(now);
        if (!"SUBMITTED".equals(participationCode.getStatus())) {
            participationCode.setStatus("IN_PROGRESS");
        }
        participationCodeMapper.updateById(participationCode);
        recordAccess(bundle.release(), participant, participationCode, "PUBLIC_CODE", remoteAddress, now);
        return issueSession(bundle, participant, attempt, resumed, now);
    }

    @Transactional
    public VerifiedSession enterByQr(String releaseCode, PublicAssessmentQrEntryRequest request, String remoteAddress) {
        String normalizedIp = AssessmentClientIpNormalizer.normalize(remoteAddress);
        if (normalizedIp == null) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR,
                    "A valid client IP address is required for QR entry", 400);
        }
        enforceRateLimit("qr:" + normalizedIp);
        ReleaseBundle bundle = requireRelease(releaseCode);
        requireOpen(bundle.publish(), LocalDateTime.now());
        if (!Boolean.TRUE.equals(bundle.release().getQrEntryEnabled())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "QR entry is not enabled for this release", 403);
        }
        String fingerprint = normalizeFingerprint(request.browserFingerprint());
        String fingerprintDigest = codeCodec.digestOpaque("qr-fingerprint:" + fingerprint);
        AssessmentParticipantEntity participant = selectQrParticipantForUpdate(bundle.publish().getId(), fingerprintDigest);
        boolean resumed = participant != null;
        if (participant == null) {
            participant = new AssessmentParticipantEntity();
            participant.setPublishId(bundle.publish().getId());
            participant.setParticipantType("PUBLIC_QR");
            participant.setBrowserFingerprintDigest(fingerprintDigest);
            try {
                participantMapper.insert(participant);
            } catch (DataIntegrityViolationException exception) {
                participant = selectQrParticipantForUpdate(bundle.publish().getId(), fingerprintDigest);
                if (participant == null) throw exception;
                resumed = true;
            }
        }
        AssessmentAttemptEntity attempt = participant.getAttemptId() == null
                ? null : attemptMapper.selectById(participant.getAttemptId());
        if (attempt == null) {
            attempt = createAttempt(bundle.publish(), participant);
            participant.setAttemptId(attempt.getId());
            participantMapper.updateById(participant);
        }
        LocalDateTime now = LocalDateTime.now();
        recordAccess(bundle.release(), participant, null, "PUBLIC_QR", normalizedIp, now);
        return issueSession(bundle, participant, attempt, resumed, now);
    }

    @Transactional
    public PublicAssessmentAttemptVO restore(String releaseCode, String sessionToken) {
        SessionBundle session = requireSession(releaseCode, sessionToken);
        session.session().setLastSeenAt(LocalDateTime.now());
        participantSessionMapper.updateById(session.session());
        return toAttempt(session.release(), session.attempt());
    }

    @Transactional
    public AssessmentAttemptProgressVO saveResponses(
            String releaseCode,
            String sessionToken,
            SaveAssessmentResponsesRequest request
    ) {
        SessionBundle session = requireSessionForUpdate(releaseCode, sessionToken);
        AssessmentAttemptEntity attempt = session.attempt();
        requireInProgress(attempt);
        requireVersion(attempt, request.baseVersion());
        List<AssessmentAttemptAnswerEntity> answers = loadAnswers(attempt.getId());
        applyResponses(request.responses(), answers, loadQuestionMap(attempt.getPaperId()), false);
        recomputeProgress(attempt, answers, LocalDateTime.now());
        if (attemptMapper.updateProgressIfInProgress(attempt) == 0) {
            throw versionConflict();
        }
        attempt.setVersion(attempt.getVersion() + 1);
        touchSession(session.session());
        return new AssessmentAttemptProgressVO(attempt.getId(), AssessmentAttemptStatus.IN_PROGRESS,
                attempt.getAnsweredCount(), attempt.getLastSavedAt(), attempt.getVersion());
    }

    @Transactional
    public void recordTiming(
            String releaseCode,
            String sessionToken,
            PublicAssessmentTimingRequest request
    ) {
        SessionBundle session = requireSession(releaseCode, sessionToken);
        requireInProgress(session.attempt());
        AssessmentAttemptAnswerEntity answer = answerMapper.selectOne(
                Wrappers.<AssessmentAttemptAnswerEntity>lambdaQuery()
                        .eq(AssessmentAttemptAnswerEntity::getAttemptId, session.attempt().getId())
                        .eq(AssessmentAttemptAnswerEntity::getQuestionOrder, request.questionOrder())
                        .last("LIMIT 1"));
        if (answer == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Assessment question was not found", 404);
        }
        if (timingEventMapper.selectCount(Wrappers.<AssessmentTimingEventEntity>lambdaQuery()
                .eq(AssessmentTimingEventEntity::getAttemptId, session.attempt().getId())
                .eq(AssessmentTimingEventEntity::getClientEventId, request.eventId())) > 0) {
            touchSession(session.session());
            return;
        }
        int acceptedDelta = (int) Math.min(30_000L, request.activeDurationMs());
        LocalDateTime now = LocalDateTime.now();
        AssessmentTimingEventEntity event = new AssessmentTimingEventEntity();
        event.setAttemptId(session.attempt().getId());
        event.setQuestionId(answer.getQuestionId());
        event.setClientEventId(request.eventId());
        event.setEventType("ACTIVE_DELTA");
        event.setEffectiveDeltaMs(acceptedDelta);
        event.setFirstPresentedAt(answer.getFirstPresentedAt() == null ? now : answer.getFirstPresentedAt());
        event.setFirstAnsweredAt(answer.getFirstAnsweredAt());
        event.setModificationCount(answer.getResponseChangeCount() == null ? 0 : answer.getResponseChangeCount());
        try {
            timingEventMapper.insert(event);
        } catch (DataIntegrityViolationException duplicateEvent) {
            touchSession(session.session());
            return;
        }
        answerMapper.recordEffectiveDuration(answer.getId(), session.attempt().getId(), acceptedDelta);
        touchSession(session.session());
    }

    @Transactional
    public AssessmentAttemptSubmitVO submit(
            String releaseCode,
            String sessionToken,
            SubmitAssessmentAttemptRequest request
    ) {
        SessionBundle session = requireSessionForUpdate(releaseCode, sessionToken);
        AssessmentAttemptEntity attempt = session.attempt();
        if (AssessmentAttemptStatus.SUBMITTED.name().equals(attempt.getStatus())) {
            return toSubmit(attempt);
        }
        requireInProgress(attempt);
        requireVersion(attempt, request.baseVersion());
        AssessmentSubmitReason reason = parseSubmitReason(request.reason());
        List<AssessmentAttemptAnswerEntity> answers = loadAnswers(attempt.getId());
        Map<Long, AssessmentQuestionEntity> questions = loadQuestionMap(attempt.getPaperId());
        applyResponses(request.responses(), answers, questions, true);
        LocalDateTime now = LocalDateTime.now();
        recomputeProgress(attempt, answers, now);
        attempt.setStatus(AssessmentAttemptStatus.SUBMITTED.name());
        attempt.setSubmittedAt(now);
        attempt.setSubmitReason(reason.name());
        if (attemptMapper.submitIfInProgress(attempt) == 0) {
            AssessmentAttemptEntity concurrent = attemptMapper.selectById(attempt.getId());
            return toSubmit(concurrent);
        }
        attempt.setVersion(attempt.getVersion() + 1);
        persistMetricsAndAiEvent(attempt, answers, questions);
        markParticipationCodeSubmitted(session.participant());
        touchSession(session.session());
        return toSubmit(attempt);
    }

    @Transactional
    public PublicAssessmentResultVO result(String releaseCode, String sessionToken) {
        SessionBundle session = requireSession(releaseCode, sessionToken);
        AssessmentAttemptEntity attempt = session.attempt();
        if (!AssessmentAttemptStatus.SUBMITTED.name().equals(attempt.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "Assessment attempt is still in progress", 409);
        }
        List<AssessmentAttemptAnswerEntity> answers = loadAnswers(attempt.getId());
        Map<Long, AssessmentQuestionEntity> questions = loadQuestionMap(attempt.getPaperId());
        AssessmentScoringV1.Result scoring = score(answers, questions);
        AssessmentMetricSnapshotVO metric = toMetric(scoring);
        AssessmentAiAnalysisEntity analysis = aiAnalysisMapper.selectOne(
                Wrappers.<AssessmentAiAnalysisEntity>lambdaQuery()
                        .eq(AssessmentAiAnalysisEntity::getAttemptId, attempt.getId())
                        .orderByDesc(AssessmentAiAnalysisEntity::getId)
                        .last("LIMIT 1"));
        AssessmentAiAnalysisVO analysisPayload = parseAnalysisPayload(analysis);
        List<AssessmentAttemptResultQuestionVO> resultQuestions = answers.stream()
                .map(this::toResultQuestion)
                .toList();
        touchSession(session.session());
        return new PublicAssessmentResultVO(attempt.getId(), session.release().release().getReleaseCode(),
                session.release().publish().getPaperTitleSnapshot(), attempt.getStatus(), answers.size(),
                attempt.getAnsweredCount(), scoring.correctCount(), attempt.getObjectiveScore(), attempt.getTotalScore(),
                attempt.getSubmittedAt(), true, metric, scoring.qualityFlags(),
                analysis == null ? null : analysis.getStatus(), analysisPayload, resultQuestions);
    }

    private AssessmentAiAnalysisVO parseAnalysisPayload(AssessmentAiAnalysisEntity analysis) {
        if (analysis == null) return null;
        String payload = "FALLBACK".equals(analysis.getStatus())
                ? analysis.getRuleFallbackJson()
                : analysis.getAnalysisJson();
        if (payload == null || payload.isBlank()) return null;
        try {
            return jsonCodec.read(payload, AssessmentAiAnalysisVO.class);
        } catch (RuntimeException exception) {
            log.error("event=assessment_ai_analysis_parse_failed analysisId={} attemptId={} status={}",
                    analysis.getId(), analysis.getAttemptId(), analysis.getStatus(), exception);
            return null;
        }
    }

    @Transactional
    public int submitExpiredAttemptsBatch(int batchSize) {
        if (batchSize <= 0) return 0;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.minus(timeoutProperties.getSubmissionGracePeriod());
        int submitted = 0;
        for (Long attemptId : attemptMapper.selectExpiredPublicAttemptIds(deadline, batchSize)) {
            AssessmentAttemptEntity attempt = attemptMapper.selectByIdForUpdate(attemptId);
            if (attempt == null || !AssessmentAttemptStatus.IN_PROGRESS.name().equals(attempt.getStatus())) continue;
            AssessmentParticipantEntity participant = participantMapper.selectById(attempt.getParticipantId());
            AssessmentPublishEntity publish = publishMapper.selectById(attempt.getPublishId());
            if (participant == null || publish == null) continue;
            List<AssessmentAttemptAnswerEntity> answers = loadAnswers(attempt.getId());
            Map<Long, AssessmentQuestionEntity> questions = loadQuestionMap(attempt.getPaperId());
            recomputeProgress(attempt, answers, now);
            attempt.setStatus(AssessmentAttemptStatus.SUBMITTED.name());
            attempt.setSubmittedAt(now);
            attempt.setSubmitReason(AssessmentSubmitReason.TIMEOUT.name());
            if (attemptMapper.submitIfInProgress(attempt) == 0) continue;
            attempt.setVersion(attempt.getVersion() + 1);
            persistMetricsAndAiEvent(attempt, answers, questions);
            markParticipationCodeSubmitted(participant);
            submitted++;
        }
        return submitted;
    }

    private SessionBundle requireSessionForUpdate(String releaseCode, String sessionToken) {
        SessionBundle session = requireSession(releaseCode, sessionToken);
        AssessmentAttemptEntity lockedAttempt = attemptMapper.selectByIdForUpdate(session.attempt().getId());
        if (lockedAttempt == null || !Objects.equals(lockedAttempt.getParticipantId(), session.participant().getId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "Participant session cannot access this attempt", 403);
        }
        return new SessionBundle(session.session(), session.participant(), session.release(), lockedAttempt);
    }

    private void requireInProgress(AssessmentAttemptEntity attempt) {
        if (!AssessmentAttemptStatus.IN_PROGRESS.name().equals(attempt.getStatus())) {
            throw new BusinessException(ResultCode.ATTEMPT_SUBMITTED, "Assessment attempt has already been submitted", 409);
        }
        if (attempt.getExpiresAt() != null && !LocalDateTime.now().isBefore(attempt.getExpiresAt())) {
            throw new BusinessException(ResultCode.ASSESSMENT_CLOSED, "Assessment attempt has expired", 409);
        }
    }

    private void requireVersion(AssessmentAttemptEntity attempt, Long baseVersion) {
        if (baseVersion == null || !Objects.equals(attempt.getVersion(), baseVersion)) {
            throw versionConflict();
        }
    }

    private BusinessException versionConflict() {
        return new BusinessException(ResultCode.CONFLICT, "Assessment attempt version conflict", 409);
    }

    private AssessmentSubmitReason parseSubmitReason(String value) {
        AssessmentSubmitReason reason;
        try {
            reason = value == null || value.isBlank() ? AssessmentSubmitReason.MANUAL : AssessmentSubmitReason.fromCode(value);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, exception.getMessage(), 400);
        }
        if (reason == AssessmentSubmitReason.SCHEDULER) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "SCHEDULER is reserved for server-side submission", 400);
        }
        return reason;
    }

    private List<AssessmentAttemptAnswerEntity> loadAnswers(Long attemptId) {
        return answerMapper.selectList(Wrappers.<AssessmentAttemptAnswerEntity>lambdaQuery()
                .eq(AssessmentAttemptAnswerEntity::getAttemptId, attemptId)
                .orderByAsc(AssessmentAttemptAnswerEntity::getQuestionOrder)
                .orderByAsc(AssessmentAttemptAnswerEntity::getId));
    }

    private Map<Long, AssessmentQuestionEntity> loadQuestionMap(Long paperId) {
        return loadQuestions(paperId).stream().collect(Collectors.toMap(
                AssessmentQuestionEntity::getId,
                Function.identity(),
                (left, right) -> left,
                LinkedHashMap::new));
    }

    private void applyResponses(
            List<AssessmentAttemptResponseRequest> requests,
            List<AssessmentAttemptAnswerEntity> answers,
            Map<Long, AssessmentQuestionEntity> questions,
            boolean finalSubmission
    ) {
        Map<Integer, AssessmentAttemptResponseRequest> byOrder = new LinkedHashMap<>();
        for (AssessmentAttemptResponseRequest request : requests == null ? List.<AssessmentAttemptResponseRequest>of() : requests) {
            if (byOrder.put(request.questionOrder(), request) != null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "Duplicate question order in responses", 400);
            }
        }
        Map<Integer, AssessmentAttemptAnswerEntity> answerByOrder = answers.stream().collect(Collectors.toMap(
                AssessmentAttemptAnswerEntity::getQuestionOrder, Function.identity()));
        for (Map.Entry<Integer, AssessmentAttemptResponseRequest> entry : byOrder.entrySet()) {
            AssessmentAttemptAnswerEntity answer = answerByOrder.get(entry.getKey());
            if (answer == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "Assessment question was not found", 404);
            }
            applyResponse(answer, entry.getValue(), finalSubmission);
            answerMapper.updateResponseSnapshot(answer);
        }
        if (finalSubmission) {
            for (AssessmentAttemptAnswerEntity answer : answers) {
                AssessmentQuestionEntity question = questions.get(answer.getQuestionId());
                AssessmentQuestionType type = parseQuestionType(answer.getQuestionType());
                boolean required = type != AssessmentQuestionType.INSTRUCTION
                        && (question == null || !Boolean.FALSE.equals(question.getRequiredAnswer()));
                if (required && !Boolean.TRUE.equals(answer.getAnswered())) {
                    throw new BusinessException(ResultCode.VALIDATION_ERROR,
                            "Required question " + answer.getQuestionOrder() + " has not been answered", 400);
                }
                if (type == AssessmentQuestionType.TRUE_FALSE_WITH_JUSTIFICATION
                        && jsonCodec.readStringList(answer.getResponseJson()).stream().anyMatch("F"::equalsIgnoreCase)
                        && normalizeText(answer.getJustificationText()) == null) {
                    throw new BusinessException(ResultCode.VALIDATION_ERROR,
                            "A justification is required when a true/false answer is F", 400);
                }
            }
        }
    }

    private void applyResponse(
            AssessmentAttemptAnswerEntity answer,
            AssessmentAttemptResponseRequest request,
            boolean finalSubmission
    ) {
        AssessmentQuestionType type = parseQuestionType(answer.getQuestionType());
        List<String> previous = jsonCodec.readStringList(answer.getResponseJson());
        String previousJustification = normalizeText(answer.getJustificationText());
        List<String> normalized = normalizeResponses(type, request.responses(), answer.getOptionsJsonSnapshot());
        String justification = normalizeText(request.justificationText());
        if (finalSubmission && type == AssessmentQuestionType.TRUE_FALSE_WITH_JUSTIFICATION
                && normalized.stream().anyMatch("F"::equalsIgnoreCase) && justification == null) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR,
                    "A justification is required when a true/false answer is F", 400);
        }
        boolean answered = !normalized.isEmpty();
        Boolean correct = answered ? isCorrect(type, normalized, jsonCodec.readStringList(answer.getCorrectAnswerJson())) : null;
        if (jsonCodec.readStringList(answer.getCorrectAnswerJson()).isEmpty()) {
            correct = null;
        }
        LocalDateTime now = LocalDateTime.now();
        if (answered && answer.getFirstAnsweredAt() == null) {
            answer.setFirstAnsweredAt(now);
        }
        if ((!previous.isEmpty() || previousJustification != null)
                && (!previous.equals(normalized) || !Objects.equals(previousJustification, justification))) {
            answer.setResponseChangeCount((answer.getResponseChangeCount() == null ? 0 : answer.getResponseChangeCount()) + 1);
        }
        answer.setResponseJson(answered ? jsonCodec.write(normalized) : null);
        answer.setJustificationText(justification);
        answer.setAnswered(answered);
        answer.setCorrect(correct);
        answer.setScoreAwarded(Boolean.TRUE.equals(correct) ? answer.getQuestionScore() : answered && correct != null ? 0 : null);
    }

    private List<String> normalizeResponses(AssessmentQuestionType type, List<String> raw, String optionsJson) {
        if (type == AssessmentQuestionType.INSTRUCTION || raw == null || raw.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> values = raw.stream()
                .map(value -> type == AssessmentQuestionType.FILL_BLANK ? normalizeFillBlankValue(value) : normalizeText(value))
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (type == AssessmentQuestionType.FILL_BLANK || type == AssessmentQuestionType.SHORT_TEXT || type == AssessmentQuestionType.NUMBER) {
            if (values.size() > 1) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "Text question accepts only one response", 400);
            }
            if (type == AssessmentQuestionType.NUMBER && !values.isEmpty()) {
                try {
                    new BigDecimal(values.iterator().next());
                } catch (NumberFormatException exception) {
                    throw new BusinessException(ResultCode.VALIDATION_ERROR, "Number question requires a numeric response", 400);
                }
            }
            return List.copyOf(values);
        }
        Map<String, String> optionKeys = jsonCodec.readOptions(optionsJson).stream().collect(Collectors.toMap(
                option -> option.key().toUpperCase(), option -> option.key(), (left, right) -> left, LinkedHashMap::new));
        LinkedHashSet<String> canonical = new LinkedHashSet<>();
        for (String value : values) {
            String key = optionKeys.get(value.toUpperCase());
            if (key == null) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR, "Unknown assessment option: " + value, 400);
            }
            canonical.add(key);
        }
        if (type != AssessmentQuestionType.MULTIPLE_CHOICE && canonical.size() > 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Single choice question accepts only one response", 400);
        }
        return List.copyOf(canonical);
    }

    private boolean isCorrect(AssessmentQuestionType type, List<String> actual, List<String> expected) {
        if (type == AssessmentQuestionType.FILL_BLANK) {
            Set<String> actualSet = actual.stream().map(this::normalizeFillBlankValue).collect(Collectors.toCollection(LinkedHashSet::new));
            Set<String> expectedSet = expected.stream().map(this::normalizeFillBlankValue).collect(Collectors.toCollection(LinkedHashSet::new));
            return actualSet.size() == 1 && expectedSet.contains(actualSet.iterator().next());
        }
        Set<String> actualSet = actual.stream().map(value -> value.toUpperCase()).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> expectedSet = expected.stream().map(value -> value.trim().toUpperCase()).collect(Collectors.toCollection(LinkedHashSet::new));
        return !actualSet.isEmpty() && actualSet.equals(expectedSet);
    }

    private AssessmentQuestionType parseQuestionType(String value) {
        try {
            return AssessmentQuestionType.fromCode(value);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, exception.getMessage(), 400);
        }
    }

    private String normalizeText(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeFillBlankValue(String value) {
        String normalized = normalizeText(value);
        if (normalized == null) return null;
        normalized = normalized.replace('\u2018', '\'').replace('\u2019', '\'')
                .replace('\u2010', '-').replace('\u2011', '-').replace('\u2013', '-').replace('\u2014', '-');
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        return normalized.replaceAll("\\s+", " ").trim();
    }

    private void recomputeProgress(AssessmentAttemptEntity attempt, List<AssessmentAttemptAnswerEntity> answers, LocalDateTime now) {
        int answered = 0;
        int score = 0;
        for (AssessmentAttemptAnswerEntity answer : answers) {
            if (Boolean.TRUE.equals(answer.getAnswered())) answered++;
            if (answer.getScoreAwarded() != null) score += answer.getScoreAwarded();
        }
        attempt.setAnsweredCount(answered);
        attempt.setObjectiveScore(score);
        attempt.setTotalScore(score);
        attempt.setLastSavedAt(now);
    }

    private AssessmentScoringV1.Result score(
            List<AssessmentAttemptAnswerEntity> answers,
            Map<Long, AssessmentQuestionEntity> questions
    ) {
        List<AssessmentScoringV1.Question> scoredQuestions = new ArrayList<>();
        Map<Integer, AssessmentScoringV1.Response> responses = new LinkedHashMap<>();
        for (AssessmentAttemptAnswerEntity answer : answers) {
            if (answer.getQuestionScore() == null || answer.getQuestionScore() <= 0) continue;
            AssessmentQuestionEntity question = questions.get(answer.getQuestionId());
            scoredQuestions.add(new AssessmentScoringV1.Question(answer.getQuestionOrder(), answer.getQuestionType(),
                    answer.getQuestionScore(), question == null || question.getWeight() == null ? 1d : question.getWeight().doubleValue(),
                    question == null ? null : question.getConstructCode(), question == null ? null : question.getContextLevel(),
                    question == null ? null : question.getTransferCategory(), jsonCodec.readStringList(answer.getCorrectAnswerJson())));
            responses.put(answer.getQuestionOrder(), new AssessmentScoringV1.Response(
                    jsonCodec.readStringList(answer.getResponseJson()), answer.getEffectiveDurationMs(), answer.getJustificationText()));
        }
        return AssessmentScoringV1.score(scoredQuestions, responses);
    }

    private AssessmentMetricSnapshotVO toMetric(AssessmentScoringV1.Result result) {
        List<AssessmentDimensionMetricVO> dimensions = result.dimensions().entrySet().stream()
                .map(entry -> new AssessmentDimensionMetricVO(entry.getKey(), entry.getValue().numerator(),
                        entry.getValue().denominator(), entry.getValue().ratio()))
                .toList();
        AssessmentReactionTimeMetricVO reaction = result.reactionTime() == null ? null
                : new AssessmentReactionTimeMetricVO(result.reactionTime().medianMs(), result.reactionTime().firstQuartileMs(),
                result.reactionTime().thirdQuartileMs(), result.reactionTime().sampleCount());
        return new AssessmentMetricSnapshotVO(AssessmentScoringV1.VERSION, result.percentage(), dimensions,
                result.cognateAdvantagePoints(), result.falseFriendInterferencePoints(), result.contextRepairPoints(), reaction);
    }

    private void persistMetricsAndAiEvent(
            AssessmentAttemptEntity attempt,
            List<AssessmentAttemptAnswerEntity> answers,
            Map<Long, AssessmentQuestionEntity> questions
    ) {
        AssessmentMetricSnapshotEntity existing = metricSnapshotMapper.selectOne(
                Wrappers.<AssessmentMetricSnapshotEntity>lambdaQuery()
                        .eq(AssessmentMetricSnapshotEntity::getAttemptId, attempt.getId())
                        .eq(AssessmentMetricSnapshotEntity::getMetricVersion, AssessmentScoringV1.VERSION)
                        .last("LIMIT 1"));
        if (existing != null) return;
        AssessmentScoringV1.Result scoring = score(answers, questions);
        double weightedMax = questions.values().stream()
                .filter(question -> question.getScore() != null && question.getScore() > 0)
                .mapToDouble(question -> question.getScore() * (question.getWeight() == null ? 1d : question.getWeight().doubleValue()))
                .sum();
        double weightedRaw = scoring.percentage() == null ? 0d : weightedMax * scoring.percentage() / 100d;
        AssessmentMetricSnapshotVO metric = toMetric(scoring);
        AssessmentMetricSnapshotEntity snapshot = new AssessmentMetricSnapshotEntity();
        snapshot.setAttemptId(attempt.getId());
        snapshot.setMetricVersion(AssessmentScoringV1.VERSION);
        snapshot.setScoringVersion(AssessmentScoringV1.VERSION);
        snapshot.setRawScore(BigDecimal.valueOf(weightedRaw).setScale(4, RoundingMode.HALF_UP));
        snapshot.setMaxScore(BigDecimal.valueOf(weightedMax).setScale(4, RoundingMode.HALF_UP));
        snapshot.setPercentageScore(scoring.percentage() == null ? null
                : BigDecimal.valueOf(scoring.percentage()).setScale(4, RoundingMode.HALF_UP));
        snapshot.setMetricsJson(jsonCodec.write(metric));
        snapshot.setQualityFlagsJson(jsonCodec.write(scoring.qualityFlags()));
        metricSnapshotMapper.insert(snapshot);

        AssessmentAiAnalysisEntity analysis = new AssessmentAiAnalysisEntity();
        analysis.setAttemptId(attempt.getId());
        analysis.setMetricSnapshotId(snapshot.getId());
        analysis.setPromptVersion(AssessmentAiAnalysisProcessor.PROMPT_VERSION);
        analysis.setIdempotencyKey(attempt.getId() + ":" + AssessmentScoringV1.VERSION + ":" + AssessmentAiAnalysisProcessor.PROMPT_VERSION);
        analysis.setStatus("PENDING");
        analysis.setRetryCount(0);
        aiAnalysisMapper.insert(analysis);
    }

    private AssessmentAttemptResultQuestionVO toResultQuestion(AssessmentAttemptAnswerEntity answer) {
        List<AssessmentOptionVO> options = jsonCodec.readOptions(answer.getOptionsJsonSnapshot()).stream()
                .map(option -> new AssessmentOptionVO(option.key(), option.label())).toList();
        return new AssessmentAttemptResultQuestionVO(answer.getId(), answer.getQuestionId(), answer.getQuestionOrder(),
                answer.getQuestionType(), answer.getStemTextSnapshot(), answer.getPromptTextSnapshot(), options,
                answer.getQuestionScore(), jsonCodec.readStringList(answer.getResponseJson()),
                jsonCodec.readStringList(answer.getCorrectAnswerJson()), answer.getCorrect(), answer.getScoreAwarded(),
                answer.getExplanationTextSnapshot(), answer.getJustificationText());
    }

    private AssessmentAttemptSubmitVO toSubmit(AssessmentAttemptEntity attempt) {
        return new AssessmentAttemptSubmitVO(attempt.getId(), AssessmentAttemptStatus.fromCode(attempt.getStatus()),
                attempt.getSubmittedAt(), attempt.getVersion(), attempt.getSubmitReason());
    }

    private void markParticipationCodeSubmitted(AssessmentParticipantEntity participant) {
        if (participant.getParticipationCodeId() == null) return;
        AssessmentParticipationCodeEntity code = participationCodeMapper.selectById(participant.getParticipationCodeId());
        if (code != null && !"SUBMITTED".equals(code.getStatus())) {
            code.setStatus("SUBMITTED");
            code.setSubmittedAt(LocalDateTime.now());
            participationCodeMapper.updateById(code);
        }
    }

    private void touchSession(AssessmentParticipantSessionEntity session) {
        session.setLastSeenAt(LocalDateTime.now());
        participantSessionMapper.updateById(session);
    }

    private AssessmentAttemptEntity createAttempt(AssessmentPublishEntity publish, AssessmentParticipantEntity participant) {
        LocalDateTime now = LocalDateTime.now();
        AssessmentAttemptEntity attempt = new AssessmentAttemptEntity();
        attempt.setPublishId(publish.getId());
        attempt.setPaperId(publish.getPaperId());
        attempt.setParticipantId(participant.getId());
        attempt.setStatus(AssessmentAttemptStatus.IN_PROGRESS.name());
        attempt.setStartedAt(now);
        LocalDateTime durationExpiry = now.plusMinutes(publish.getDurationMinutes());
        attempt.setExpiresAt(publish.getDueAt() != null && publish.getDueAt().isBefore(durationExpiry) ? publish.getDueAt() : durationExpiry);
        attempt.setAnsweredCount(0);
        attempt.setObjectiveScore(0);
        attempt.setTotalScore(0);
        attempt.setVersion(1L);
        attemptMapper.insert(attempt);
        int order = 1;
        for (AssessmentQuestionEntity question : loadQuestions(publish.getPaperId())) {
            AssessmentAttemptAnswerEntity answer = new AssessmentAttemptAnswerEntity();
            answer.setAttemptId(attempt.getId());
            answer.setQuestionId(question.getId());
            answer.setQuestionOrder(order++);
            answer.setQuestionType(question.getQuestionType());
            answer.setStemTextSnapshot(question.getStemText());
            answer.setPromptTextSnapshot(question.getPromptText());
            answer.setOptionsJsonSnapshot(question.getOptionsJson());
            answer.setCorrectAnswerJson(question.getCorrectAnswerJson());
            answer.setExplanationTextSnapshot(question.getExplanationText());
            answer.setQuestionScore(question.getScore());
            answer.setAnswered(false);
            answerMapper.insert(answer);
        }
        return attempt;
    }

    private PublicAssessmentAttemptVO toAttempt(ReleaseBundle bundle, AssessmentAttemptEntity attempt) {
        Map<Long, AssessmentQuestionEntity> questions = new LinkedHashMap<>();
        for (AssessmentQuestionEntity question : loadQuestions(attempt.getPaperId())) {
            questions.put(question.getId(), question);
        }
        Map<String, SectionPresentation> sections = loadSectionPresentations(attempt.getPaperId());
        Map<Long, String> itemCodes = loadItemCodes(attempt.getPaperId());
        List<PublicAssessmentQuestionVO> items = answerMapper.selectList(
                        Wrappers.<AssessmentAttemptAnswerEntity>lambdaQuery()
                                .eq(AssessmentAttemptAnswerEntity::getAttemptId, attempt.getId())
                                .orderByAsc(AssessmentAttemptAnswerEntity::getQuestionOrder))
                .stream().map(answer -> {
                    AssessmentQuestionEntity question = questions.get(answer.getQuestionId());
                    List<AssessmentOptionVO> options = jsonCodec.readOptions(answer.getOptionsJsonSnapshot()).stream()
                            .map(option -> new AssessmentOptionVO(option.key(), option.label())).toList();
                    SectionPresentation section = question == null ? null : sections.get(question.getSectionCode());
                    return new PublicAssessmentQuestionVO(answer.getQuestionId(), answer.getQuestionOrder(), answer.getQuestionType(),
                            question == null ? null : question.getSectionCode(),
                            section == null ? null : section.title(),
                            section == null ? null : section.sharedMaterial(), answer.getStemTextSnapshot(),
                            answer.getPromptTextSnapshot(), options, question == null || !Boolean.FALSE.equals(question.getRequiredAnswer()),
                            "TRUE_FALSE_WITH_JUSTIFICATION".equals(answer.getQuestionType()),
                            jsonCodec.readStringList(answer.getResponseJson()), answer.getJustificationText(),
                            question == null ? null : itemCodes.get(question.getId()),
                            question == null ? null : question.getDisplayConditionJson());
                }).toList();
        AssessmentPublishEntity publish = bundle.publish();
        return new PublicAssessmentAttemptVO(attempt.getId(), bundle.release().getReleaseCode(), publish.getPaperTitleSnapshot(),
                publish.getPaperDescriptionSnapshot(), publish.getInstructionsText(), attempt.getStatus(), publish.getDurationMinutes(),
                items.size(), attempt.getAnsweredCount(), attempt.getStartedAt(), attempt.getExpiresAt(), attempt.getLastSavedAt(),
                attempt.getVersion(), LocalDateTime.now(), items);
    }

    private SessionBundle requireSession(String releaseCode, String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Participant session is required", 401);
        }
        AssessmentParticipantSessionEntity session = participantSessionMapper.selectOne(
                Wrappers.<AssessmentParticipantSessionEntity>lambdaQuery()
                        .eq(AssessmentParticipantSessionEntity::getSessionTokenDigest, codeCodec.digestOpaque(token))
                        .isNull(AssessmentParticipantSessionEntity::getRevokedAt)
                        .gt(AssessmentParticipantSessionEntity::getExpiresAt, LocalDateTime.now())
                        .last("LIMIT 1"));
        if (session == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Participant session has expired", 401);
        }
        AssessmentParticipantEntity participant = participantMapper.selectById(session.getParticipantId());
        if (participant == null || participant.getAttemptId() == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Participant session is invalid", 401);
        }
        ReleaseBundle bundle = requireRelease(releaseCode);
        if (!Objects.equals(participant.getPublishId(), bundle.publish().getId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "Participant session belongs to another release", 403);
        }
        AssessmentAttemptEntity attempt = attemptMapper.selectById(participant.getAttemptId());
        if (attempt == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Assessment attempt was not found", 404);
        }
        return new SessionBundle(session, participant, bundle, attempt);
    }

    private ReleaseBundle requireRelease(String rawReleaseCode) {
        String releaseCode = rawReleaseCode == null ? "" : rawReleaseCode.trim().toUpperCase();
        AssessmentPublicReleaseEntity release = publicReleaseMapper.selectOne(
                Wrappers.<AssessmentPublicReleaseEntity>lambdaQuery()
                        .eq(AssessmentPublicReleaseEntity::getReleaseCode, releaseCode).last("LIMIT 1"));
        if (release == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Public assessment release was not found", 404);
        }
        AssessmentPublishEntity publish = publishMapper.selectById(release.getPublishId());
        if (publish == null || !AssessmentPublishStatus.PUBLISHED.name().equals(publish.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Public assessment release was not found", 404);
        }
        return new ReleaseBundle(release, publish);
    }

    private List<AssessmentQuestionEntity> loadQuestions(Long paperId) {
        return questionMapper.selectList(Wrappers.<AssessmentQuestionEntity>lambdaQuery()
                .eq(AssessmentQuestionEntity::getPaperId, paperId)
                .orderByAsc(AssessmentQuestionEntity::getSortOrder).orderByAsc(AssessmentQuestionEntity::getId));
    }

    private Map<String, SectionPresentation> loadSectionPresentations(Long paperId) {
        Long versionId = jdbcTemplate.query("""
                        SELECT id FROM assessment_questionnaire_version
                        WHERE paper_id = ? AND deleted = FALSE ORDER BY version_no DESC LIMIT 1
                        """, (resultSet, rowNumber) -> resultSet.getLong(1), paperId)
                .stream().findFirst().orElse(null);
        if (versionId == null) {
            return Map.of();
        }
        return jdbcTemplate.query("""
                        SELECT section_code, title, shared_material FROM assessment_questionnaire_section
                        WHERE questionnaire_version_id = ? AND deleted = FALSE
                        """, (resultSet, rowNumber) -> new SectionPresentation(
                        resultSet.getString(1), resultSet.getString(2), resultSet.getString(3)), versionId)
                .stream().collect(Collectors.toMap(SectionPresentation::sectionCode, Function.identity()));
    }

    private Map<Long, String> loadItemCodes(Long paperId) {
        Long versionId = jdbcTemplate.query("""
                        SELECT id FROM assessment_questionnaire_version
                        WHERE paper_id = ? AND deleted = FALSE ORDER BY version_no DESC LIMIT 1
                        """, (resultSet, rowNumber) -> resultSet.getLong(1), paperId)
                .stream().findFirst().orElse(null);
        if (versionId == null) {
            return Map.of();
        }
        return jdbcTemplate.query("""
                        SELECT assessment_question_id, item_code FROM assessment_questionnaire_item
                        WHERE questionnaire_version_id = ? AND deleted = FALSE
                        """, (resultSet, rowNumber) -> new Object[]{resultSet.getLong(1), resultSet.getString(2)}, versionId)
                .stream().collect(Collectors.toMap(row -> (Long) row[0], row -> (String) row[1]));
    }

    private record SectionPresentation(String sectionCode, String title, String sharedMaterial) {
    }

    private void requireOpen(AssessmentPublishEntity publish, LocalDateTime now) {
        if (publish.getStartsAt() != null && now.isBefore(publish.getStartsAt())) {
            throw new BusinessException(ResultCode.ASSESSMENT_NOT_STARTED, "Assessment has not started yet", 409);
        }
        if (publish.getDueAt() != null && !now.isBefore(publish.getDueAt())) {
            throw new BusinessException(ResultCode.ASSESSMENT_CLOSED, "Assessment is already closed", 409);
        }
    }

    private void enforceRateLimit(String remoteAddress) {
        String key = remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress;
        LocalDateTime threshold = LocalDateTime.now().minus(VERIFY_WINDOW);
        Deque<LocalDateTime> attempts = verificationAttempts.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (attempts) {
            while (!attempts.isEmpty() && attempts.peekFirst().isBefore(threshold)) attempts.removeFirst();
            if (attempts.size() >= VERIFY_LIMIT) {
                throw new BusinessException(ResultCode.RATE_LIMITED, "Too many participant-code attempts", 429);
            }
            attempts.addLast(LocalDateTime.now());
        }
    }

    private AssessmentParticipantEntity selectQrParticipantForUpdate(Long publishId, String fingerprintDigest) {
        return participantMapper.selectOne(Wrappers.<AssessmentParticipantEntity>lambdaQuery()
                .eq(AssessmentParticipantEntity::getPublishId, publishId)
                .eq(AssessmentParticipantEntity::getParticipantType, "PUBLIC_QR")
                .eq(AssessmentParticipantEntity::getBrowserFingerprintDigest, fingerprintDigest)
                .last("LIMIT 1 FOR UPDATE"));
    }

    private String normalizeFingerprint(String rawFingerprint) {
        String fingerprint = rawFingerprint == null ? "" : rawFingerprint.trim();
        if (fingerprint.length() < 16 || fingerprint.length() > 128
                || !fingerprint.matches("^[A-Za-z0-9_-]+$")) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "Browser fingerprint is invalid", 400);
        }
        return fingerprint;
    }

    private void recordAccess(
            AssessmentPublicReleaseEntity release,
            AssessmentParticipantEntity participant,
            AssessmentParticipationCodeEntity participationCode,
            String accessMode,
            String remoteAddress,
            LocalDateTime now
    ) {
        String normalizedIp = AssessmentClientIpNormalizer.normalize(remoteAddress);
        String auditableIp = normalizedIp == null ? "unknown" : normalizedIp;
        AssessmentParticipantAccessCipher.EncryptedValue encrypted = accessCipher.encrypt(auditableIp);
        AssessmentParticipantAccessEntity access = new AssessmentParticipantAccessEntity();
        access.setPublicReleaseId(release.getId());
        access.setParticipantId(participant.getId());
        access.setParticipationCodeId(participationCode == null ? null : participationCode.getId());
        access.setAccessMode(accessMode);
        access.setIpCiphertext(encrypted.ciphertext());
        access.setIpIv(encrypted.iv());
        access.setIpKeyVersion(encrypted.keyVersion());
        access.setAccessedAt(now);
        participantAccessMapper.insert(access);
    }

    private VerifiedSession issueSession(
            ReleaseBundle bundle,
            AssessmentParticipantEntity participant,
            AssessmentAttemptEntity attempt,
            boolean resumed,
            LocalDateTime now
    ) {
        String token = newSessionToken();
        LocalDateTime expiresAt = now.plus(configuredSessionTtl);
        AssessmentParticipantSessionEntity session = new AssessmentParticipantSessionEntity();
        session.setParticipantId(participant.getId());
        session.setSessionTokenDigest(codeCodec.digestOpaque(token));
        session.setExpiresAt(expiresAt);
        session.setLastSeenAt(now);
        participantSessionMapper.insert(session);
        return new VerifiedSession(token, expiresAt, new PublicAssessmentSessionVO(true, resumed,
                bundle.release().getReleaseCode(), toAttempt(bundle, attempt)));
    }

    private BusinessException invalidCode() {
        return new BusinessException(ResultCode.PARTICIPATION_CODE_INVALID,
                "Participation code is invalid or unavailable", 422);
    }

    private String newSessionToken() {
        byte[] token = new byte[32];
        secureRandom.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    public record VerifiedSession(String token, LocalDateTime expiresAt, PublicAssessmentSessionVO response) { }
    private record ReleaseBundle(AssessmentPublicReleaseEntity release, AssessmentPublishEntity publish) { }
    private record SessionBundle(AssessmentParticipantSessionEntity session, AssessmentParticipantEntity participant,
                                 ReleaseBundle release, AssessmentAttemptEntity attempt) { }
}
