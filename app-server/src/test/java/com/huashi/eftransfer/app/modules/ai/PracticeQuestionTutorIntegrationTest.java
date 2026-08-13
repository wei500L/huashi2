package com.huashi.eftransfer.app.modules.ai;

import com.huashi.eftransfer.app.integration.ai.client.AiGatewayFailureReason;
import com.huashi.eftransfer.app.modules.ai.support.AiConstants;
import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "app.assessment.seed.lexibridge-v3-enabled=true")
@Import(AiInsightIntegrationTest.AiIntegrationTestConfiguration.class)
class PracticeQuestionTutorIntegrationTest extends AbstractWebIntegrationTest {

    @Autowired
    private AiInsightIntegrationTest.StubAiGatewayClient stubAiGatewayClient;

    @BeforeEach
    void resetStub() {
        stubAiGatewayClient.reset();
    }

    @Test
    void groundedQuestionTutorIsMarkedAi() throws Exception {
        long sessionId = startWordMeaningSession();

        mockMvc.perform(post("/api/ai/practice-question-tutor")
                        .with(bearer(loginAndGetAccessToken("student.li", "Student@123456")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "practiceSessionId": %d,
                                  "questionOrder": 1
                                }
                                """.formatted(sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generationSource").value(AiConstants.GENERATION_SOURCE_AI))
                .andExpect(jsonPath("$.data.explanation").value(org.hamcrest.Matchers.containsString("[C1]")));
    }

    @Test
    void emptyCitationsFallBackToBankExplanation() throws Exception {
        stubAiGatewayClient.setRagRetrieveMode(
                AiInsightIntegrationTest.StubAiGatewayClient.RagRetrieveMode.EMPTY);
        long sessionId = startWordMeaningSession();
        String token = loginAndGetAccessToken("student.li", "Student@123456");

        MvcResult result = mockMvc.perform(post("/api/ai/practice-question-tutor")
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "practiceSessionId": %d,
                                  "questionOrder": 1
                                }
                                """.formatted(sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generationSource").value(AiConstants.GENERATION_SOURCE_RULE_FALLBACK))
                .andExpect(jsonPath("$.data.fallbackReason").value(
                        AiGatewayFailureReason.GROUNDING_VALIDATION_FAILED.name()))
                .andReturn();

        JsonNode data = readJson(result).path("data");
        assertThat(data.path("generationSource").asText()).isEqualTo(AiConstants.GENERATION_SOURCE_RULE_FALLBACK);
        assertThat(data.path("explanation").asText()).isNotBlank();
    }

    @Test
    void ragFailureFallsBackToBankExplanation() throws Exception {
        stubAiGatewayClient.setRagRetrieveMode(
                AiInsightIntegrationTest.StubAiGatewayClient.RagRetrieveMode.FAILURE);
        long sessionId = startWordMeaningSession();

        mockMvc.perform(post("/api/ai/practice-question-tutor")
                        .with(bearer(loginAndGetAccessToken("student.li", "Student@123456")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "practiceSessionId": %d,
                                  "questionOrder": 1
                                }
                                """.formatted(sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generationSource").value(AiConstants.GENERATION_SOURCE_RULE_FALLBACK))
                .andExpect(jsonPath("$.data.fallbackReason").value(
                        AiGatewayFailureReason.GROUNDING_VALIDATION_FAILED.name()));
    }

    private long startWordMeaningSession() throws Exception {
        String token = loginAndGetAccessToken("student.li", "Student@123456");
        JsonNode created = readJson(mockMvc.perform(post("/api/student/practice/sessions")
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bankCode": "LEXIBRIDGE_FF4_V2",
                                  "sectionCode": "FF4_WORD_MEANING"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()).path("data");
        return created.path("sessionId").asLong();
    }
}
