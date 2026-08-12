package com.huashi.eftransfer.app.modules.practice.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huashi.eftransfer.app.common.util.SecurityUtils;
import com.huashi.eftransfer.app.modules.assessment.support.AssessmentJsonCodec;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentOptionVO;
import com.huashi.eftransfer.app.modules.practice.dto.PracticeSessionPageQuery;
import com.huashi.eftransfer.app.modules.practice.dto.PracticeSpellingCheckRequest;
import com.huashi.eftransfer.app.modules.practice.dto.SavePracticeDraftRequest;
import com.huashi.eftransfer.app.modules.practice.dto.StartPracticeSessionRequest;
import com.huashi.eftransfer.app.modules.practice.dto.SubmitPracticeRequest;
import com.huashi.eftransfer.app.modules.practice.entity.PracticeSessionAnswerEntity;
import com.huashi.eftransfer.app.modules.practice.entity.PracticeSessionEntity;
import com.huashi.eftransfer.app.modules.practice.mapper.PracticeSessionAnswerMapper;
import com.huashi.eftransfer.app.modules.practice.mapper.PracticeSessionMapper;
import com.huashi.eftransfer.app.modules.practice.support.PracticeSectionCatalog;
import com.huashi.eftransfer.app.modules.practice.support.PracticeSpellingAnalyzer;
import com.huashi.eftransfer.app.modules.practice.vo.PracticeHistoryVO;
import com.huashi.eftransfer.app.modules.practice.vo.PracticeProgressVO;
import com.huashi.eftransfer.app.modules.practice.vo.PracticeQuestionVO;
import com.huashi.eftransfer.app.modules.practice.vo.PracticeResultQuestionVO;
import com.huashi.eftransfer.app.modules.practice.vo.PracticeResultVO;
import com.huashi.eftransfer.app.modules.practice.vo.PracticeSectionMetricVO;
import com.huashi.eftransfer.app.modules.practice.vo.PracticeSessionCreatedVO;
import com.huashi.eftransfer.app.modules.practice.vo.PracticeSessionDetailVO;
import com.huashi.eftransfer.app.modules.practice.vo.PracticeSpellingCheckVO;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import com.huashi.eftransfer.shared.page.PageResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Lifecycle of a student self-practice session: start (snapshot questions),
 * draft saving, spelling hint checks, whole-paper completion grading, result
 * and history. Sessions are untimed and fully self-service.
 */
@Service
public class PracticeSessionService {

    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_COMPLETED = "COMPLETED";

    private final PracticeSessionMapper practiceSessionMapper;
    private final PracticeSessionAnswerMapper practiceSessionAnswerMapper;
    private final PracticeBankService practiceBankService;
    private final AssessmentJsonCodec assessmentJsonCodec;
    private final JdbcTemplate jdbcTemplate;

