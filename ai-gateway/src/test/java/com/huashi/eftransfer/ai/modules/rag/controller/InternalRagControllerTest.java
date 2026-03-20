package com.huashi.eftransfer.ai.modules.rag.controller;

import com.huashi.eftransfer.ai.modules.rag.service.KnowledgeIngestionService;
import com.huashi.eftransfer.ai.modules.rag.service.RagService;
import com.huashi.eftransfer.shared.ai.RagAnswerResponse;
import com.huashi.eftransfer.shared.ai.RagCitation;
import com.huashi.eftransfer.shared.ai.RagContextChunk;
import com.huashi.eftransfer.shared.ai.RagExplainRiskResponse;
import com.huashi.eftransfer.shared.ai.RagReindexResponse;
import com.huashi.eftransfer.shared.ai.RagRetrieveResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InternalRagController.class)
class InternalRagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RagService ragService;

    @MockBean
    private KnowledgeIngestionService knowledgeIngestionService;

    @Test
    void shouldReturnRagAnswerPayload() throws Exception {
        when(ragService.answer(any())).thenReturn(new RagAnswerResponse(
                "coin / coin is risky because it can trigger false friend confusion [C1].",
                true,
                null,
                List.of(new RagCitation("C1", "LEXICAL_PAIR", "1001", "coin / coin", "False friend pair guidance", 0.88d)),
                List.of(new RagContextChunk(
                        "C1",
                        "LEXICAL_PAIR",
                        "1001",
                        "coin / coin",
                        "False friend pair guidance",
                        "False friend pair guidance",
                        0.88d,
                        Map.of("chunkKind", "LEXICAL_PAIR")
                ))
        ));

        mockMvc.perform(post("/internal/ai/rag/answer")
                        .header("X-Trace-Id", "trace-rag-answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "Why is coin/coin risky?"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.grounded").value(true))
                .andExpect(jsonPath("$.data.citations[0].citationId").value("C1"))
                .andExpect(jsonPath("$.data.contextChunks[0].sourceType").value("LEXICAL_PAIR"));
    }

    @Test
    void shouldReturnRagRetrievePayload() throws Exception {
        when(ragService.retrieve(any())).thenReturn(new RagRetrieveResponse(
                true,
                null,
                List.of(new RagCitation("C1", "LEXICAL_PAIR", "1001", "coin / coin", "False friend pair guidance", 0.88d)),
                List.of(new RagContextChunk(
                        "C1",
                        "LEXICAL_PAIR",
                        "1001",
                        "coin / coin",
                        "False friend pair guidance",
                        "False friend pair guidance",
                        0.88d,
                        Map.of("chunkKind", "LEXICAL_PAIR")
                ))
        ));

        mockMvc.perform(post("/internal/ai/rag/retrieve")
                        .header("X-Trace-Id", "trace-rag-retrieve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "Why is coin/coin risky?"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.grounded").value(true))
                .andExpect(jsonPath("$.data.citations[0].citationId").value("C1"))
                .andExpect(jsonPath("$.data.contextChunks[0].sourceType").value("LEXICAL_PAIR"));
    }

    @Test
    void shouldReturnExplainRiskPayload() throws Exception {
        when(ragService.explainRisk(any())).thenReturn(new RagExplainRiskResponse(
                "The learner is over-relying on surface similarity [C1].",
                "The pair behaves like a false friend and the dominant errors show unstable sense boundaries [C1].",
                "Prioritize contrastive false-friend discrimination before speed work [C2].",
                null,
                List.of(
                        new RagCitation("C1", "ERROR_TYPE", "false_friend_confusion", "False Friend Confusion", "False friend confusion happens when...", 0.91d),
                        new RagCitation("C2", "TRAINING_GUIDE", "false_friend_discrimination", "Training Guide: False Friend Discrimination", "Prioritize high-risk false friends...", 0.86d)
                ),
                List.of()
        ));

        mockMvc.perform(post("/internal/ai/rag/explain-risk")
                        .header("X-Trace-Id", "trace-rag-risk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "diagnosticSummary": {
                                    "negativeTransferRisk": 0.81,
                                    "contextSensitivity": 0.42,
                                    "overallAccuracy": 0.57,
                                    "averageReactionTimeMs": 1310
                                  },
                                  "errorTypeDistribution": [
                                    {
                                      "code": "false_friend_confusion",
                                      "label": "False Friend Confusion",
                                      "count": 4,
                                      "ratio": 0.5
                                    }
                                  ],
                                  "highRiskLexicalPairs": [
                                    {
                                      "lexicalPairId": 1001,
                                      "englishWord": "coin",
                                      "frenchWord": "coin",
                                      "chineseGloss": "硬币；角落",
                                      "lexicalPairType": "FALSE_FRIEND",
                                      "riskScore": 0.88,
                                      "errorCount": 3,
                                      "averageReactionTime": 1280,
                                      "dominantErrorType": "false_friend_confusion",
                                      "riskLevel": "CRITICAL"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.riskExplanation").value(org.hamcrest.Matchers.containsString("surface similarity")))
                .andExpect(jsonPath("$.data.citations.length()").value(2));
    }

    @Test
    void shouldReturnReindexJobPayload() throws Exception {
        when(knowledgeIngestionService.submit(any())).thenReturn(new RagReindexResponse(7L, "PENDING"));

        mockMvc.perform(post("/internal/ai/rag/reindex")
                        .header("X-Trace-Id", "trace-rag-reindex")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mode": "INCREMENTAL",
                                  "sourceTypes": ["LEXICAL_PAIR"],
                                  "forceReembed": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobId").value(7))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }
}
