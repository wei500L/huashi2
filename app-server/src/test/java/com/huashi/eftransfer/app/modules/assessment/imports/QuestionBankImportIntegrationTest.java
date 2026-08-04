package com.huashi.eftransfer.app.modules.assessment.imports;

import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QuestionBankImportIntegrationTest extends AbstractWebIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void preflightsAndTransactionallyCommitsJsonPackage() throws Exception {
        String token = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");

        MvcResult preflight = mockMvc.perform(post("/api/teacher/assessments/question-bank/imports/preflight")
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("Stem", "Explanation")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.scoredItemCount").value(1))
                .andReturn();
        long importId = readJson(preflight).path("data").path("importId").asLong();

        mockMvc.perform(post("/api/teacher/assessments/question-bank/imports/{importId}/commit", importId)
                        .with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMMITTED"))
                .andExpect(jsonPath("$.data.paperId").isNumber());

        assertThat(count("assessment_question_bank")).isEqualTo(1);
        assertThat(count("assessment_question_version")).isEqualTo(1);
        assertThat(count("assessment_questionnaire")).isEqualTo(1);
        assertThat(count("assessment_questionnaire_version")).isEqualTo(1);
        assertThat(count("assessment_questionnaire_section")).isEqualTo(1);
        assertThat(count("assessment_questionnaire_item")).isEqualTo(1);
        assertThat(count("assessment_paper")).isEqualTo(1);
        assertThat(count("assessment_question")).isEqualTo(1);
    }

    @Test
    void blocksCommitUntilEveryReviewIssueIsConfirmed() throws Exception {
        String token = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");
        long firstImportId = preflight(token, validPayload("Stem", "Explanation")).path("importId").asLong();
        mockMvc.perform(post("/api/teacher/assessments/question-bank/imports/{importId}/commit", firstImportId)
                        .with(bearer(token)))
                .andExpect(status().isOk());

        JsonNode review = preflight(token, validPayload("Changed stem", "Changed explanation"));
        long reviewImportId = review.path("importId").asLong();
        assertThat(review.path("status").asText()).isEqualTo("REVIEW_REQUIRED");

        mockMvc.perform(get("/api/teacher/assessments/question-bank/imports/{importId}/publish-readiness", reviewImportId)
                        .with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publishable").value(false))
                .andExpect(jsonPath("$.data.openReviewCount").value(2));

        mockMvc.perform(post("/api/teacher/assessments/question-bank/imports/{importId}/commit", reviewImportId)
                        .with(bearer(token)))
                .andExpect(status().isConflict());

        String issueIds = objectMapper.writeValueAsString(review.path("issues").valueStream()
                .map(node -> node.path("issueId").asLong()).toList());
        mockMvc.perform(post("/api/teacher/assessments/question-bank/imports/{importId}/confirm", reviewImportId)
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"issueIds\":" + issueIds + ",\"resolutionNote\":\"source checked\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY"));

        mockMvc.perform(post("/api/teacher/assessments/question-bank/imports/{importId}/commit", reviewImportId)
                        .with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMMITTED"));
    }

    private JsonNode preflight(String token, String payload) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/teacher/assessments/question-bank/imports/preflight")
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data");
    }

    private long count(String table) {
        Long result = jdbcTemplate.queryForObject("select count(*) from " + table, Long.class);
        return result == null ? 0 : result;
    }

    private String validPayload(String stem, String explanation) {
        return """
                {
                  "Questionnaire": {
                    "code": "LEXIBRIDGE_RESEARCH_V1",
                    "title": "Lexi-Bridge",
                    "description": "Research questionnaire",
                    "durationMinutes": 40,
                    "scoringVersion": "SCORING_V1",
                    "aiPromptVersion": "assessment-analysis/v1"
                  },
                  "Sections": [{
                    "sectionCode": "S1",
                    "title": "Vocabulary",
                    "sortOrder": 1,
                    "formalSection": true
                  }],
                  "Items": [{
                    "itemCode": "Q1",
                    "sectionCode": "S1",
                    "questionType": "SINGLE_CHOICE",
                    "stemText": "%s",
                    "correctAnswers": ["A"],
                    "explanationText": "%s",
                    "requiredAnswer": true,
                    "scored": true,
                    "weight": 1,
                    "transferCategory": "COGNATE",
                    "contextLevel": "WORD",
                    "constructCode": "LEXICAL_TRANSFER"
                  }],
                  "Options": [
                    {"itemCode":"Q1","optionCode":"A","optionText":"Alpha","correct":true},
                    {"itemCode":"Q1","optionCode":"B","optionText":"Beta","correct":false}
                  ]
                }
                """.formatted(stem, explanation);
    }
}