    public PracticeSessionService(
            PracticeSessionMapper practiceSessionMapper,
            PracticeSessionAnswerMapper practiceSessionAnswerMapper,
            PracticeBankService practiceBankService,
            AssessmentJsonCodec assessmentJsonCodec,
            JdbcTemplate jdbcTemplate
    ) {
        this.practiceSessionMapper = practiceSessionMapper;
        this.practiceSessionAnswerMapper = practiceSessionAnswerMapper;
        this.practiceBankService = practiceBankService;
        this.assessmentJsonCodec = assessmentJsonCodec;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public PracticeSessionCreatedVO createSession(StartPracticeSessionRequest request) {
        Long ownerUserId = currentUserId();
        String sectionCode = normalizeSection(request.sectionCode());
        if (hasActiveSession(ownerUserId)) {
            throw new BusinessException(ResultCode.ACTIVE_SESSION_EXISTS,
                    "An active practice session already exists for this student", 409);
        }
        List<PracticeBankService.BankQuestion> questions = practiceBankService.loadBankQuestions(
                request.bankCode(), sectionCode);
        if (!request.targetWords().isEmpty()) {
            List<String> normalizedWords = request.targetWords().stream()
                    .map(word -> word == null ? "" : word.trim().toLowerCase(java.util.Locale.ROOT))
                    .filter(word -> !word.isBlank())
                    .distinct()
                    .toList();
            questions = questions.stream()
                    .filter(question -> question.targetWord() != null
                            && normalizedWords.contains(question.targetWord().trim().toLowerCase(java.util.Locale.ROOT)))
                    .toList();
            if (questions.isEmpty()) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR,
                        "No practice questions match the requested target words", 400);
            }
        }
        if (questions.isEmpty()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR,
                    "No practice questions are available for this section", 400);
        }
        List<PracticeBankService.BankQuestion> shuffled = new ArrayList<>(questions);
        Collections.shuffle(shuffled, ThreadLocalRandom.current());

        LocalDateTime now = LocalDateTime.now();
        PracticeSessionEntity session = new PracticeSessionEntity();
        session.setOwnerUserId(ownerUserId);
        session.setBankCode(request.bankCode());
        session.setSectionCode(sectionCode);
        session.setStatus(STATUS_IN_PROGRESS);
        session.setTotalCount(shuffled.size());
        session.setAnsweredCount(0);
        session.setCorrectCount(0);
        session.setStartedAt(now);
        practiceSessionMapper.insert(session);

        int order = 1;
        for (PracticeBankService.BankQuestion question : shuffled) {
            PracticeSessionAnswerEntity answer = new PracticeSessionAnswerEntity();
            answer.setSessionId(session.getId());
            answer.setQuestionOrder(order++);
            answer.setQuestionVersionId(question.questionVersionId());
            answer.setQuestionCode(question.questionCode());
            answer.setQuestionType(question.questionType());
            answer.setSectionCode(PracticeSectionCatalog.sectionOfConstruct(question.constructCode()));
            answer.setConstructCode(question.constructCode());
            answer.setTransferCategory(question.transferCategory());
            answer.setTargetWord(question.targetWord());
            answer.setStemTextSnapshot(question.stemText());
            answer.setPromptTextSnapshot(question.promptText());
            answer.setOptionsJsonSnapshot(question.optionsJson());
            answer.setCorrectAnswerJson(question.correctAnswerJson());
            answer.setExplanationTextSnapshot(question.explanationText());
            answer.setOptionExplanationsJson(question.optionExplanationsJson());
            answer.setWrongAttemptCount(0);
            answer.setSpellingHintShown(false);
            answer.setAnsweredAt(null);
            practiceSessionAnswerMapper.insert(answer);
        }
        return new PracticeSessionCreatedVO(session.getId(), session.getBankCode(), sectionCode, shuffled.size());
    }

    @Transactional(readOnly = true)
    public PracticeSessionDetailVO getDetail(Long sessionId) {
        PracticeSessionEntity session = requireOwnedSession(sessionId);
        List<PracticeQuestionVO> questions = loadAnswers(sessionId).stream()
                .map(this::toQuestionVO)
                .toList();
        return new PracticeSessionDetailVO(
                session.getId(),
                session.getBankCode(),
                session.getSectionCode(),
                session.getStatus(),
                session.getTotalCount(),
                session.getAnsweredCount(),
                session.getCorrectCount(),
                session.getStartedAt(),
                session.getCompletedAt(),
                questions
        );
    }

    @Transactional
    public PracticeProgressVO saveDraft(Long sessionId, SavePracticeDraftRequest request) {
        PracticeSessionEntity session = requireOwnedSessionForUpdate(sessionId);
        requireInProgress(session);
        LocalDateTime now = LocalDateTime.now();
        for (SavePracticeDraftRequest.DraftAnswerItem item : request.answers()) {
            PracticeSessionAnswerEntity answer = findAnswerForUpdate(sessionId, item.questionOrder());
            List<String> response = item.response().stream()
                    .map(value -> value == null ? "" : value.trim())
                    .toList();
            boolean hasValue = response.stream().anyMatch(value -> !value.isBlank());
            answer.setResponseJson(hasValue ? assessmentJsonCodec.write(response) : null);
            answer.setAnsweredAt(hasValue ? now : null);
            practiceSessionAnswerMapper.updateById(answer);
        }
        int answeredCount = countAnswered(sessionId);
        practiceSessionMapper.syncAnsweredCount(sessionId, answeredCount);
        return new PracticeProgressVO(sessionId, session.getStatus(), session.getTotalCount(), answeredCount, null);
    }

    @Transactional
    public PracticeSpellingCheckVO checkSpelling(Long sessionId, PracticeSpellingCheckRequest request) {
        PracticeSessionEntity session = requireOwnedSessionForUpdate(sessionId);
        requireInProgress(session);
        PracticeSessionAnswerEntity answer = findAnswerForUpdate(sessionId, request.questionOrder());
        if (!"SPELLING".equalsIgnoreCase(answer.getQuestionType())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Spelling question was not found", 404);
        }
        List<String> expected = assessmentJsonCodec.readStringList(answer.getCorrectAnswerJson());
        String candidate = request.candidate() == null ? "" : request.candidate().trim();
        boolean correct = !expected.isEmpty()
                && PracticeSectionCatalog.isCorrect("SPELLING", List.of(candidate), expected);
        LocalDateTime now = LocalDateTime.now();
        int wrongAttempts = answer.getWrongAttemptCount() == null ? 0 : answer.getWrongAttemptCount();
        if (!correct) {
            if (wrongAttempts >= PracticeSectionCatalog.MAX_SPELLING_WRONG_ATTEMPTS) {
                throw new BusinessException(ResultCode.RATE_LIMITED,
                        "Spelling attempt limit reached for this question", 429);
            }
            wrongAttempts++;
            answer.setWrongAttemptCount(wrongAttempts);
            answer.setSpellingHintShown(true);
            practiceSessionAnswerMapper.updateById(answer);
            return new PracticeSpellingCheckVO(false, true, firstLetterOf(expected), wrongAttempts);
        }
        practiceSessionAnswerMapper.updateById(answer);
        return new PracticeSpellingCheckVO(true, Boolean.TRUE.equals(answer.getSpellingHintShown()),
                Boolean.TRUE.equals(answer.getSpellingHintShown()) ? firstLetterOf(expected) : null, wrongAttempts);
    }

    /**
     * Grades the submitted answers once, whole-paper style, and completes the
     * session. Only the submitted questions count as answered.
     */
    @Transactional
    public PracticeProgressVO complete(Long sessionId, SubmitPracticeRequest request) {        PracticeSessionEntity session = requireOwnedSessionForUpdate(sessionId);
        requireInProgress(session);
        LocalDateTime now = LocalDateTime.now();
        Map<Integer, PracticeSessionAnswerEntity> answersByOrder = new LinkedHashMap<>();
        for (PracticeSessionAnswerEntity answer : loadAnswers(sessionId)) {
            answersByOrder.put(answer.getQuestionOrder(), answer);
        }
        int correctCount = 0;
        for (SubmitPracticeRequest.AnswerItem item : request.answers()) {
            PracticeSessionAnswerEntity answer = answersByOrder.get(item.questionOrder());
            if (answer == null) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR,
                        "Unknown question order " + item.questionOrder(), 400);
            }
            List<String> response = item.response().stream()
                    .map(value -> value == null ? "" : value.trim())
                    .filter(value -> !value.isBlank())
                    .toList();
            boolean answered = !response.isEmpty();
            boolean correct = answered && PracticeSectionCatalog.isCorrect(
                    answer.getQuestionType(), response,
                    assessmentJsonCodec.readStringList(answer.getCorrectAnswerJson()));
            answer.setResponseJson(answered ? assessmentJsonCodec.write(response) : null);
            answer.setIsCorrect(answered ? correct : null);
            answer.setAnsweredAt(answered ? now : null);
            practiceSessionAnswerMapper.updateById(answer);
            if (correct) {
                correctCount++;
            }
        }
        int answeredCount = request.answers().size();
        practiceSessionMapper.complete(sessionId, answeredCount, correctCount, now);
        return new PracticeProgressVO(sessionId, STATUS_COMPLETED, session.getTotalCount(), answeredCount, correctCount);
    }

    /**
     * Abandons an in-progress session so the student can start a fresh one.
     * Unsubmitted answers are discarded and the session stays out of the
     * active slot.
     */
    @Transactional
    public PracticeProgressVO abandon(Long sessionId) {
        PracticeSessionEntity session = requireOwnedSessionForUpdate(sessionId);
        requireInProgress(session);
        practiceSessionMapper.update(null, Wrappers.<PracticeSessionEntity>lambdaUpdate()
                .eq(PracticeSessionEntity::getId, sessionId)
                .eq(PracticeSessionEntity::getStatus, STATUS_IN_PROGRESS)
                .set(PracticeSessionEntity::getStatus, "ABANDONED")
                .set(PracticeSessionEntity::getCompletedAt, LocalDateTime.now()));
        return new PracticeProgressVO(sessionId, "ABANDONED", session.getTotalCount(),
                session.getAnsweredCount(), session.getCorrectCount());
    }

    @Transactional(readOnly = true)
    public PracticeResultVO getResult(Long sessionId) {
        PracticeSessionEntity session = requireOwnedSession(sessionId);
        List<PracticeSessionAnswerEntity> answers = loadAnswers(sessionId);
        List<PracticeResultQuestionVO> questions = answers.stream()
                .map(this::toResultQuestionVO)
                .toList();
        List<PracticeSectionMetricVO> sectionMetrics = buildSectionMetrics(answers);
        int correctCount = (int) answers.stream()
                .filter(answer -> Boolean.TRUE.equals(answer.getIsCorrect()))
                .count();
        int answeredCount = (int) answers.stream()
                .filter(answer -> answer.getAnsweredAt() != null || Boolean.TRUE.equals(answer.getIsCorrect()))
                .count();
        double percentage = answeredCount == 0 ? 0d : correctCount / (double) answeredCount * 100d;
        return new PracticeResultVO(
                session.getId(),
                session.getBankCode(),
                session.getSectionCode(),
                session.getStatus(),
                session.getTotalCount(),
                answeredCount,
                correctCount,
                percentage,
                session.getStartedAt(),
                session.getCompletedAt(),
                session.getTutoringStatus(),
                session.getTutoringJson(),
                sectionMetrics,
                questions
        );
    }

    /**
     * Persists the tutoring report snapshot on the practice session so result
     * pages can render it without re-triggering an AI job.
     */
    public void saveTutoringSnapshot(Long sessionId, String status, String tutoringJson) {
        practiceSessionMapper.update(null, Wrappers.<PracticeSessionEntity>lambdaUpdate()
                .eq(PracticeSessionEntity::getId, sessionId)
                .set(PracticeSessionEntity::getTutoringStatus, status)
                .set(PracticeSessionEntity::getTutoringJson, tutoringJson));
    }

    /**
     * Aggregates which words were answered wrong across the student's recent
     * completed practice sessions (excluding the current one), so tutoring can
     * flag repeated errors.
     */
    @Transactional(readOnly = true)
    public List<WrongWordStat> listRecentWrongWordStats(Long studentUserId, Long excludeSessionId, int recentSessionLimit) {
        if (recentSessionLimit <= 0) {
            return List.of();
        }
        return jdbcTemplate.query("""
                        SELECT a.target_word,
                               COUNT(*) AS wrong_times,
                               COUNT(DISTINCT s.id) AS sessions_with_error,
                               MIN(s.started_at) AS first_seen_at,
                               MAX(s.completed_at) AS last_seen_at
                        FROM practice_session_answer a
                        JOIN practice_session s ON s.id = a.session_id
                        WHERE s.owner_user_id = ?
                          AND s.status = 'COMPLETED'
                          AND s.deleted = FALSE
                          AND a.deleted = FALSE
                          AND a.is_correct = FALSE
                          AND a.target_word IS NOT NULL
                          AND a.target_word <> ''
                          AND s.id <> ?
                          AND s.id IN (
                              SELECT recent.id FROM (
                                  SELECT inner_s.id
                                  FROM practice_session inner_s
                                  WHERE inner_s.owner_user_id = ?
                                    AND inner_s.status = 'COMPLETED'
                                    AND inner_s.deleted = FALSE
                                  ORDER BY inner_s.completed_at DESC, inner_s.id DESC
                                  LIMIT ?
                              ) recent
                          )
                        GROUP BY a.target_word
                        ORDER BY wrong_times DESC, sessions_with_error DESC
                        """,
                (resultSet, rowNumber) -> new WrongWordStat(
                        resultSet.getString(1),
                        resultSet.getInt(2),
                        resultSet.getInt(3),
                        resultSet.getTimestamp(4) == null ? null : resultSet.getTimestamp(4).toLocalDateTime(),
                        resultSet.getTimestamp(5) == null ? null : resultSet.getTimestamp(5).toLocalDateTime()
                ),
                studentUserId, excludeSessionId, studentUserId, recentSessionLimit);
    }

    /**
     * Spelling error pattern for a wrong spelling answer: what kind of
     * mismatch the student produced (accent/orthography, replaced, missing or
     * extra letter, or a distant guess).
     */
    public String analyzeSpellingPattern(String candidate, List<String> expected) {
        return PracticeSpellingAnalyzer.analyze(candidate, expected);
    }

    public record WrongWordStat(
            String targetWord,
            int wrongTimes,
            int sessionsWithError,
            LocalDateTime firstSeenAt,
            LocalDateTime lastSeenAt
    ) {
    }

    @Transactional(readOnly = true)
    public PageResult<PracticeHistoryVO> pageHistory(PracticeSessionPageQuery query) {
        Long ownerUserId = currentUserId();
        var wrapper = Wrappers.<PracticeSessionEntity>lambdaQuery()
                .eq(PracticeSessionEntity::getOwnerUserId, ownerUserId)
                .orderByDesc(PracticeSessionEntity::getStartedAt)
                .orderByDesc(PracticeSessionEntity::getId);
        long total = practiceSessionMapper.selectCount(wrapper);
        List<PracticeSessionEntity> sessions = practiceSessionMapper.selectList(wrapper
                .last("LIMIT " + query.pageSize() + " OFFSET " + ((query.pageNo() - 1) * query.pageSize())));
        List<PracticeHistoryVO> records = sessions.stream()
                .map(session -> new PracticeHistoryVO(
                        session.getId(),
                        session.getBankCode(),
                        session.getSectionCode(),
                        session.getStatus(),
                        session.getTotalCount(),
                        session.getAnsweredCount(),
                        session.getCorrectCount(),
                        session.getTotalCount() == null || session.getTotalCount() == 0 ? 0d
                                : (session.getCorrectCount() == null ? 0 : session.getCorrectCount())
                                / (double) session.getTotalCount() * 100d,
                        session.getStartedAt(),
                        session.getCompletedAt()
                ))
                .toList();
        return new PageResult<>(total, query.pageNo(), query.pageSize(), records);
    }

    /**
     * Wrong-answer snapshot used by the AI tutoring scene: each entry carries
     * the target word, construct, the student's answer, and the bank
     * explanation, so tutoring can be personalized to this session.
     */
    @Transactional(readOnly = true)
    public List<WrongAnswerEntry> listWrongAnswers(Long sessionId) {
        PracticeSessionEntity session = requireOwnedSession(sessionId);
        return loadAnswers(sessionId).stream()
                .filter(answer -> Boolean.FALSE.equals(answer.getIsCorrect()))
                .map(answer -> {
                    List<String> response = assessmentJsonCodec.readStringList(answer.getResponseJson());
                    List<String> expected = assessmentJsonCodec.readStringList(answer.getCorrectAnswerJson());
                    String spellingPattern = "SPELLING".equalsIgnoreCase(answer.getQuestionType())
                            ? PracticeSpellingAnalyzer.analyze(response.isEmpty() ? null : response.get(0), expected)
                            : null;
                    return new WrongAnswerEntry(
                            session.getId(),
                            answer.getQuestionOrder(),
                            answer.getQuestionCode(),
                            answer.getQuestionType(),
                            answer.getSectionCode(),
                            answer.getConstructCode(),
                            answer.getTargetWord(),
                            answer.getStemTextSnapshot(),
                            response,
                            expected,
                            answer.getExplanationTextSnapshot(),
                            Boolean.TRUE.equals(answer.getSpellingHintShown()),
                            spellingPattern
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public PracticeQuestionSnapshot getQuestionSnapshot(Long sessionId, Integer questionOrder) {
        requireOwnedSession(sessionId);
        PracticeSessionAnswerEntity answer = practiceSessionAnswerMapper.selectOne(
                Wrappers.<PracticeSessionAnswerEntity>lambdaQuery()
                        .eq(PracticeSessionAnswerEntity::getSessionId, sessionId)
                        .eq(PracticeSessionAnswerEntity::getQuestionOrder, questionOrder)
                        .last("LIMIT 1"));
        if (answer == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Practice question was not found", 404);
        }
        return new PracticeQuestionSnapshot(
                sessionId,
                answer.getQuestionOrder(),
                answer.getQuestionCode(),
                answer.getQuestionType(),
                answer.getSectionCode(),
                answer.getConstructCode(),
                answer.getTransferCategory(),
                answer.getTargetWord(),
                answer.getStemTextSnapshot(),
                answer.getPromptTextSnapshot(),
                answer.getOptionsJsonSnapshot(),
                answer.getCorrectAnswerJson(),
                answer.getExplanationTextSnapshot(),
                answer.getOptionExplanationsJson(),
                assessmentJsonCodec.readStringList(answer.getResponseJson()),
                Boolean.TRUE.equals(answer.getIsCorrect()),
                Boolean.TRUE.equals(answer.getSpellingHintShown()),
                "SPELLING".equalsIgnoreCase(answer.getQuestionType())
                        ? PracticeSpellingAnalyzer.analyze(
                                assessmentJsonCodec.readStringList(answer.getResponseJson()).stream().findFirst().orElse(null),
                                assessmentJsonCodec.readStringList(answer.getCorrectAnswerJson()))
                        : null
        );
    }

    public record WrongAnswerEntry(
            Long practiceSessionId,
            Integer questionOrder,
            String questionCode,
            String questionType,
            String sectionCode,
            String constructCode,
            String targetWord,
            String stemText,
            List<String> response,
            List<String> correctAnswer,
            String explanationText,
            boolean hintShown,
            String spellingErrorPattern
    ) {
    }

    public record PracticeQuestionSnapshot(
            Long practiceSessionId,
            Integer questionOrder,
            String questionCode,
            String questionType,
            String sectionCode,
            String constructCode,
            String transferCategory,
            String targetWord,
            String stemText,
            String promptText,
            String optionsJson,
            String correctAnswerJson,
            String explanationText,
            String optionExplanationsJson,
            List<String> response,
            Boolean correct,
            boolean hintShown,
            String spellingErrorPattern
    ) {
    }

    private List<PracticeSectionMetricVO> buildSectionMetrics(List<PracticeSessionAnswerEntity> answers) {
        Map<String, int[]> stats = new LinkedHashMap<>();
        for (PracticeSectionCatalog.SectionMeta section : PracticeSectionCatalog.SECTIONS) {
            stats.put(section.code(), new int[]{0, 0});
        }
        for (PracticeSessionAnswerEntity answer : answers) {
            if (answer.getSectionCode() == null) {
                continue;
            }
            int[] entry = stats.get(answer.getSectionCode());
            if (entry == null) {
                continue;
            }
            entry[0]++;
            if (Boolean.TRUE.equals(answer.getIsCorrect())) {
                entry[1]++;
            }
        }
        List<PracticeSectionMetricVO> metrics = new ArrayList<>();
        for (PracticeSectionCatalog.SectionMeta section : PracticeSectionCatalog.SECTIONS) {
            int[] entry = stats.get(section.code());
            double percentage = entry[0] == 0 ? 0d : entry[1] / (double) entry[0] * 100d;
            metrics.add(new PracticeSectionMetricVO(
                    section.code(), section.title(), entry[0], entry[1], percentage));
        }
        return metrics;
    }

    private PracticeQuestionVO toQuestionVO(PracticeSessionAnswerEntity answer) {
        List<String> expected = assessmentJsonCodec.readStringList(answer.getCorrectAnswerJson());
        return new PracticeQuestionVO(
                answer.getQuestionOrder(),
                answer.getQuestionCode(),
                answer.getQuestionType(),
                answer.getStemTextSnapshot(),
                answer.getPromptTextSnapshot(),
                readOptions(answer.getOptionsJsonSnapshot()),
                answer.getSectionCode(),
                answer.getConstructCode(),
                answer.getTransferCategory(),
                answer.getTargetWord(),
                assessmentJsonCodec.readStringList(answer.getResponseJson()),
                answer.getSpellingHintShown(),
                Boolean.TRUE.equals(answer.getSpellingHintShown()) ? firstLetterOf(expected) : null,
                answer.getWrongAttemptCount(),
                answer.getAnsweredAt() != null
        );
    }

    private PracticeResultQuestionVO toResultQuestionVO(PracticeSessionAnswerEntity answer) {
        return new PracticeResultQuestionVO(
                answer.getQuestionOrder(),
                answer.getQuestionCode(),
                answer.getQuestionType(),
                answer.getSectionCode(),
                answer.getConstructCode(),
                answer.getTransferCategory(),
                answer.getTargetWord(),
                answer.getStemTextSnapshot(),
                answer.getPromptTextSnapshot(),
                readOptions(answer.getOptionsJsonSnapshot()),
                assessmentJsonCodec.readStringList(answer.getCorrectAnswerJson()),
                assessmentJsonCodec.readStringList(answer.getResponseJson()),
                answer.getIsCorrect(),
                answer.getExplanationTextSnapshot(),
                readOptionExplanations(answer.getOptionExplanationsJson()),
                answer.getSpellingHintShown(),
                answer.getWrongAttemptCount(),
                "SPELLING".equalsIgnoreCase(answer.getQuestionType())
                        ? PracticeSpellingAnalyzer.analyze(
                                assessmentJsonCodec.readStringList(answer.getResponseJson()).stream().findFirst().orElse(null),
                                assessmentJsonCodec.readStringList(answer.getCorrectAnswerJson()))
                        : null
        );
    }

    private List<AssessmentOptionVO> readOptions(String optionsJson) {
        return assessmentJsonCodec.readOptions(optionsJson).stream()
                .map(option -> new AssessmentOptionVO(option.key(), option.label()))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> readOptionExplanations(String optionExplanationsJson) {
        if (optionExplanationsJson == null || optionExplanationsJson.isBlank()) {
            return Map.of();
        }
        Object value = assessmentJsonCodec.read(optionExplanationsJson, Object.class);
        if (!(value instanceof Map<?, ?> raw)) {
            return Map.of();
        }
        Map<String, String> explanations = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            explanations.put(String.valueOf(entry.getKey()),
                    entry.getValue() == null ? null : String.valueOf(entry.getValue()));
        }
        return explanations;
    }

    private int countAnswered(Long sessionId) {
        Long count = practiceSessionAnswerMapper.selectCount(Wrappers.<PracticeSessionAnswerEntity>lambdaQuery()
                .eq(PracticeSessionAnswerEntity::getSessionId, sessionId)
                .isNotNull(PracticeSessionAnswerEntity::getResponseJson)
                .apply("response_json <> ''"));
        return count == null ? 0 : count.intValue();
    }

    private List<PracticeSessionAnswerEntity> loadAnswers(Long sessionId) {
        return practiceSessionAnswerMapper.selectBySessionId(sessionId);
    }

    private PracticeSessionAnswerEntity findAnswerForUpdate(Long sessionId, Integer questionOrder) {
        PracticeSessionAnswerEntity answer = practiceSessionAnswerMapper.selectBySessionAndOrderForUpdate(
                sessionId, questionOrder);
        if (answer == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Practice question was not found", 404);
        }
        return answer;
    }

    private boolean hasActiveSession(Long ownerUserId) {
        Long count = practiceSessionMapper.selectCount(Wrappers.<PracticeSessionEntity>lambdaQuery()
                .eq(PracticeSessionEntity::getOwnerUserId, ownerUserId)
                .eq(PracticeSessionEntity::getStatus, STATUS_IN_PROGRESS));
        return count != null && count > 0;
    }

    private PracticeSessionEntity requireOwnedSession(Long sessionId) {
        Long ownerUserId = currentUserId();
        PracticeSessionEntity session = practiceSessionMapper.selectOne(
                Wrappers.<PracticeSessionEntity>lambdaQuery()
                        .eq(PracticeSessionEntity::getId, sessionId)
                        .last("LIMIT 1"));
        if (session == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Practice session was not found", 404);
        }
        if (!Objects.equals(session.getOwnerUserId(), ownerUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "Practice session access denied", 403);
        }
        return session;
    }

    private PracticeSessionEntity requireOwnedSessionForUpdate(Long sessionId) {
        Long ownerUserId = currentUserId();
        PracticeSessionEntity session = practiceSessionMapper.selectByIdForUpdate(sessionId);
        if (session == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Practice session was not found", 404);
        }
        if (!Objects.equals(session.getOwnerUserId(), ownerUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "Practice session access denied", 403);
        }
        return session;
    }

    private void requireInProgress(PracticeSessionEntity session) {
        if (!STATUS_IN_PROGRESS.equals(session.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "Practice session is not in progress", 409);
        }
    }

    private String normalizeSection(String sectionCode) {
        if (sectionCode == null || sectionCode.isBlank()) {
            return null;
        }
        if (!PracticeSectionCatalog.isPracticeSection(sectionCode)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR,
                    "Unknown practice section: " + sectionCode, 400);
        }
        return sectionCode;
    }

    private String firstLetterOf(List<String> expected) {
        if (expected == null || expected.isEmpty()) {
            return null;
        }
        String value = expected.get(0);
        return value == null || value.isBlank() ? null : value.substring(0, 1);
    }

    private Long currentUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "Authentication required", 401));
    }
}
