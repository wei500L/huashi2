package com.huashi.eftransfer.app.modules.practice;

import com.huashi.eftransfer.app.modules.practice.service.PracticeSessionService;
import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the student self-practice flow: bank listing, session start with
 * question snapshotting, draft saving, spelling hint checks, whole-paper
 * grading, result metrics and the single-active-session constraint.
 */
@TestPropertySource(properties = "app.assessment.seed.lexibridge-v3-enabled=true")
class PracticeSessionFlowIntegrationTest extends AbstractWebIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PracticeSessionService practiceSessionService;


    @Test
    void bankListingExposesOnlyTheFourPracticeSections() throws Exception {
        String token = loginAndGetAccessToken("student.li", "Student@123456");
        JsonNode banks = readJson(mockMvc.perform(get("/api/student/practice/banks").with(bearer(token)))
                .andExpect(status().isOk())
                .andReturn()).path("data");
        assertThat(banks.size()).isEqualTo(1);
        JsonNode bank = banks.get(0);
        assertThat(bank.path("bankCode").asText()).isEqualTo("LEXIBRIDGE_FF4_V2");
        assertThat(bank.path("sections").size()).isEqualTo(4);
        assertThat(bank.path("totalQuestionCount").asInt()).isEqualTo(239);
    }

    @Test
    void wholePaperSessionSnapshotsQuestionsGradesOnCompleteAndBlocksConcurrentSession() throws Exception {
        String token = loginAndGetAccessToken("student.li", "Student@123456");

        JsonNode created = readJson(mockMvc.perform(post("/api/student/practice/sessions").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bankCode": "LEXIBRIDGE_FF4_V2",
                                  "sectionCode": "FF4_WORD_MEANING"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").isNumber())
                .andExpect(jsonPath("$.data.totalCount").value(27))
                .andReturn()).path("data");
        long sessionId = created.path("sessionId").asLong();

        JsonNode detail = readJson(mockMvc.perform(get("/api/student/practice/sessions/" + sessionId).with(bearer(token)))
                .andExpect(status().isOk())
                .andReturn()).path("data");
        assertThat(detail.path("questions").size()).isEqualTo(27);
        assertThat(detail.path("questions").get(0).path("correctAnswer").isMissingNode()).isTrue();
        assertThat(detail.path("questions").get(0).path("explanation").isMissingNode()).isTrue();

        mockMvc.perform(post("/api/student/practice/sessions/" + sessionId + "/draft").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answers": [
                                    {"questionOrder": 1, "response": ["A"]}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answeredCount").value(1));

        List<String> correctKey = jdbcTemplate.queryForList(
                "SELECT correct_answer_json FROM practice_session_answer WHERE session_id = ? AND question_order = 1",
                String.class, sessionId);
        String correctAnswer = correctKey.getFirst().replaceAll("[\\[\\]\"]", "");

        mockMvc.perform(post("/api/student/practice/sessions/" + sessionId + "/complete").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answers": [
                                    {"questionOrder": 1, "response": ["%s"]},
                                    {"questionOrder": 2, "response": ["wrong-key"]}
                                  ]
                                }
                                """.formatted(correctAnswer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.correctCount").value(1));

        JsonNode result = readJson(mockMvc.perform(get("/api/student/practice/sessions/" + sessionId + "/result").with(bearer(token)))
                .andExpect(status().isOk())
                .andReturn()).path("data");
        assertThat(result.path("answeredCount").asInt()).isEqualTo(2);
        assertThat(result.path("correctCount").asInt()).isEqualTo(1);
        assertThat(result.path("percentage").asDouble()).isEqualTo(50d);
        assertThat(result.path("sectionMetrics").size()).isEqualTo(4);
        assertThat(result.path("questions").get(0).path("correct").asBoolean()).isTrue();
        assertThat(result.path("questions").get(0).path("correctAnswer").isArray()).isTrue();

        mockMvc.perform(post("/api/student/practice/sessions").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bankCode": "LEXIBRIDGE_FF4_V2",
                                  "sectionCode": "FF4_WORD_MEANING"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/student/practice/sessions").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bankCode": "LEXIBRIDGE_FF4_V2"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void concurrentCreateMapsUniqueKeyCollisionToConflictInsteadOfServerError() throws Exception {
        String token = loginAndGetAccessToken("student.li", "Student@123456");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            java.util.concurrent.Callable<Integer> task = () -> {
                ready.countDown();
                start.await(10, TimeUnit.SECONDS);
                return mockMvc.perform(post("/api/student/practice/sessions").with(bearer(token))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "bankCode": "LEXIBRIDGE_FF4_V2",
                                          "sectionCode": "FF4_WORD_MEANING"
                                        }
                                        """))
                        .andReturn()
                        .getResponse()
                        .getStatus();
            };
            Future<Integer> first = pool.submit(task);
            Future<Integer> second = pool.submit(task);
            ready.await(10, TimeUnit.SECONDS);
            start.countDown();
            List<Integer> statuses = List.of(
                    first.get(30, TimeUnit.SECONDS),
                    second.get(30, TimeUnit.SECONDS)
            );
            assertThat(statuses).doesNotContain(500);
            assertThat(statuses).contains(409);
            assertThat(statuses.stream().filter(status -> status == 200).count()).isLessThanOrEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void completeCountsOnlyNonBlankAnswersAndMatchesResultPage() throws Exception {
        String token = loginAndGetAccessToken("student.li", "Student@123456");
        JsonNode created = readJson(mockMvc.perform(post("/api/student/practice/sessions").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bankCode": "LEXIBRIDGE_FF4_V2",
                                  "sectionCode": "FF4_WORD_MEANING"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()).path("data");
        long sessionId = created.path("sessionId").asLong();

        String correctAnswer = jdbcTemplate.queryForList(
                        "SELECT correct_answer_json FROM practice_session_answer WHERE session_id = ? AND question_order = 1",
                        String.class, sessionId)
                .getFirst()
                .replaceAll("[\\[\\]\"]", "");

        mockMvc.perform(post("/api/student/practice/sessions/" + sessionId + "/complete").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answers": [
                                    {"questionOrder": 1, "response": ["%s"]},
                                    {"questionOrder": 2, "response": ["  "]},
                                    {"questionOrder": 3, "response": []}
                                  ]
                                }
                                """.formatted(correctAnswer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.answeredCount").value(1))
                .andExpect(jsonPath("$.data.correctCount").value(1));

        Integer storedAnsweredCount = jdbcTemplate.queryForObject(
                "SELECT answered_count FROM practice_session WHERE id = ?", Integer.class, sessionId);
        assertThat(storedAnsweredCount).isEqualTo(1);

        JsonNode result = readJson(mockMvc.perform(get("/api/student/practice/sessions/" + sessionId + "/result")
                        .with(bearer(token)))
                .andExpect(status().isOk())
                .andReturn()).path("data");
        assertThat(result.path("answeredCount").asInt()).isEqualTo(1);
        assertThat(result.path("correctCount").asInt()).isEqualTo(1);
    }

    @Test
    void spellingCheckRevealsFirstLetterAfterWrongAttempt() throws Exception {
        String token = loginAndGetAccessToken("student.li", "Student@123456");
        JsonNode created = readJson(mockMvc.perform(post("/api/student/practice/sessions").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bankCode": "LEXIBRIDGE_FF4_V2",
                                  "sectionCode": "FF4_SPELLING"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()).path("data");
        long sessionId = created.path("sessionId").asLong();

        JsonNode wrong = readJson(mockMvc.perform(post(
                        "/api/student/practice/sessions/" + sessionId + "/answers/spelling-check").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionOrder": 1,
                                  "candidate": "not-the-word"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correct").value(false))
                .andExpect(jsonPath("$.data.hintShown").value(true))
                .andReturn()).path("data");
        assertThat(wrong.path("hintFirstLetter").asText().length()).isEqualTo(1);

        String correctAnswer = jdbcTemplate.queryForObject(
                "SELECT correct_answer_json FROM practice_session_answer WHERE session_id = ? AND question_order = 1",
                String.class, sessionId);
        String expected = correctAnswer.replaceAll("[\\[\\]\"]", "");

        mockMvc.perform(post("/api/student/practice/sessions/" + sessionId + "/answers/spelling-check").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questionOrder": 1,
                                  "candidate": "%s"
                                }
                                """.formatted(expected)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correct").value(true))
                .andExpect(jsonPath("$.data.hintShown").value(true));
    }

    @Test
    void abandonReleasesTheActiveSessionSlot() throws Exception {
        String token = loginAndGetAccessToken("student.li", "Student@123456");
        JsonNode created = readJson(mockMvc.perform(post("/api/student/practice/sessions").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bankCode": "LEXIBRIDGE_FF4_V2",
                                  "sectionCode": "FF4_TRUE_FALSE"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()).path("data");
        long sessionId = created.path("sessionId").asLong();

        mockMvc.perform(post("/api/student/practice/sessions").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bankCode": "LEXIBRIDGE_FF4_V2"
                                }
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/student/practice/sessions/" + sessionId + "/abandon").with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ABANDONED"));

        mockMvc.perform(post("/api/student/practice/sessions").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bankCode": "LEXIBRIDGE_FF4_V2",
                                  "sectionCode": "FF4_TRUE_FALSE"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void targetWordsSessionFiltersTheBankByWord() throws Exception {
        String token = loginAndGetAccessToken("student.li", "Student@123456");
        JsonNode created = readJson(mockMvc.perform(post("/api/student/practice/sessions").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bankCode": "LEXIBRIDGE_FF4_V2",
                                  "sectionCode": "FF4_WORD_MEANING",
                                  "targetWords": ["hardi", "rater"]
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()).path("data");
        assertThat(created.path("totalCount").asInt()).isEqualTo(2);
        JsonNode detail = readJson(mockMvc.perform(get("/api/student/practice/sessions/" + created.path("sessionId").asLong())
                        .with(bearer(token)))
                .andExpect(status().isOk())
                .andReturn()).path("data");
        assertThat(detail.path("questions").get(0).path("targetWord").asText()).isIn("hardi", "rater");
        assertThat(detail.path("questions").get(1).path("targetWord").asText()).isIn("hardi", "rater");

        mockMvc.perform(post("/api/student/practice/sessions/" + created.path("sessionId").asLong() + "/abandon")
                        .with(bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/student/practice/sessions").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bankCode": "LEXIBRIDGE_FF4_V2",
                                  "sectionCode": "FF4_WORD_MEANING",
                                  "targetWords": ["no-such-word-in-bank"]
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tutoringSnapshotIsPersistedAndReturnedWithTheResult() throws Exception {
        String token = loginAndGetAccessToken("student.li", "Student@123456");
        JsonNode created = readJson(mockMvc.perform(post("/api/student/practice/sessions").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bankCode": "LEXIBRIDGE_FF4_V2",
                                  "sectionCode": "FF4_TRUE_FALSE"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()).path("data");
        long sessionId = created.path("sessionId").asLong();
        String correctAnswer = jdbcTemplate.queryForObject(
                "SELECT correct_answer_json FROM practice_session_answer WHERE session_id = ? AND question_order = 1",
                String.class, sessionId);
        mockMvc.perform(post("/api/student/practice/sessions/" + sessionId + "/complete").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answers": [{"questionOrder": 1, "response": %s}]
                                }
                                """.formatted(correctAnswer)))
                .andExpect(status().isOk());

        practiceSessionService.saveTutoringSnapshot(sessionId, "AI", "{\"requestId\":\"r1\",\"explanation\":\"快照测试\"}");

        JsonNode result = readJson(mockMvc.perform(get("/api/student/practice/sessions/" + sessionId + "/result").with(bearer(token)))
                .andExpect(status().isOk())
                .andReturn()).path("data");
        assertThat(result.path("tutoringStatus").asText()).isEqualTo("AI");
        assertThat(result.path("tutoringJson").asText()).contains("快照测试");
    }

    @Test
    void spellingErrorPatternIsReportedOnWrongSpellingAnswers() throws Exception {
        String token = loginAndGetAccessToken("student.li", "Student@123456");
        JsonNode created = readJson(mockMvc.perform(post("/api/student/practice/sessions").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bankCode": "LEXIBRIDGE_FF4_V2",
                                  "sectionCode": "FF4_SPELLING"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()).path("data");
        long sessionId = created.path("sessionId").asLong();
        String correctAnswer = jdbcTemplate.queryForObject(
                "SELECT correct_answer_json FROM practice_session_answer WHERE session_id = ? AND question_order = 1",
                String.class, sessionId);
        String expected = correctAnswer.replaceAll("[\\[\\]\"]", "");

        mockMvc.perform(post("/api/student/practice/sessions/" + sessionId + "/complete").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answers": [
                                    {"questionOrder": 1, "response": ["%s"]},
                                    {"questionOrder": 2, "response": ["totally-wrong-word"]}
                                  ]
                                }
                                """.formatted(expected)))
                .andExpect(status().isOk());

        JsonNode result = readJson(mockMvc.perform(get("/api/student/practice/sessions/" + sessionId + "/result").with(bearer(token)))
                .andExpect(status().isOk())
                .andReturn()).path("data");
        JsonNode wrongQuestion = result.path("questions").get(1);
        assertThat(wrongQuestion.path("spellingErrorPattern").asText()).isEqualTo("DISTANT");
    }

    @Test
    void historyReturnsCompletedAndInProgressSessions() throws Exception {
        String token = loginAndGetAccessToken("student.li", "Student@123456");
        mockMvc.perform(post("/api/student/practice/sessions").with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bankCode": "LEXIBRIDGE_FF4_V2",
                                  "sectionCode": "FF4_TRUE_FALSE"
                                }
                                """))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/student/practice/history?pageNo=1&pageSize=10").with(bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode history = readJson(result).path("data");
        assertThat(history.path("total").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(history.path("records").get(0).path("status").asText()).isEqualTo("IN_PROGRESS");
    }
}
