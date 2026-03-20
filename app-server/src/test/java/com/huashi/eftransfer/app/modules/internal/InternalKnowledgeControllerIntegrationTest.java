package com.huashi.eftransfer.app.modules.internal;

import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

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
}
