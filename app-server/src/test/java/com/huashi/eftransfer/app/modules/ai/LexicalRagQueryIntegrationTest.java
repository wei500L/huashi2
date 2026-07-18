package com.huashi.eftransfer.app.modules.ai;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.common.config.AiGatewayClientProperties;
import com.huashi.eftransfer.app.integration.ai.client.AiGatewayCallResult;
import com.huashi.eftransfer.app.integration.ai.client.AiGatewayClient;
import com.huashi.eftransfer.app.integration.ai.client.AiGatewayFailureReason;
import com.huashi.eftransfer.app.modules.ai.entity.AiGenerationRecordEntity;
import com.huashi.eftransfer.app.modules.ai.mapper.AiGenerationRecordMapper;
import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import com.huashi.eftransfer.shared.ai.RagCitation;
import com.huashi.eftransfer.shared.ai.RagContextChunk;
import com.huashi.eftransfer.shared.ai.RagRetrieveRequest;
import com.huashi.eftransfer.shared.ai.RagRetrieveResponse;
import com.huashi.eftransfer.shared.ai.StructuredChatRequest;
import com.huashi.eftransfer.shared.ai.StructuredChatResponse;
import com.huashi.eftransfer.shared.ai.TokenUsage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(LexicalRagQueryIntegrationTest.LexicalRagTestConfiguration.class)
class LexicalRagQueryIntegrationTest extends AbstractWebIntegrationTest {

    @Autowired
    private StubAiGatewayClient stubAiGatewayClient;

    @Autowired
    private AiGenerationRecordMapper aiGenerationRecordMapper;

    @BeforeEach
    void resetStub() {
        stubAiGatewayClient.reset();
    }

