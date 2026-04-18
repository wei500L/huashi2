package com.huashi.eftransfer.app.modules.assessment.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.common.util.SecurityUtils;
import com.huashi.eftransfer.app.modules.analytics.entity.TeachingClassEntity;
import com.huashi.eftransfer.app.modules.analytics.mapper.TeachingClassMapper;
import com.huashi.eftransfer.app.modules.analytics.service.TeachingClassService;
import com.huashi.eftransfer.app.modules.assessment.dto.AssessmentHistoryPageQuery;
import com.huashi.eftransfer.app.modules.assessment.dto.AssessmentAttemptResponseRequest;
import com.huashi.eftransfer.app.modules.assessment.dto.AssessmentOptionRequest;
import com.huashi.eftransfer.app.modules.assessment.dto.AssessmentPaperSaveRequest;
import com.huashi.eftransfer.app.modules.assessment.dto.AssessmentPublishRequest;
import com.huashi.eftransfer.app.modules.assessment.dto.AssessmentQuestionRequest;
import com.huashi.eftransfer.app.modules.assessment.dto.SaveAssessmentResponsesRequest;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentAttemptAnswerEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentAttemptEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentPaperEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentPublishEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentPublishRecipientEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentQuestionEntity;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentAttemptAnswerMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentAttemptMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentPaperMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentPublishMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentPublishRecipientMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentQuestionMapper;
import com.huashi.eftransfer.app.modules.assessment.support.AssessmentJsonCodec;
import com.huashi.eftransfer.app.modules.assessment.support.AssessmentOptionPayload;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentAttemptDetailVO;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentAttemptProgressVO;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentAttemptQuestionVO;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentAttemptResultQuestionVO;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentAttemptResultVO;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentAttemptStartVO;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentAttemptSubmitVO;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentOptionVO;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentPaperDetailVO;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentPaperQuestionVO;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentPaperSummaryVO;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentPublishDetailVO;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentPublishRosterItemVO;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentPublishSummaryVO;
import com.huashi.eftransfer.app.modules.assessment.vo.StudentAssessmentHistorySummaryVO;
import com.huashi.eftransfer.app.modules.assessment.vo.StudentAssessmentSummaryVO;
import com.huashi.eftransfer.app.modules.assessment.vo.TeacherAssessmentAttemptResultVO;
import com.huashi.eftransfer.app.modules.notification.service.NotificationScenarioService;
import com.huashi.eftransfer.app.modules.user.entity.UserEntity;
import com.huashi.eftransfer.app.modules.user.mapper.UserMapper;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.enums.AssessmentAttemptStatus;
import com.huashi.eftransfer.shared.enums.AssessmentPaperStatus;
import com.huashi.eftransfer.shared.enums.AssessmentPublishStatus;
import com.huashi.eftransfer.shared.enums.AssessmentQuestionType;
import com.huashi.eftransfer.shared.exception.BusinessException;
import com.huashi.eftransfer.shared.page.PageQuery;
import com.huashi.eftransfer.shared.page.PageResult;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AssessmentService {

    private static final DateTimeFormatter PAPER_CODE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final AssessmentPaperMapper assessmentPaperMapper;
    private final AssessmentQuestionMapper assessmentQuestionMapper;
    private final AssessmentPublishMapper assessmentPublishMapper;
    private final AssessmentPublishRecipientMapper assessmentPublishRecipientMapper;
    private final AssessmentAttemptMapper assessmentAttemptMapper;
    private final AssessmentAttemptAnswerMapper assessmentAttemptAnswerMapper;
    private final TeachingClassMapper teachingClassMapper;
    private final TeachingClassService teachingClassService;
    private final AssessmentJsonCodec assessmentJsonCodec;
    private final UserMapper userMapper;
    private final NotificationScenarioService notificationScenarioService;

    public AssessmentService(
            AssessmentPaperMapper assessmentPaperMapper,
            AssessmentQuestionMapper assessmentQuestionMapper,
            AssessmentPublishMapper assessmentPublishMapper,
            AssessmentPublishRecipientMapper assessmentPublishRecipientMapper,
            AssessmentAttemptMapper assessmentAttemptMapper,
            AssessmentAttemptAnswerMapper assessmentAttemptAnswerMapper,
            TeachingClassMapper teachingClassMapper,
            TeachingClassService teachingClassService,
            AssessmentJsonCodec assessmentJsonCodec,
            UserMapper userMapper,
            NotificationScenarioService notificationScenarioService
    ) {
        this.assessmentPaperMapper = assessmentPaperMapper;
        this.assessmentQuestionMapper = assessmentQuestionMapper;
        this.assessmentPublishMapper = assessmentPublishMapper;
        this.assessmentPublishRecipientMapper = assessmentPublishRecipientMapper;
        this.assessmentAttemptMapper = assessmentAttemptMapper;
        this.assessmentAttemptAnswerMapper = assessmentAttemptAnswerMapper;
        this.teachingClassMapper = teachingClassMapper;
        this.teachingClassService = teachingClassService;
        this.assessmentJsonCodec = assessmentJsonCodec;
        this.userMapper = userMapper;
        this.notificationScenarioService = notificationScenarioService;
    }

    public List<AssessmentPaperSummaryVO> listTeacherPapers() {
        var query = Wrappers.<AssessmentPaperEntity>lambdaQuery()
                .orderByDesc(AssessmentPaperEntity::getUpdatedAt)
                .orderByDesc(AssessmentPaperEntity::getId);
        if (!isAdmin()) {
            query.eq(AssessmentPaperEntity::getOwnerUserId, currentUserId());
        }
        return assessmentPaperMapper.selectList(query).stream()
                .map(this::toPaperSummary)
                .toList();
    }

    @Transactional
    public AssessmentPaperDetailVO createPaper(AssessmentPaperSaveRequest request) {
        List<NormalizedQuestion> normalizedQuestions = normalizeQuestions(request.questions());
        AssessmentPaperEntity paper = new AssessmentPaperEntity();
        paper.setPaperCode(generatePaperCode());
        paper.setTitle(normalizeRequiredText(request.title(), "title"));
        paper.setDescription(normalizeOptionalText(request.description()));
        paper.setOwnerUserId(currentUserId());
        paper.setStatus(AssessmentPaperStatus.DRAFT.name());
        paper.setDurationMinutes(request.durationMinutes());
        paper.setQuestionCount(normalizedQuestions.size());
        paper.setTotalScore(normalizedQuestions.stream().mapToInt(NormalizedQuestion::score).sum());
        assessmentPaperMapper.insert(paper);
        replacePaperQuestions(paper.getId(), normalizedQuestions);
        return buildPaperDetail(requireAccessiblePaper(paper.getId()));
    }

    @Transactional
    public AssessmentPaperDetailVO updatePaper(Long paperId, AssessmentPaperSaveRequest request) {
        AssessmentPaperEntity paper = requireEditablePaper(paperId);
        List<NormalizedQuestion> normalizedQuestions = normalizeQuestions(request.questions());
        paper.setTitle(normalizeRequiredText(request.title(), "title"));
        paper.setDescription(normalizeOptionalText(request.description()));
        paper.setDurationMinutes(request.durationMinutes());
        paper.setQuestionCount(normalizedQuestions.size());
        paper.setTotalScore(normalizedQuestions.stream().mapToInt(NormalizedQuestion::score).sum());
        assessmentPaperMapper.updateById(paper);
        replacePaperQuestions(paperId, normalizedQuestions);
        return buildPaperDetail(requireAccessiblePaper(paperId));
    }

    public AssessmentPaperDetailVO getPaperDetail(Long paperId) {
        return buildPaperDetail(requireAccessiblePaper(paperId));
    }

    @Transactional
    public AssessmentPublishSummaryVO publishPaper(Long paperId, AssessmentPublishRequest request) {
        AssessmentPaperEntity paper = requireAccessiblePaper(paperId);
        validatePublishWindow(request.startsAt(), request.dueAt());
        if (loadQuestionsByPaper(paperId).isEmpty()) {
            throw new BusinessException(ResultCode.CONFLICT, "Assessment paper must contain questions before publishing", 409);
        }
        TeachingClassEntity teachingClass = teachingClassService.requireAccessibleClass(request.teachingClassId());

        AssessmentPublishEntity publish = new AssessmentPublishEntity();
        publish.setPaperId(paper.getId());
        publish.setTeachingClassId(teachingClass.getId());
        publish.setPublishedBy(currentUserId());
        publish.setStatus(AssessmentPublishStatus.PUBLISHED.name());
        publish.setPaperTitleSnapshot(paper.getTitle());
        publish.setPaperDescriptionSnapshot(paper.getDescription());
        publish.setQuestionCountSnapshot(paper.getQuestionCount());
        publish.setTotalScoreSnapshot(paper.getTotalScore());
        publish.setDurationMinutes(paper.getDurationMinutes());
        publish.setInstructionsText(normalizeOptionalText(request.instructionsText()));
        publish.setStartsAt(request.startsAt());
        publish.setDueAt(request.dueAt());
        publish.setPublishedAt(LocalDateTime.now());
        assessmentPublishMapper.insert(publish);
        snapshotRecipients(publish);

        paper.setStatus(AssessmentPaperStatus.PUBLISHED.name());
        paper.setLatestPublishAt(publish.getPublishedAt());
        assessmentPaperMapper.updateById(paper);

        List<AssessmentPublishRecipientEntity> recipients = loadRecipientsByPublish(publish.getId());
        notificationScenarioService.notifyAssessmentPublished(publish, teachingClass, recipients);

        int assignedCount = recipients.size();
        return buildPublishSummary(publish, teachingClass.getClassName(), assignedCount, 0, 0);
    }

    public List<StudentAssessmentSummaryVO> listStudentAssessments() {
        Long studentUserId = currentUserId();
        LocalDateTime now = LocalDateTime.now();
        List<AssessmentPublishRecipientEntity> recipients = assessmentPublishRecipientMapper.selectList(
                Wrappers.<AssessmentPublishRecipientEntity>lambdaQuery()
                        .eq(AssessmentPublishRecipientEntity::getStudentUserId, studentUserId)
                        .orderByDesc(AssessmentPublishRecipientEntity::getPublishId)
                        .orderByDesc(AssessmentPublishRecipientEntity::getId)
        );
        if (recipients.isEmpty()) {
            return List.of();
        }

        List<AssessmentPublishEntity> publishes = assessmentPublishMapper.selectList(Wrappers.<AssessmentPublishEntity>lambdaQuery()
                .in(AssessmentPublishEntity::getId, recipients.stream().map(AssessmentPublishRecipientEntity::getPublishId).toList())
                .eq(AssessmentPublishEntity::getStatus, AssessmentPublishStatus.PUBLISHED.name())
                .orderByDesc(AssessmentPublishEntity::getPublishedAt)
                .orderByDesc(AssessmentPublishEntity::getId));
        if (publishes.isEmpty()) {
            return List.of();
        }

        Map<Long, TeachingClassEntity> classMap = loadTeachingClassMap(
                publishes.stream().map(AssessmentPublishEntity::getTeachingClassId).toList()
        );
        Map<Long, AssessmentAttemptEntity> attemptByPublishId = assessmentAttemptMapper.selectList(
                        Wrappers.<AssessmentAttemptEntity>lambdaQuery()
                                .eq(AssessmentAttemptEntity::getStudentUserId, studentUserId)
                                .in(AssessmentAttemptEntity::getPublishId, publishes.stream().map(AssessmentPublishEntity::getId).toList())
                ).stream()
                .collect(Collectors.toMap(AssessmentAttemptEntity::getPublishId, Function.identity(), (left, right) -> right, LinkedHashMap::new));

        return publishes.stream()
                .map(publish -> {
                    AssessmentAttemptEntity attempt = attemptByPublishId.get(publish.getId());
                    String attemptStatus = attempt == null
                            ? null
                            : isAttemptExpired(attempt, publish, now)
                            ? AssessmentAttemptStatus.SUBMITTED.name()
                            : attempt.getStatus();
                    TeachingClassEntity teachingClass = classMap.get(publish.getTeachingClassId());
                    return new StudentAssessmentSummaryVO(
                            publish.getId(),
                            publish.getPaperId(),
                            publish.getPaperTitleSnapshot(),
                            publish.getPaperDescriptionSnapshot(),
                            publish.getTeachingClassId(),
                            teachingClass == null ? "未知班级" : teachingClass.getClassName(),
                            publish.getInstructionsText(),
                            publish.getDurationMinutes(),
                            publish.getQuestionCountSnapshot(),
                            publish.getTotalScoreSnapshot(),
                            publish.getStartsAt(),
                            publish.getDueAt(),
                            publish.getPublishedAt(),
                            attemptStatus,
                            attempt == null ? null : attempt.getId(),
                            attempt == null ? null : attempt.getAnsweredCount(),
                            attempt == null ? null : attempt.getStartedAt(),
                            attempt == null ? null : attempt.getExpiresAt(),
                            attempt == null ? null : attempt.getSubmittedAt()
                    );
                })
                .toList();
    }

    public PageResult<StudentAssessmentHistorySummaryVO> pageStudentHistory(AssessmentHistoryPageQuery query) {
        PageQuery pageQuery = query.toPageQuery();
        Long studentUserId = currentUserId();
        LocalDateTime now = LocalDateTime.now();

        List<AssessmentAttemptEntity> attempts = assessmentAttemptMapper.selectList(Wrappers.<AssessmentAttemptEntity>lambdaQuery()
                .eq(AssessmentAttemptEntity::getStudentUserId, studentUserId)
                .orderByDesc(AssessmentAttemptEntity::getStartedAt)
                .orderByDesc(AssessmentAttemptEntity::getId));
        if (attempts.isEmpty()) {
            return new PageResult<>(0, pageQuery.pageNo(), pageQuery.pageSize(), List.of());
        }

        Map<Long, AssessmentPublishEntity> publishMap = loadPublishMap(
                attempts.stream().map(AssessmentAttemptEntity::getPublishId).toList()
        );
        Map<Long, TeachingClassEntity> classMap = loadTeachingClassMap(
                publishMap.values().stream()
                        .map(AssessmentPublishEntity::getTeachingClassId)
                        .filter(Objects::nonNull)
                        .toList()
        );

        List<StudentAssessmentHistorySummaryVO> filtered = attempts.stream()
                .map(attempt -> {
                    AssessmentPublishEntity publish = publishMap.get(attempt.getPublishId());
                    if (publish == null) {
                        return null;
                    }
                    AssessmentAttemptEntity effectiveAttempt = finalizeExpiredAttemptIfNecessary(attempt, publish, now);
                    if (query.status() != null && !query.status().isBlank()
                            && !query.status().equalsIgnoreCase(effectiveAttempt.getStatus())) {
                        return null;
                    }
                    TeachingClassEntity teachingClass = classMap.get(publish.getTeachingClassId());
                    return new StudentAssessmentHistorySummaryVO(
                            effectiveAttempt.getId(),
                            publish.getId(),
                            publish.getPaperId(),
                            publish.getPaperTitleSnapshot(),
                            publish.getPaperDescriptionSnapshot(),
                            teachingClass == null ? "未知班级" : teachingClass.getClassName(),
                            AssessmentAttemptStatus.fromCode(effectiveAttempt.getStatus()),
                            publish.getQuestionCountSnapshot(),
                            effectiveAttempt.getAnsweredCount(),
                            effectiveAttempt.getObjectiveScore(),
                            effectiveAttempt.getTotalScore(),
                            effectiveAttempt.getStartedAt(),
                            effectiveAttempt.getLastSavedAt(),
                            effectiveAttempt.getExpiresAt(),
                            effectiveAttempt.getSubmittedAt()
                    );
                })
                .filter(Objects::nonNull)
                .toList();

        int total = filtered.size();
        long fromIndex = Math.min(pageQuery.offset(), total);
        long toIndex = Math.min(fromIndex + pageQuery.pageSize(), total);
        return new PageResult<>(
                total,
                pageQuery.pageNo(),
                pageQuery.pageSize(),
                filtered.subList((int) fromIndex, (int) toIndex)
        );
    }

    @Transactional
    public AssessmentAttemptStartVO startOrResumeAttempt(Long publishId) {
        Long studentUserId = currentUserId();
        AssessmentPublishEntity publish = requireAccessiblePublishForStudent(publishId, studentUserId);
        LocalDateTime now = LocalDateTime.now();
        AssessmentAttemptStartVO resumedAttempt = resumeExistingAttempt(publish, studentUserId, now);
        if (resumedAttempt != null) {
            return resumedAttempt;
        }

        requirePublishAvailableForStart(publish, now);
        List<AssessmentQuestionEntity> questions = loadQuestionsByPaper(publish.getPaperId());
        if (questions.isEmpty()) {
            throw new BusinessException(ResultCode.CONFLICT, "Assessment paper does not contain any question", 409);
        }

        AssessmentAttemptEntity attempt = new AssessmentAttemptEntity();
        attempt.setPublishId(publish.getId());
        attempt.setPaperId(publish.getPaperId());
        attempt.setStudentUserId(studentUserId);
        attempt.setStatus(AssessmentAttemptStatus.IN_PROGRESS.name());
        attempt.setStartedAt(now);
        attempt.setExpiresAt(resolveAttemptExpiresAt(publish, now));
        attempt.setAnsweredCount(0);
        attempt.setObjectiveScore(0);
        attempt.setTotalScore(0);
        attempt.setLastSavedAt(now);
        try {
            assessmentAttemptMapper.insert(attempt);
        } catch (DataIntegrityViolationException exception) {
            AssessmentAttemptStartVO concurrentAttempt = resumeExistingAttempt(publish, studentUserId, now);
            if (concurrentAttempt != null) {
                return concurrentAttempt;
            }
            throw exception;
        }

        int questionOrder = 1;
        for (AssessmentQuestionEntity question : questions) {
            AssessmentAttemptAnswerEntity answer = new AssessmentAttemptAnswerEntity();
            answer.setAttemptId(attempt.getId());
            answer.setQuestionId(question.getId());
            answer.setQuestionOrder(questionOrder++);
            answer.setQuestionType(question.getQuestionType());
            answer.setStemTextSnapshot(question.getStemText());
            answer.setPromptTextSnapshot(question.getPromptText());
            answer.setOptionsJsonSnapshot(question.getOptionsJson());
            answer.setCorrectAnswerJson(question.getCorrectAnswerJson());
            answer.setExplanationTextSnapshot(question.getExplanationText());
            answer.setQuestionScore(question.getScore());
            answer.setAnswered(Boolean.FALSE);
            answer.setCorrect(null);
            answer.setScoreAwarded(null);
            assessmentAttemptAnswerMapper.insert(answer);
        }

        return new AssessmentAttemptStartVO(attempt.getId(), publishId, AssessmentAttemptStatus.fromCode(attempt.getStatus()), false);
    }

    public AssessmentAttemptDetailVO getAttemptDetail(Long attemptId) {
        AttemptBundle bundle = requireAccessibleAttempt(attemptId);
        LocalDateTime now = LocalDateTime.now();
        AssessmentAttemptEntity attempt = finalizeExpiredAttemptIfNecessary(bundle.attempt(), bundle.publish(), now);
        List<AssessmentAttemptAnswerEntity> answers = loadAttemptAnswers(attempt.getId());

        return new AssessmentAttemptDetailVO(
                attempt.getId(),
                bundle.publish().getId(),
                bundle.publish().getPaperId(),
                bundle.publish().getPaperTitleSnapshot(),
                bundle.publish().getPaperDescriptionSnapshot(),
                bundle.teachingClass().getClassName(),
                AssessmentAttemptStatus.fromCode(attempt.getStatus()),
                bundle.publish().getInstructionsText(),
                bundle.publish().getDurationMinutes(),
                bundle.publish().getQuestionCountSnapshot(),
                attempt.getAnsweredCount(),
                bundle.publish().getTotalScoreSnapshot(),
                attempt.getStartedAt(),
                attempt.getExpiresAt(),
                attempt.getSubmittedAt(),
                attempt.getLastSavedAt(),
                now,
                answers.stream().map(this::toAttemptQuestion).toList()
        );
    }

    @Transactional
    public AssessmentAttemptProgressVO saveResponses(Long attemptId, SaveAssessmentResponsesRequest request) {
        AttemptBundle bundle = requireAccessibleAttempt(attemptId);
        LocalDateTime now = LocalDateTime.now();
        AssessmentAttemptEntity attempt = finalizeExpiredAttemptIfNecessary(bundle.attempt(), bundle.publish(), now);
        if (!AssessmentAttemptStatus.IN_PROGRESS.name().equalsIgnoreCase(attempt.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "Assessment attempt has already been submitted", 409);
        }

        Map<Integer, AssessmentAttemptResponseRequest> requestByOrder = new LinkedHashMap<>();
        for (AssessmentAttemptResponseRequest response : request.responses()) {
            if (requestByOrder.put(response.questionOrder(), response) != null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "Duplicate question order in responses", 400);
            }
        }

        List<AssessmentAttemptAnswerEntity> answers = loadAttemptAnswers(attempt.getId());
        Map<Integer, AssessmentAttemptAnswerEntity> answerByOrder = answers.stream()
                .collect(Collectors.toMap(AssessmentAttemptAnswerEntity::getQuestionOrder, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        for (AssessmentAttemptResponseRequest response : requestByOrder.values()) {
            AssessmentAttemptAnswerEntity answer = answerByOrder.get(response.questionOrder());
            if (answer == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "Assessment question was not found", 404);
            }
            applyResponse(answer, response.responses());
            assessmentAttemptAnswerMapper.updateById(answer);
        }

        recomputeAttemptProgress(attempt, answers, now);
        assessmentAttemptMapper.updateById(attempt);
        return new AssessmentAttemptProgressVO(attempt.getId(), AssessmentAttemptStatus.fromCode(attempt.getStatus()), attempt.getAnsweredCount(), attempt.getLastSavedAt());
    }

    @Transactional
    public AssessmentAttemptSubmitVO submitAttempt(Long attemptId) {
        AttemptBundle bundle = requireAccessibleAttempt(attemptId);
        LocalDateTime now = LocalDateTime.now();
        AssessmentAttemptEntity attempt = finalizeExpiredAttemptIfNecessary(bundle.attempt(), bundle.publish(), now);
        if (!AssessmentAttemptStatus.IN_PROGRESS.name().equalsIgnoreCase(attempt.getStatus())) {
            return new AssessmentAttemptSubmitVO(attempt.getId(), AssessmentAttemptStatus.fromCode(attempt.getStatus()), attempt.getSubmittedAt());
        }
        AssessmentAttemptEntity submitted = submitAttemptInternal(attempt, now);
        notificationScenarioService.notifyAssessmentSubmitted(submitted, bundle.publish(), bundle.teachingClass());
        return new AssessmentAttemptSubmitVO(submitted.getId(), AssessmentAttemptStatus.fromCode(submitted.getStatus()), submitted.getSubmittedAt());
    }

    public AssessmentAttemptResultVO getAttemptResult(Long attemptId) {
        AttemptBundle bundle = requireAccessibleAttempt(attemptId);
        LocalDateTime now = LocalDateTime.now();
        AssessmentAttemptEntity attempt = finalizeExpiredAttemptIfNecessary(bundle.attempt(), bundle.publish(), now);
        if (!AssessmentAttemptStatus.SUBMITTED.name().equalsIgnoreCase(attempt.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "Assessment attempt is still in progress", 409);
        }

        List<AssessmentAttemptAnswerEntity> answers = loadAttemptAnswers(attempt.getId());
        int correctCount = (int) answers.stream().filter(answer -> Boolean.TRUE.equals(answer.getCorrect())).count();
        return new AssessmentAttemptResultVO(
                attempt.getId(),
                bundle.publish().getId(),
                bundle.publish().getPaperId(),
                bundle.publish().getPaperTitleSnapshot(),
                bundle.publish().getPaperDescriptionSnapshot(),
                bundle.teachingClass().getClassName(),
                AssessmentAttemptStatus.fromCode(attempt.getStatus()),
                bundle.publish().getInstructionsText(),
                bundle.publish().getQuestionCountSnapshot(),
                attempt.getAnsweredCount(),
                correctCount,
                attempt.getObjectiveScore(),
                attempt.getTotalScore(),
                attempt.getStartedAt(),
                attempt.getExpiresAt(),
                attempt.getSubmittedAt(),
                answers.stream().map(this::toAttemptResultQuestion).toList()
        );
    }

    public AssessmentPublishDetailVO getPublishDetail(Long publishId) {
        AssessmentPublishEntity publish = requireAccessiblePublishForTeacher(publishId);
        LocalDateTime now = LocalDateTime.now();
        TeachingClassEntity teachingClass = teachingClassMapper.selectById(publish.getTeachingClassId());
        List<AssessmentPublishRecipientEntity> recipients = loadRecipientsByPublish(publishId);
        Map<Long, AssessmentAttemptEntity> attemptByStudentId = loadAttemptMapByPublishAndStudent(publishId, recipients);
        Map<Long, String> studentNameMap = loadUserDisplayNameMap(
                recipients.stream().map(AssessmentPublishRecipientEntity::getStudentUserId).toList()
        );

        List<AssessmentPublishRosterItemVO> roster = new ArrayList<>();
        int submittedCount = 0;
        int inProgressCount = 0;
        int totalSubmittedScore = 0;

        for (AssessmentPublishRecipientEntity recipient : recipients) {
            AssessmentAttemptEntity attempt = attemptByStudentId.get(recipient.getStudentUserId());
            AssessmentAttemptEntity effectiveAttempt = attempt == null
                    ? null
                    : finalizeExpiredAttemptIfNecessary(attempt, publish, now);
            String status = effectiveAttempt == null ? "NOT_STARTED" : effectiveAttempt.getStatus();
            if (AssessmentAttemptStatus.SUBMITTED.name().equalsIgnoreCase(status)) {
                submittedCount++;
                totalSubmittedScore += effectiveAttempt.getTotalScore() == null ? 0 : effectiveAttempt.getTotalScore();
            } else if (AssessmentAttemptStatus.IN_PROGRESS.name().equalsIgnoreCase(status)) {
                inProgressCount++;
            }
            roster.add(new AssessmentPublishRosterItemVO(
                    recipient.getStudentUserId(),
                    studentNameMap.getOrDefault(recipient.getStudentUserId(), "未知学生"),
                    status,
                    effectiveAttempt == null ? null : effectiveAttempt.getId(),
                    effectiveAttempt == null ? null : effectiveAttempt.getAnsweredCount(),
                    publish.getQuestionCountSnapshot(),
                    effectiveAttempt == null ? null : effectiveAttempt.getObjectiveScore(),
                    effectiveAttempt == null ? null : effectiveAttempt.getTotalScore(),
                    effectiveAttempt == null ? null : effectiveAttempt.getStartedAt(),
                    effectiveAttempt == null ? null : effectiveAttempt.getExpiresAt(),
                    effectiveAttempt == null ? null : effectiveAttempt.getSubmittedAt(),
                    effectiveAttempt == null ? null : effectiveAttempt.getLastSavedAt()
            ));
        }

        roster.sort(Comparator
                .comparing(AssessmentPublishRosterItemVO::attemptStatus, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(AssessmentPublishRosterItemVO::studentName, Comparator.nullsLast(String::compareToIgnoreCase)));

        int assignedCount = recipients.size();
        int notStartedCount = Math.max(0, assignedCount - submittedCount - inProgressCount);
        Double averageScore = submittedCount == 0 ? null : (double) totalSubmittedScore / submittedCount;
        return new AssessmentPublishDetailVO(
                publish.getId(),
                publish.getPaperId(),
                publish.getPaperTitleSnapshot(),
                publish.getPaperDescriptionSnapshot(),
                publish.getTeachingClassId(),
                teachingClass == null ? "未知班级" : teachingClass.getClassName(),
                publish.getStatus(),
                publish.getDurationMinutes(),
                publish.getQuestionCountSnapshot(),
                publish.getTotalScoreSnapshot(),
                publish.getInstructionsText(),
                publish.getStartsAt(),
                publish.getDueAt(),
                publish.getPublishedAt(),
                assignedCount,
                notStartedCount,
                inProgressCount,
                submittedCount,
                averageScore,
                List.copyOf(roster)
        );
    }

    public TeacherAssessmentAttemptResultVO getTeacherAttemptResult(Long attemptId) {
        AttemptBundle bundle = requireAccessibleAttemptForTeacher(attemptId);
        LocalDateTime now = LocalDateTime.now();
        AssessmentAttemptEntity attempt = finalizeExpiredAttemptIfNecessary(bundle.attempt(), bundle.publish(), now);
        if (!AssessmentAttemptStatus.SUBMITTED.name().equalsIgnoreCase(attempt.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "Assessment attempt is still in progress", 409);
        }

        List<AssessmentAttemptAnswerEntity> answers = loadAttemptAnswers(attempt.getId());
        int correctCount = (int) answers.stream().filter(answer -> Boolean.TRUE.equals(answer.getCorrect())).count();
        UserEntity student = userMapper.selectById(attempt.getStudentUserId());
        return new TeacherAssessmentAttemptResultVO(
                attempt.getId(),
                bundle.publish().getId(),
                bundle.publish().getPaperId(),
                bundle.publish().getTeachingClassId(),
                attempt.getStudentUserId(),
                resolveUserDisplayName(student),
                bundle.publish().getPaperTitleSnapshot(),
                bundle.publish().getPaperDescriptionSnapshot(),
                bundle.teachingClass().getClassName(),
                AssessmentAttemptStatus.fromCode(attempt.getStatus()),
                bundle.publish().getInstructionsText(),
                bundle.publish().getQuestionCountSnapshot(),
                attempt.getAnsweredCount(),
                correctCount,
                attempt.getObjectiveScore(),
                attempt.getTotalScore(),
                attempt.getStartedAt(),
                attempt.getExpiresAt(),
                attempt.getSubmittedAt(),
                answers.stream().map(this::toAttemptResultQuestion).toList()
        );
    }

    private AssessmentPaperDetailVO buildPaperDetail(AssessmentPaperEntity paper) {
        List<AssessmentQuestionEntity> questions = loadQuestionsByPaper(paper.getId());
        List<AssessmentPublishEntity> publishes = assessmentPublishMapper.selectList(Wrappers.<AssessmentPublishEntity>lambdaQuery()
                .eq(AssessmentPublishEntity::getPaperId, paper.getId())
                .orderByDesc(AssessmentPublishEntity::getPublishedAt)
                .orderByDesc(AssessmentPublishEntity::getId));

        Map<Long, TeachingClassEntity> classMap = loadTeachingClassMap(
                publishes.stream().map(AssessmentPublishEntity::getTeachingClassId).toList()
        );
        Map<Long, List<AssessmentAttemptEntity>> attemptsByPublishId = loadAttemptGroupsByPublishId(publishes);
        Map<Long, Integer> assignedCountByPublishId = loadAssignedCountByPublishId(publishes.stream()
                .map(AssessmentPublishEntity::getId)
                .toList());
        LocalDateTime now = LocalDateTime.now();

        List<AssessmentPublishSummaryVO> publishSummaries = publishes.stream()
                .map(publish -> {
                    List<AssessmentAttemptEntity> attempts = attemptsByPublishId.getOrDefault(publish.getId(), List.of());
                    TeachingClassEntity teachingClass = classMap.get(publish.getTeachingClassId());
                    PublishStats stats = calculatePublishStats(publish, attempts, assignedCountByPublishId.getOrDefault(publish.getId(), 0), now);
                    return buildPublishSummary(
                            publish,
                            teachingClass == null ? "未知班级" : teachingClass.getClassName(),
                            stats.assignedCount(),
                            stats.attemptCount(),
                            stats.submittedCount()
                    );
                })
                .toList();

        return new AssessmentPaperDetailVO(
                paper.getId(),
                paper.getPaperCode(),
                paper.getTitle(),
                paper.getDescription(),
                paper.getStatus(),
                paper.getDurationMinutes(),
                paper.getQuestionCount(),
                paper.getTotalScore(),
                paper.getLatestPublishAt(),
                questions.stream().map(this::toPaperQuestion).toList(),
                publishSummaries
        );
    }

    private AssessmentPublishSummaryVO buildPublishSummary(
            AssessmentPublishEntity publish,
            String className,
            Integer assignedCount,
            Integer attemptCount,
            Integer submittedCount
    ) {
        int safeAssignedCount = assignedCount == null ? 0 : assignedCount;
        int safeSubmittedCount = submittedCount == null ? 0 : submittedCount;
        return new AssessmentPublishSummaryVO(
                publish.getId(),
                publish.getTeachingClassId(),
                className,
                publish.getStatus(),
                publish.getDurationMinutes(),
                publish.getQuestionCountSnapshot(),
                publish.getTotalScoreSnapshot(),
                publish.getInstructionsText(),
                publish.getStartsAt(),
                publish.getDueAt(),
                publish.getPublishedAt(),
                safeAssignedCount,
                attemptCount,
                safeSubmittedCount,
                Math.max(0, safeAssignedCount - safeSubmittedCount)
        );
    }

    private AssessmentPaperSummaryVO toPaperSummary(AssessmentPaperEntity paper) {
        return new AssessmentPaperSummaryVO(
                paper.getId(),
                paper.getPaperCode(),
                paper.getTitle(),
                paper.getDescription(),
                paper.getStatus(),
                paper.getDurationMinutes(),
                paper.getQuestionCount(),
                paper.getTotalScore(),
                paper.getLatestPublishAt(),
                paper.getUpdatedAt()
        );
    }

    private AssessmentPaperQuestionVO toPaperQuestion(AssessmentQuestionEntity question) {
        return new AssessmentPaperQuestionVO(
                question.getId(),
                question.getQuestionType(),
                question.getSortOrder(),
                question.getStemText(),
                question.getPromptText(),
                assessmentJsonCodec.readOptions(question.getOptionsJson()).stream()
                        .map(option -> new AssessmentOptionVO(option.key(), option.label()))
                        .toList(),
                assessmentJsonCodec.readStringList(question.getCorrectAnswerJson()),
                question.getExplanationText(),
                question.getScore()
        );
    }

    private AssessmentAttemptQuestionVO toAttemptQuestion(AssessmentAttemptAnswerEntity answer) {
        return new AssessmentAttemptQuestionVO(
                answer.getId(),
                answer.getQuestionId(),
                answer.getQuestionOrder(),
                answer.getQuestionType(),
                answer.getStemTextSnapshot(),
                answer.getPromptTextSnapshot(),
                assessmentJsonCodec.readOptions(answer.getOptionsJsonSnapshot()).stream()
                        .map(option -> new AssessmentOptionVO(option.key(), option.label()))
                        .toList(),
                answer.getQuestionScore(),
                assessmentJsonCodec.readStringList(answer.getResponseJson()),
                Boolean.TRUE.equals(answer.getAnswered())
        );
    }

    private AssessmentAttemptResultQuestionVO toAttemptResultQuestion(AssessmentAttemptAnswerEntity answer) {
        return new AssessmentAttemptResultQuestionVO(
                answer.getId(),
                answer.getQuestionId(),
                answer.getQuestionOrder(),
                answer.getQuestionType(),
                answer.getStemTextSnapshot(),
                answer.getPromptTextSnapshot(),
                assessmentJsonCodec.readOptions(answer.getOptionsJsonSnapshot()).stream()
                        .map(option -> new AssessmentOptionVO(option.key(), option.label()))
                        .toList(),
                answer.getQuestionScore(),
                assessmentJsonCodec.readStringList(answer.getResponseJson()),
                assessmentJsonCodec.readStringList(answer.getCorrectAnswerJson()),
                answer.getCorrect(),
                answer.getScoreAwarded(),
                answer.getExplanationTextSnapshot()
        );
    }

    private void replacePaperQuestions(Long paperId, List<NormalizedQuestion> normalizedQuestions) {
        assessmentQuestionMapper.delete(Wrappers.<AssessmentQuestionEntity>lambdaQuery()
                .eq(AssessmentQuestionEntity::getPaperId, paperId));

        int sortOrder = 1;
        for (NormalizedQuestion question : normalizedQuestions) {
            AssessmentQuestionEntity entity = new AssessmentQuestionEntity();
            entity.setPaperId(paperId);
            entity.setQuestionType(question.questionType().name());
            entity.setSortOrder(sortOrder++);
            entity.setStemText(question.stemText());
            entity.setPromptText(question.promptText());
            entity.setOptionsJson(question.options().isEmpty() ? null : assessmentJsonCodec.write(question.options()));
            entity.setCorrectAnswerJson(assessmentJsonCodec.write(question.correctAnswers()));
            entity.setExplanationText(question.explanationText());
            entity.setScore(question.score());
            assessmentQuestionMapper.insert(entity);
        }
    }

    private List<NormalizedQuestion> normalizeQuestions(List<AssessmentQuestionRequest> questions) {
        List<NormalizedQuestion> normalizedQuestions = new ArrayList<>();
        for (AssessmentQuestionRequest question : questions) {
            AssessmentQuestionType questionType = parseQuestionType(question.questionType());
            String stemText = normalizeRequiredText(question.stemText(), "stemText");
            String promptText = normalizeOptionalText(question.promptText());
            String explanationText = normalizeOptionalText(question.explanationText());

            if (questionType == AssessmentQuestionType.FILL_BLANK) {
                List<String> correctAnswers = normalizeFillBlankAnswers(question.correctAnswers());
                normalizedQuestions.add(new NormalizedQuestion(
                        questionType,
                        stemText,
                        promptText,
                        List.of(),
                        correctAnswers,
                        explanationText,
                        question.score()
                ));
                continue;
            }

            List<AssessmentOptionPayload> options = normalizeChoiceOptions(question.options());
            Map<String, String> canonicalOptionKeys = options.stream()
                    .collect(Collectors.toMap(
                            option -> option.key().toUpperCase(Locale.ROOT),
                            AssessmentOptionPayload::key,
                            (left, right) -> left,
                            LinkedHashMap::new
                    ));
            List<String> correctAnswers = normalizeChoiceCorrectAnswers(question.correctAnswers(), canonicalOptionKeys);
            if (questionType == AssessmentQuestionType.SINGLE_CHOICE && correctAnswers.size() != 1) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "Single choice question must contain exactly one correct answer", 400);
            }
            normalizedQuestions.add(new NormalizedQuestion(
                    questionType,
                    stemText,
                    promptText,
                    options,
                    correctAnswers,
                    explanationText,
                    question.score()
            ));
        }
        return normalizedQuestions;
    }

    private List<AssessmentOptionPayload> normalizeChoiceOptions(List<AssessmentOptionRequest> options) {
        if (options == null || options.size() < 2) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Choice question must contain at least two options", 400);
        }
        List<AssessmentOptionPayload> normalizedOptions = new ArrayList<>();
        Set<String> seenKeys = new LinkedHashSet<>();
        for (AssessmentOptionRequest option : options) {
            String key = normalizeRequiredText(option.key(), "option.key").toUpperCase(Locale.ROOT);
            String label = normalizeRequiredText(option.label(), "option.label");
            if (!seenKeys.add(key)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "Choice option keys must be unique", 400);
            }
            normalizedOptions.add(new AssessmentOptionPayload(key, label));
        }
        return normalizedOptions;
    }

    private List<String> normalizeChoiceCorrectAnswers(List<String> correctAnswers, Map<String, String> canonicalOptionKeys) {
        LinkedHashSet<String> normalizedAnswers = new LinkedHashSet<>();
        for (String answer : correctAnswers) {
            String normalized = normalizeRequiredText(answer, "correctAnswers").toUpperCase(Locale.ROOT);
            String canonicalKey = canonicalOptionKeys.get(normalized);
            if (canonicalKey == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "Correct answers must reference existing option keys", 400);
            }
            normalizedAnswers.add(canonicalKey);
        }
        return List.copyOf(normalizedAnswers);
    }

    private List<String> normalizeFillBlankAnswers(List<String> correctAnswers) {
        LinkedHashSet<String> normalizedAnswers = new LinkedHashSet<>();
        for (String answer : correctAnswers) {
            normalizedAnswers.add(normalizeRequiredText(answer, "correctAnswers"));
        }
        return List.copyOf(normalizedAnswers);
    }

    private void applyResponse(AssessmentAttemptAnswerEntity answer, List<String> rawResponses) {
        AssessmentQuestionType questionType = parseQuestionType(answer.getQuestionType());
        List<String> normalizedResponses = normalizeAttemptResponses(questionType, rawResponses, answer.getOptionsJsonSnapshot());
        List<String> correctAnswers = assessmentJsonCodec.readStringList(answer.getCorrectAnswerJson());
        boolean answered = !normalizedResponses.isEmpty();
        Boolean correct = answered ? isResponseCorrect(questionType, normalizedResponses, correctAnswers) : null;

        answer.setResponseJson(answered ? assessmentJsonCodec.write(normalizedResponses) : null);
        answer.setAnswered(answered);
        answer.setCorrect(correct);
        answer.setScoreAwarded(Boolean.TRUE.equals(correct) ? answer.getQuestionScore() : answered ? 0 : null);
    }

    private List<String> normalizeAttemptResponses(
            AssessmentQuestionType questionType,
            List<String> rawResponses,
            String optionsJson
    ) {
        if (rawResponses == null || rawResponses.isEmpty()) {
            return List.of();
        }
        if (questionType == AssessmentQuestionType.FILL_BLANK) {
            LinkedHashSet<String> values = rawResponses.stream()
                    .map(this::normalizeOptionalText)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (values.size() > 1) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "Fill blank question accepts only one response", 400);
            }
            return List.copyOf(values);
        }

        Map<String, String> validOptionKeys = assessmentJsonCodec.readOptions(optionsJson).stream()
                .collect(Collectors.toMap(
                        option -> option.key().toUpperCase(Locale.ROOT),
                        AssessmentOptionPayload::key,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String rawResponse : rawResponses) {
            String normalized = normalizeRequiredText(rawResponse, "responses").toUpperCase(Locale.ROOT);
            String canonicalKey = validOptionKeys.get(normalized);
            if (canonicalKey == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "Response references an unknown option", 400);
            }
            values.add(canonicalKey);
        }
        if (questionType == AssessmentQuestionType.SINGLE_CHOICE && values.size() > 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Single choice question accepts only one response", 400);
        }
        return List.copyOf(values);
    }

    private boolean isResponseCorrect(
            AssessmentQuestionType questionType,
            List<String> normalizedResponses,
            List<String> correctAnswers
    ) {
        if (questionType == AssessmentQuestionType.FILL_BLANK) {
            Set<String> correctSet = correctAnswers.stream()
                    .map(this::normalizeFillBlankValue)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            return normalizedResponses.stream()
                    .map(this::normalizeFillBlankValue)
                    .filter(Objects::nonNull)
                    .anyMatch(correctSet::contains);
        }
        return new LinkedHashSet<>(normalizedResponses).equals(new LinkedHashSet<>(correctAnswers));
    }

    private void recomputeAttemptProgress(AssessmentAttemptEntity attempt, List<AssessmentAttemptAnswerEntity> answers, LocalDateTime now) {
        int answeredCount = 0;
        int objectiveScore = 0;
        for (AssessmentAttemptAnswerEntity answer : answers) {
            if (Boolean.TRUE.equals(answer.getAnswered())) {
                answeredCount++;
            }
            if (answer.getScoreAwarded() != null) {
                objectiveScore += answer.getScoreAwarded();
            }
        }
        attempt.setAnsweredCount(answeredCount);
        attempt.setObjectiveScore(objectiveScore);
        attempt.setTotalScore(objectiveScore);
        attempt.setLastSavedAt(now);
    }

    private AssessmentAttemptEntity submitAttemptInternal(AssessmentAttemptEntity attempt, LocalDateTime now) {
        List<AssessmentAttemptAnswerEntity> answers = loadAttemptAnswers(attempt.getId());
        for (AssessmentAttemptAnswerEntity answer : answers) {
            List<String> responses = assessmentJsonCodec.readStringList(answer.getResponseJson());
            applyResponse(answer, responses);
            assessmentAttemptAnswerMapper.updateById(answer);
        }
        recomputeAttemptProgress(attempt, answers, now);
        attempt.setStatus(AssessmentAttemptStatus.SUBMITTED.name());
        attempt.setSubmittedAt(now);
        assessmentAttemptMapper.updateById(attempt);
        return attempt;
    }

    private AssessmentAttemptEntity finalizeExpiredAttemptIfNecessary(
            AssessmentAttemptEntity attempt,
            AssessmentPublishEntity publish,
            LocalDateTime now
    ) {
        if (!AssessmentAttemptStatus.IN_PROGRESS.name().equalsIgnoreCase(attempt.getStatus())) {
            return attempt;
        }
        if (!isAttemptExpired(attempt, publish, now)) {
            return attempt;
        }
        return submitAttemptInternal(attempt, now);
    }

    private boolean isAttemptExpired(AssessmentAttemptEntity attempt, AssessmentPublishEntity publish, LocalDateTime now) {
        if (!AssessmentAttemptStatus.IN_PROGRESS.name().equalsIgnoreCase(attempt.getStatus())) {
            return false;
        }
        if (attempt.getExpiresAt() != null && !now.isBefore(attempt.getExpiresAt())) {
            return true;
        }
        return publish.getDueAt() != null && !now.isBefore(publish.getDueAt());
    }

    private void snapshotRecipients(AssessmentPublishEntity publish) {
        List<Long> studentIds = teachingClassService.listActiveStudentIds(
                publish.getTeachingClassId(),
                publish.getPublishedAt() == null ? LocalDateTime.now() : publish.getPublishedAt()
        );
        for (Long studentId : studentIds) {
            AssessmentPublishRecipientEntity recipient = new AssessmentPublishRecipientEntity();
            recipient.setPublishId(publish.getId());
            recipient.setPaperId(publish.getPaperId());
            recipient.setTeachingClassId(publish.getTeachingClassId());
            recipient.setStudentUserId(studentId);
            assessmentPublishRecipientMapper.insert(recipient);
        }
    }

    private List<AssessmentPublishRecipientEntity> loadRecipientsByPublish(Long publishId) {
        return assessmentPublishRecipientMapper.selectList(Wrappers.<AssessmentPublishRecipientEntity>lambdaQuery()
                .eq(AssessmentPublishRecipientEntity::getPublishId, publishId)
                .orderByAsc(AssessmentPublishRecipientEntity::getStudentUserId)
                .orderByAsc(AssessmentPublishRecipientEntity::getId));
    }

    private Map<Long, Integer> loadAssignedCountByPublishId(Collection<Long> publishIds) {
        LinkedHashSet<Long> deduplicated = publishIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (deduplicated.isEmpty()) {
            return Map.of();
        }
        return assessmentPublishRecipientMapper.selectList(Wrappers.<AssessmentPublishRecipientEntity>lambdaQuery()
                        .in(AssessmentPublishRecipientEntity::getPublishId, deduplicated))
                .stream()
                .collect(Collectors.groupingBy(
                        AssessmentPublishRecipientEntity::getPublishId,
                        LinkedHashMap::new,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));
    }

    private Map<Long, List<AssessmentAttemptEntity>> loadAttemptGroupsByPublishId(List<AssessmentPublishEntity> publishes) {
        if (publishes.isEmpty()) {
            return Map.of();
        }
        return assessmentAttemptMapper.selectList(Wrappers.<AssessmentAttemptEntity>lambdaQuery()
                        .in(AssessmentAttemptEntity::getPublishId, publishes.stream().map(AssessmentPublishEntity::getId).toList()))
                .stream()
                .collect(Collectors.groupingBy(
                        AssessmentAttemptEntity::getPublishId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private Map<Long, AssessmentAttemptEntity> loadAttemptMapByPublishAndStudent(
            Long publishId,
            List<AssessmentPublishRecipientEntity> recipients
    ) {
        if (recipients.isEmpty()) {
            return Map.of();
        }
        return assessmentAttemptMapper.selectList(Wrappers.<AssessmentAttemptEntity>lambdaQuery()
                        .eq(AssessmentAttemptEntity::getPublishId, publishId)
                        .in(AssessmentAttemptEntity::getStudentUserId, recipients.stream()
                                .map(AssessmentPublishRecipientEntity::getStudentUserId)
                                .toList()))
                .stream()
                .collect(Collectors.toMap(
                        AssessmentAttemptEntity::getStudentUserId,
                        Function.identity(),
                        (left, right) -> {
                            if (left.getStartedAt() == null) {
                                return right;
                            }
                            if (right.getStartedAt() == null) {
                                return left;
                            }
                            return right.getStartedAt().isAfter(left.getStartedAt()) ? right : left;
                        },
                        LinkedHashMap::new
                ));
    }

    private Map<Long, AssessmentPublishEntity> loadPublishMap(Collection<Long> publishIds) {
        LinkedHashSet<Long> deduplicated = publishIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (deduplicated.isEmpty()) {
            return Map.of();
        }
        return assessmentPublishMapper.selectBatchIds(deduplicated).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        AssessmentPublishEntity::getId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private Map<Long, String> loadUserDisplayNameMap(Collection<Long> userIds) {
        LinkedHashSet<Long> deduplicated = userIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (deduplicated.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(deduplicated).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        UserEntity::getId,
                        this::resolveUserDisplayName,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private String resolveUserDisplayName(UserEntity user) {
        if (user == null) {
            return "未知用户";
        }
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
            return user.getDisplayName();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return "用户 #" + user.getId();
    }

    private PublishStats calculatePublishStats(
            AssessmentPublishEntity publish,
            List<AssessmentAttemptEntity> attempts,
            int assignedCount,
            LocalDateTime now
    ) {
        int submittedCount = 0;
        int inProgressCount = 0;
        for (AssessmentAttemptEntity attempt : attempts) {
            if (AssessmentAttemptStatus.SUBMITTED.name().equalsIgnoreCase(attempt.getStatus())
                    || isAttemptExpired(attempt, publish, now)) {
                submittedCount++;
            } else if (AssessmentAttemptStatus.IN_PROGRESS.name().equalsIgnoreCase(attempt.getStatus())) {
                inProgressCount++;
            }
        }
        return new PublishStats(
                assignedCount,
                attempts.size(),
                submittedCount,
                Math.max(0, assignedCount - submittedCount - inProgressCount),
                inProgressCount
        );
    }

    private AssessmentAttemptEntity loadAttemptByPublishAndStudent(Long publishId, Long studentUserId) {
        return assessmentAttemptMapper.selectOne(Wrappers.<AssessmentAttemptEntity>lambdaQuery()
                .eq(AssessmentAttemptEntity::getPublishId, publishId)
                .eq(AssessmentAttemptEntity::getStudentUserId, studentUserId)
                .orderByDesc(AssessmentAttemptEntity::getId)
                .last("LIMIT 1"));
    }

    private AssessmentAttemptStartVO resumeExistingAttempt(
            AssessmentPublishEntity publish,
            Long studentUserId,
            LocalDateTime now
    ) {
        AssessmentAttemptEntity existingAttempt = loadAttemptByPublishAndStudent(publish.getId(), studentUserId);
        if (existingAttempt == null) {
            return null;
        }
        AssessmentAttemptEntity effectiveAttempt = finalizeExpiredAttemptIfNecessary(existingAttempt, publish, now);
        return new AssessmentAttemptStartVO(
                effectiveAttempt.getId(),
                publish.getId(),
                AssessmentAttemptStatus.fromCode(effectiveAttempt.getStatus()),
                true
        );
    }

    private AttemptBundle requireAccessibleAttempt(Long attemptId) {
        AssessmentAttemptEntity attempt = assessmentAttemptMapper.selectById(attemptId);
        if (attempt == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Assessment attempt was not found", 404);
        }
        Long currentUserId = currentUserId();
        if (!isAdmin() && !Objects.equals(attempt.getStudentUserId(), currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "You do not have access to this assessment attempt", 403);
        }
        AssessmentPublishEntity publish = assessmentPublishMapper.selectById(attempt.getPublishId());
        if (publish == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Assessment publish was not found", 404);
        }
        TeachingClassEntity teachingClass = teachingClassMapper.selectById(publish.getTeachingClassId());
        if (teachingClass == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Teaching class was not found", 404);
        }
        return new AttemptBundle(attempt, publish, teachingClass);
    }

    private AttemptBundle requireAccessibleAttemptForTeacher(Long attemptId) {
        AssessmentAttemptEntity attempt = assessmentAttemptMapper.selectById(attemptId);
        if (attempt == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Assessment attempt was not found", 404);
        }
        AssessmentPublishEntity publish = requireAccessiblePublishForTeacher(attempt.getPublishId());
        TeachingClassEntity teachingClass = teachingClassMapper.selectById(publish.getTeachingClassId());
        if (teachingClass == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Teaching class was not found", 404);
        }
        return new AttemptBundle(attempt, publish, teachingClass);
    }

    private AssessmentPublishEntity requireAccessiblePublishForStudent(Long publishId, Long studentUserId) {
        AssessmentPublishEntity publish = assessmentPublishMapper.selectById(publishId);
        if (publish == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Assessment publish was not found", 404);
        }
        if (!AssessmentPublishStatus.PUBLISHED.name().equalsIgnoreCase(publish.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "Assessment publish is not available", 409);
        }
        AssessmentPublishRecipientEntity recipient = assessmentPublishRecipientMapper.selectOne(
                Wrappers.<AssessmentPublishRecipientEntity>lambdaQuery()
                        .eq(AssessmentPublishRecipientEntity::getPublishId, publishId)
                        .eq(AssessmentPublishRecipientEntity::getStudentUserId, studentUserId)
                        .last("LIMIT 1")
        );
        if (recipient == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Assessment publish was not assigned to this student", 404);
        }
        return publish;
    }

    private AssessmentPublishEntity requireAccessiblePublishForTeacher(Long publishId) {
        AssessmentPublishEntity publish = assessmentPublishMapper.selectById(publishId);
        if (publish == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Assessment publish was not found", 404);
        }
        teachingClassService.requireAccessibleClass(publish.getTeachingClassId());
        return publish;
    }

    private AssessmentPaperEntity requireAccessiblePaper(Long paperId) {
        AssessmentPaperEntity paper = assessmentPaperMapper.selectById(paperId);
        if (paper == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Assessment paper was not found", 404);
        }
        if (!isAdmin() && !Objects.equals(paper.getOwnerUserId(), currentUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "You do not have access to this assessment paper", 403);
        }
        return paper;
    }

    private AssessmentPaperEntity requireEditablePaper(Long paperId) {
        AssessmentPaperEntity paper = requireAccessiblePaper(paperId);
        Long publishCount = assessmentPublishMapper.selectCount(Wrappers.<AssessmentPublishEntity>lambdaQuery()
                .eq(AssessmentPublishEntity::getPaperId, paperId));
        if (publishCount != null && publishCount > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "Published assessment papers are locked from editing", 409);
        }
        return paper;
    }

    private List<AssessmentQuestionEntity> loadQuestionsByPaper(Long paperId) {
        return assessmentQuestionMapper.selectList(Wrappers.<AssessmentQuestionEntity>lambdaQuery()
                .eq(AssessmentQuestionEntity::getPaperId, paperId)
                .orderByAsc(AssessmentQuestionEntity::getSortOrder)
                .orderByAsc(AssessmentQuestionEntity::getId));
    }

    private List<AssessmentAttemptAnswerEntity> loadAttemptAnswers(Long attemptId) {
        return assessmentAttemptAnswerMapper.selectList(Wrappers.<AssessmentAttemptAnswerEntity>lambdaQuery()
                .eq(AssessmentAttemptAnswerEntity::getAttemptId, attemptId)
                .orderByAsc(AssessmentAttemptAnswerEntity::getQuestionOrder)
                .orderByAsc(AssessmentAttemptAnswerEntity::getId));
    }

    private Map<Long, TeachingClassEntity> loadTeachingClassMap(Collection<Long> classIds) {
        if (classIds.isEmpty()) {
            return Map.of();
        }
        return teachingClassMapper.selectBatchIds(classIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(TeachingClassEntity::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private void requirePublishAvailableForStart(AssessmentPublishEntity publish, LocalDateTime now) {
        if (publish.getStartsAt() != null && now.isBefore(publish.getStartsAt())) {
            throw new BusinessException(ResultCode.CONFLICT, "Assessment has not started yet", 409);
        }
        if (publish.getDueAt() != null && !now.isBefore(publish.getDueAt())) {
            throw new BusinessException(ResultCode.CONFLICT, "Assessment is already closed", 409);
        }
    }

    private LocalDateTime resolveAttemptExpiresAt(AssessmentPublishEntity publish, LocalDateTime startedAt) {
        LocalDateTime durationExpiry = startedAt.plusMinutes(publish.getDurationMinutes());
        if (publish.getDueAt() == null || publish.getDueAt().isAfter(durationExpiry)) {
            return durationExpiry;
        }
        if (!publish.getDueAt().isAfter(startedAt)) {
            throw new BusinessException(ResultCode.CONFLICT, "Assessment is already closed", 409);
        }
        return publish.getDueAt();
    }

    private void validatePublishWindow(LocalDateTime startsAt, LocalDateTime dueAt) {
        if (dueAt != null && !dueAt.isAfter(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Assessment due time must be in the future", 400);
        }
        if (startsAt != null && dueAt != null && !dueAt.isAfter(startsAt)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Assessment due time must be after start time", 400);
        }
    }

    private AssessmentQuestionType parseQuestionType(String value) {
        try {
            return AssessmentQuestionType.fromCode(value);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Unsupported assessment question type", 400);
        }
    }

    private String normalizeRequiredText(String value, String fieldName) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, fieldName + " must not be blank", 400);
        }
        return normalized;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeFillBlankValue(String value) {
        String normalized = normalizeOptionalText(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String generatePaperCode() {
        return "ASM-" + LocalDateTime.now().format(PAPER_CODE_TIME) + "-"
                + ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    private Long currentUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "Authentication required", 401));
    }

    private boolean isAdmin() {
        return SecurityUtils.getCurrentPrincipal()
                .map(principal -> principal.roles().contains("ADMIN"))
                .orElse(false);
    }

    private record AttemptBundle(
            AssessmentAttemptEntity attempt,
            AssessmentPublishEntity publish,
            TeachingClassEntity teachingClass
    ) {
    }

    private record PublishStats(
            int assignedCount,
            int attemptCount,
            int submittedCount,
            int notStartedCount,
            int inProgressCount
    ) {
    }

    private record NormalizedQuestion(
            AssessmentQuestionType questionType,
            String stemText,
            String promptText,
            List<AssessmentOptionPayload> options,
            List<String> correctAnswers,
            String explanationText,
            Integer score
    ) {
    }
}
