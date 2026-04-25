package com.huashi.eftransfer.app.modules.internal;

import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalKnowledgeControllerIntegrationTest extends AbstractWebIntegrationTest {

    @Test
    void shouldExportLexicalKnowledgeWithToken() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");

        MvcResult createResult = mockMvc.perform(post("/api/lexical-pairs")
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "englishWord": "coin",
                                  "frenchWord": "coin",
                                  "chineseGloss": "硬币；角落",
                                  "lexicalPairType": "false_friend",
                                  "semanticOverlapScore": 0.10,
                                  "falseFriendRisk": 0.92,
                                  "defaultContextSupport": "high",
                                  "difficultyLevel": 4,
                                  "notes": "High confusion for beginners",
                                  "source": "Teacher Curated",
                                  "active": true,
                                  "knowledgeStatus": "ready",
                                  "embeddingStatus": "pending",
                                  "tags": ["false-friend", "high-frequency"],
                                  "senses": [
                                    {
                                      "sortOrder": 1,
                                      "englishDefinition": "a piece of money",
                                      "frenchDefinition": "une pièce de monnaie",
                                      "chineseDefinition": "硬币",
                                      "examples": [
                                        {
                                          "sortOrder": 1,
                                          "englishExample": "I found a coin on the floor.",
                                          "frenchExample": "J'ai trouvé une pièce par terre.",
                                          "chineseTranslation": "我在地上捡到一枚硬币。",
                                          "contextSupportLevel": "high",
                                          "source": "Teacher Curated"
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        long lexicalPairId = readJson(createResult).path("data").asLong();

        mockMvc.perform(get("/internal/knowledge/lexical-pairs/export")
                        .header("X-Internal-Token", "test-internal-knowledge-token")
                        .param("ids", String.valueOf(lexicalPairId))
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].lexicalPairId").value((int) lexicalPairId))
                .andExpect(jsonPath("$.data.items[0].englishWord").value("coin"))
                .andExpect(jsonPath("$.data.items[0].senses.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].senses[0].examples.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].tags.length()").value(2));
    }

    @Test
    void shouldRejectInvalidInternalKnowledgeToken() throws Exception {
        mockMvc.perform(get("/internal/knowledge/lexical-pairs/export")
                        .header("X-Internal-Token", "wrong-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void shouldRejectMissingInternalKnowledgeToken() throws Exception {
        mockMvc.perform(get("/internal/knowledge/lexical-pairs/export"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void shouldSyncLexicalPairEmbeddingStatusWithToken() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");

        MvcResult createResult = mockMvc.perform(post("/api/lexical-pairs")
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "englishWord": "embeddedstatusalpha",
                                  "frenchWord": "embeddedstatusalpha",
                                  "chineseGloss": "嵌入状态同步测试",
                                  "lexicalPairType": "cognate",
                                  "semanticOverlapScore": 0.95,
                                  "falseFriendRisk": 0.05,
                                  "defaultContextSupport": "low",
                                  "difficultyLevel": 1,
                                  "source": "Internal Sync Test",
                                  "active": true,
                                  "knowledgeStatus": "ready",
                                  "embeddingStatus": "pending",
                                  "tags": ["internal-sync"]
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        long lexicalPairId = readJson(createResult).path("data").asLong();
        ObjectNode payload = objectMapper.createObjectNode();
        ArrayNode items = payload.putArray("items");
        items.addObject()
                .put("lexicalPairId", lexicalPairId)
                .put("embeddingStatus", "embedded")
                .put("lastEmbeddedAt", "2026-04-25T15:54:55Z");

        mockMvc.perform(post("/internal/knowledge/lexical-pairs/embedding-status")
                        .header("X-Internal-Token", "test-internal-knowledge-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.updatedCount").value(1));

        mockMvc.perform(get("/api/lexical-pairs/{lexicalPairId}", lexicalPairId)
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.embeddingStatus").value("EMBEDDED"))
                .andExpect(jsonPath("$.data.lastEmbeddedAt").value("2026-04-25T15:54:55"));
    }
}