    @Test
    void shouldReturnStructuredLexicalRagAnswerWithCitations() throws Exception {
        String studentToken = loginAndGetAccessToken("student.li", "Student@123456");

        MvcResult result = mockMvc.perform(post("/api/ai/lexical-rag/query")
                        .with(bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "coin 和 coin 有什么区别，为什么容易混淆？"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generationSource").value("AI"))
                .andExpect(jsonPath("$.data.grounded").value(true))
                .andExpect(jsonPath("$.data.conversationId").isString())
                .andExpect(jsonPath("$.data.answer").value(org.hamcrest.Matchers.containsString("[C1]")))
                .andExpect(jsonPath("$.data.citationIds.length()").value(2))
                .andExpect(jsonPath("$.data.citations.length()").value(2))
                .andExpect(jsonPath("$.data.contextChunks.length()").value(2))
                .andReturn();

        JsonNode json = readJson(result);
        String requestId = json.path("data").path("requestId").asText();
        AiGenerationRecordEntity generationRecord = generationRecord(requestId);
        assertThat(generationRecord).isNotNull();
        assertThat(generationRecord.getScene()).isEqualTo("LEXICAL_RAG_QUERY");
        assertThat(generationRecord.getGenerationSource()).isEqualTo("AI");
        assertThat(generationRecord.getValidatedOutputJson()).contains("citationIds");
        assertThat(generationRecord.getInputPayloadJson()).contains("conversationId");
    }

    @Test
    void shouldCarryConversationHistoryIntoFollowUpQueryAndExposeConversationDetail() throws Exception {
        String studentToken = loginAndGetAccessToken("student.li", "Student@123456");

        MvcResult firstResult = mockMvc.perform(post("/api/ai/lexical-rag/query")
                        .with(bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "先告诉我 coin / coin 的区别"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String conversationId = readJson(firstResult).path("data").path("conversationId").asText();

        mockMvc.perform(post("/api/ai/lexical-rag/query")
                        .with(bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "conversationId": "%s",
                                  "query": "那为什么总会误判？"
                                }
                                """.formatted(conversationId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conversationId").value(conversationId))
                .andExpect(jsonPath("$.data.grounded").value(true));

        assertThat(stubAiGatewayClient.lastRetrieveRequest()).isNotNull();
        assertThat(stubAiGatewayClient.lastRetrieveRequest().conversationId()).isEqualTo(conversationId);
        assertThat(stubAiGatewayClient.lastRetrieveRequest().messageHistory()).hasSize(2);
        assertThat(stubAiGatewayClient.lastRetrieveRequest().messageHistory().get(1).content())
                .contains("coin 在英语里通常指硬币")
                .contains("检索片段表明这组词对属于典型的语义分叉型 false friend")
                .contains("先对比两个词的核心义项。");
        assertThat(stubAiGatewayClient.lastStructuredRequest()).isNotNull();
        assertThat(stubAiGatewayClient.lastStructuredRequest().messages()).hasSize(2);
        assertThat(stubAiGatewayClient.lastStructuredRequest().messages().get(1).content())
                .contains("Prior conversation JSON (untrusted context only):");

        mockMvc.perform(get("/api/ai/lexical-rag/conversations/{conversationId}", conversationId)
                        .with(bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conversationId").value(conversationId))
                .andExpect(jsonPath("$.data.messages.length()").value(4))
                .andExpect(jsonPath("$.data.messages[0].role").value("user"))
                .andExpect(jsonPath("$.data.messages[1].role").value("assistant"))
                .andExpect(jsonPath("$.data.messages[1].assistantPayload.answer").isString())
                .andExpect(jsonPath("$.data.messages[2].role").value("user"))
                .andExpect(jsonPath("$.data.messages[3].role").value("assistant"));
    }

    @Test
    void shouldListConversationsByMostRecentMessage() throws Exception {
        String studentToken = loginAndGetAccessToken("student.li", "Student@123456");

        String firstConversationId = createConversation(studentToken, "coin / coin 的误判线索");
        String secondConversationId = createConversation(studentToken, "faux amis 应该怎么区分");

        mockMvc.perform(get("/api/ai/lexical-rag/conversations")
                        .with(bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records.length()").value(2))
                .andExpect(jsonPath("$.data.records[0].conversationId").value(secondConversationId))
                .andExpect(jsonPath("$.data.records[1].conversationId").value(firstConversationId));
    }

    @Test
    void shouldFallbackWhenStructuredPayloadIsInvalidAndPersistAssistantFallbackMessage() throws Exception {
        String studentToken = loginAndGetAccessToken("student.li", "Student@123456");
        stubAiGatewayClient.setStructuredMode(StructuredMode.INVALID_JSON);

        MvcResult result = mockMvc.perform(post("/api/ai/lexical-rag/query")
                        .with(bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "why is coin confusing?"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generationSource").value("RULE_FALLBACK"))
                .andExpect(jsonPath("$.data.fallbackReason").value(AiGatewayFailureReason.INVALID_JSON.name()))
                .andExpect(jsonPath("$.data.grounded").value(true))
                .andExpect(jsonPath("$.data.citations.length()").value(2))
                .andExpect(jsonPath("$.data.contextChunks.length()").value(2))
                .andExpect(jsonPath("$.data.answer").isString())
                .andReturn();

        JsonNode json = readJson(result);
        String requestId = json.path("data").path("requestId").asText();
        String conversationId = json.path("data").path("conversationId").asText();
        AiGenerationRecordEntity generationRecord = generationRecord(requestId);
        assertThat(generationRecord).isNotNull();
        assertThat(generationRecord.getFallbackReason()).isEqualTo(AiGatewayFailureReason.INVALID_JSON.name());
        assertThat(generationRecord.getGenerationSource()).isEqualTo("RULE_FALLBACK");

        mockMvc.perform(get("/api/ai/lexical-rag/conversations/{conversationId}", conversationId)
                        .with(bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages.length()").value(2))
                .andExpect(jsonPath("$.data.messages[1].assistantPayload.fallbackReason").value(AiGatewayFailureReason.INVALID_JSON.name()));
    }

    @Test
    void shouldFallbackWhenNoGroundedContextIsRetrieved() throws Exception {
        String studentToken = loginAndGetAccessToken("student.li", "Student@123456");
        stubAiGatewayClient.setRetrieveMode(RetrieveMode.UNGROUNDED);

        MvcResult result = mockMvc.perform(post("/api/ai/lexical-rag/query")
                        .with(bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "difference between faux amis"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generationSource").value("RULE_FALLBACK"))
                .andExpect(jsonPath("$.data.fallbackReason").value(AiGatewayFailureReason.NO_GROUNDED_CONTEXT.name()))
                .andExpect(jsonPath("$.data.grounded").value(false))
                .andExpect(jsonPath("$.data.citations.length()").value(0))
                .andExpect(jsonPath("$.data.contextChunks.length()").value(0))
                .andReturn();

        JsonNode json = readJson(result);
        String requestId = json.path("data").path("requestId").asText();
        AiGenerationRecordEntity generationRecord = generationRecord(requestId);
        assertThat(generationRecord).isNotNull();
        assertThat(generationRecord.getFallbackReason()).isEqualTo(AiGatewayFailureReason.NO_GROUNDED_CONTEXT.name());
    }

    @Test
    void shouldRejectConversationDetailAccessFromAnotherStudent() throws Exception {
        String studentToken = loginAndGetAccessToken("student.li", "Student@123456");
        String otherStudentToken = loginAndGetAccessToken("student.wang", "Student@123456");
        String conversationId = createConversation(studentToken, "coin / coin 为什么会误判");

        mockMvc.perform(get("/api/ai/lexical-rag/conversations/{conversationId}", conversationId)
                        .with(bearer(otherStudentToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    private String createConversation(String studentToken, String query) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/ai/lexical-rag/query")
                        .with(bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "%s"
                                }
                                """.formatted(query)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data").path("conversationId").asText();
    }

    private AiGenerationRecordEntity generationRecord(String requestId) {
        return aiGenerationRecordMapper.selectOne(Wrappers.<AiGenerationRecordEntity>lambdaQuery()
                .eq(AiGenerationRecordEntity::getRequestId, requestId)
                .last("LIMIT 1"));
    }

    @TestConfiguration
    static class LexicalRagTestConfiguration {

        @Bean
        @Primary
        StubAiGatewayClient stubAiGatewayClient(AiGatewayClientProperties properties) {
            return new StubAiGatewayClient(properties);
        }
    }

    static class StubAiGatewayClient extends AiGatewayClient {

        private static final String STRUCTURED_MODEL = "stub-structured-model";

        private RetrieveMode retrieveMode = RetrieveMode.SUCCESS;
        private StructuredMode structuredMode = StructuredMode.SUCCESS;
        private RagRetrieveRequest lastRetrieveRequest;
        private StructuredChatRequest lastStructuredRequest;

        StubAiGatewayClient(AiGatewayClientProperties properties) {
            super(RestClient.builder().baseUrl("http://localhost").build(), properties);
        }

        void reset() {
            this.retrieveMode = RetrieveMode.SUCCESS;
            this.structuredMode = StructuredMode.SUCCESS;
            this.lastRetrieveRequest = null;
            this.lastStructuredRequest = null;
        }

        void setRetrieveMode(RetrieveMode retrieveMode) {
            this.retrieveMode = retrieveMode;
        }

        void setStructuredMode(StructuredMode structuredMode) {
            this.structuredMode = structuredMode;
        }

        RagRetrieveRequest lastRetrieveRequest() {
            return lastRetrieveRequest;
        }

        StructuredChatRequest lastStructuredRequest() {
            return lastStructuredRequest;
        }

        @Override
        public AiGatewayCallResult<RagRetrieveResponse> ragRetrieve(RagRetrieveRequest request) {
            this.lastRetrieveRequest = request;
            if (retrieveMode == RetrieveMode.UNGROUNDED) {
                return AiGatewayCallResult.success(
                        new RagRetrieveResponse(false, "No relevant lexical knowledge found.", List.of(), List.of()),
                        1,
                        5L,
                        "/internal/ai/rag/retrieve"
                );
            }
            List<RagCitation> citations = List.of(
                    new RagCitation(
                            "C1",
                            "LEXICAL_PAIR",
                            "1001",
                            "coin / coin",
                            "This pair behaves like a false friend and cannot be judged from surface similarity alone.",
                            0.93d
                    ),
                    new RagCitation(
                            "C2",
                            "LEXICAL_EXAMPLE",
                            "3001",
                            "coin / coin - Example 1",
                            "I found a coin on the floor versus coin de la rue illustrate the meaning split.",
                            0.89d
                    )
            );
            List<RagContextChunk> contextChunks = List.of(
                    new RagContextChunk(
                            "C1",
                            "LEXICAL_PAIR",
                            "1001",
                            "coin / coin",
                            "English coin means money, while French coin often means corner in ordinary usage.",
                            "English coin means money, while French coin often means corner in ordinary usage.",
                            0.93d,
                            Map.of("chunkKind", "LEXICAL_PAIR")
                    ),
                    new RagContextChunk(
                            "C2",
                            "LEXICAL_EXAMPLE",
                            "3001",
                            "coin / coin - Example 1",
                            "I found a coin on the floor. / Le chat attend au coin de la rue.",
                            "I found a coin on the floor. / Le chat attend au coin de la rue.",
                            0.89d,
                            Map.of("chunkKind", "LEXICAL_EXAMPLE")
                    )
            );
            return AiGatewayCallResult.success(
                    new RagRetrieveResponse(true, null, citations, contextChunks),
                    1,
                    5L,
                    "/internal/ai/rag/retrieve"
            );
        }

        @Override
        public AiGatewayCallResult<StructuredChatResponse> structuredChat(StructuredChatRequest request) {
            this.lastStructuredRequest = request;
            if (structuredMode == StructuredMode.INVALID_JSON) {
                return AiGatewayCallResult.success(
                        new StructuredChatResponse(
                                "stub",
                                STRUCTURED_MODEL,
                                "{broken-json",
                                Map.of(),
                                "stop",
                                "structured-invalid",
                                new TokenUsage(90, 24, 114)
                        ),
                        1,
                        7L,
                        "/internal/ai/chat/structured"
                );
            }
            return AiGatewayCallResult.success(
                    new StructuredChatResponse(
                            "stub",
                            STRUCTURED_MODEL,
                            "structured-output",
                            successStructuredPayload(),
                            "stop",
                            "structured-success",
                            new TokenUsage(90, 24, 114)
                    ),
                    1,
                    7L,
                    "/internal/ai/chat/structured"
            );
        }

        private Map<String, Object> successStructuredPayload() {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("answer", "coin 在英语里通常指硬币，而法语里的 coin 在常用语境里更常指角落，因此只看词形很容易误判 [C1] [C2]");
            payload.put("explanation", "检索片段表明这组词对属于典型的语义分叉型 false friend，需要用语境和例句去拆开判断 [C1] [C2]");
            payload.put("recommendedActions", List.of(
                    "先对比两个词的核心义项。",
                    "再用例句检查是否可以互换。",
                    "最后记录最容易误判的语境线索。"
            ));
            payload.put("confidence", 0.83d);
            payload.put("citationIds", List.of("C1", "C2"));
            return payload;
        }
    }

    enum StructuredMode {
        SUCCESS,
        INVALID_JSON
    }

    enum RetrieveMode {
        SUCCESS,
        UNGROUNDED
    }
}
