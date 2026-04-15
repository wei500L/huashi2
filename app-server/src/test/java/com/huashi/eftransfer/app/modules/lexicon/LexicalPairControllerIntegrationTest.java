package com.huashi.eftransfer.app.modules.lexicon;

import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LexicalPairControllerIntegrationTest extends AbstractWebIntegrationTest {

    @Test
    void shouldCreateQueryDetailUpdateAndDeleteLexicalPair() throws Exception {
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
                                      "frenchDefinition": "coin de rue",
                                      "chineseDefinition": "硬币；角落",
                                      "examples": [
                                        {
                                          "sortOrder": 1,
                                          "englishExample": "I found a coin on the floor.",
                                          "frenchExample": "Le chat dort dans le coin.",
                                          "chineseTranslation": "我在地上捡到一枚硬币；猫睡在角落里。",
                                          "contextSupportLevel": "high",
                                          "source": "Teacher Curated"
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isNumber())
                .andReturn();

        long lexicalPairId = readJson(createResult).path("data").asLong();
        assertThat(lexicalPairId).isPositive();

        mockMvc.perform(get("/api/lexical-pairs")
                        .with(bearer(teacherToken))
                        .param("pageNo", "1")
                        .param("pageSize", "10")
                        .param("keyword", "coin")
                        .param("lexicalPairType", "FALSE_FRIEND")
                        .param("riskLevel", "critical")
                        .param("contextSupportLevel", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].englishWord").value("coin"))
                .andExpect(jsonPath("$.data.records[0].riskLevel").value("CRITICAL"))
                .andExpect(jsonPath("$.data.records[0].tags.length()").value(2));

        mockMvc.perform(get("/api/lexical-pairs")
                        .with(bearer(teacherToken))
                        .param("pageNo", "1")
                        .param("pageSize", "10")
                        .param("keyword", "yingbi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].englishWord").value("coin"));

        mockMvc.perform(get("/api/lexical-pairs/suggestions")
                        .with(bearer(teacherToken))
                        .param("keyword", "yb")
                        .param("limit", "5")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].englishWord").value("coin"))
                .andExpect(jsonPath("$.data[0].matchedBy").value("INITIALS"));

        mockMvc.perform(get("/api/lexical-pairs/{id}", lexicalPairId)
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.englishWord").value("coin"))
                .andExpect(jsonPath("$.data.senses.length()").value(1))
                .andExpect(jsonPath("$.data.senses[0].examples.length()").value(1))
                .andExpect(jsonPath("$.data.knowledgeStatus").value("READY"));

        mockMvc.perform(put("/api/lexical-pairs/{id}", lexicalPairId)
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "englishWord": "coin",
                                  "frenchWord": "coin",
                                  "chineseGloss": "硬币；角落",
                                  "lexicalPairType": "false_friend",
                                  "semanticOverlapScore": 0.12,
                                  "falseFriendRisk": 0.60,
                                  "defaultContextSupport": "medium",
                                  "difficultyLevel": 3,
                                  "notes": "Updated",
                                  "source": "Updated Source",
                                  "active": true,
                                  "knowledgeStatus": "draft",
                                  "embeddingStatus": "failed",
                                  "tags": ["updated-tag"],
                                  "senses": [
                                    {
                                      "sortOrder": 1,
                                      "englishDefinition": "money",
                                      "frenchDefinition": "coin",
                                      "chineseDefinition": "钱币",
                                      "examples": [
                                        {
                                          "sortOrder": 1,
                                          "englishExample": "A coin is shiny.",
                                          "frenchExample": "Le coin est petit.",
                                          "chineseTranslation": "硬币很亮。",
                                          "contextSupportLevel": "medium",
                                          "source": "Updated Source"
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value((int) lexicalPairId));

        mockMvc.perform(get("/api/lexical-pairs/{id}", lexicalPairId)
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.data.defaultContextSupport").value("MEDIUM"))
                .andExpect(jsonPath("$.data.tags[0]").value("updated-tag"));

        mockMvc.perform(delete("/api/lexical-pairs/{id}", lexicalPairId)
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/lexical-pairs/{id}", lexicalPairId)
                        .with(bearer(teacherToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRankSuggestionsBeforeApplyingResponseLimit() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");

        createLexicalPair(teacherToken, "focus", "foyer", "焦点");
        for (int index = 1; index <= 31; index++) {
            createLexicalPair(
                    teacherToken,
                    "filler-%02d".formatted(index),
                    "remplissage-%02d".formatted(index),
                    "focus 干扰词 %02d".formatted(index)
            );
        }

        mockMvc.perform(get("/api/lexical-pairs/suggestions")
                        .with(bearer(teacherToken))
                        .param("keyword", "focus")
                        .param("limit", "5")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[0].englishWord").value("focus"))
                .andExpect(jsonPath("$.data[0].matchedBy").value("ENGLISH_WORD"));
    }

    private long createLexicalPair(String teacherToken, String englishWord, String frenchWord, String chineseGloss) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/lexical-pairs")
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "englishWord": "%s",
                                  "frenchWord": "%s",
                                  "chineseGloss": "%s",
                                  "lexicalPairType": "false_friend",
                                  "semanticOverlapScore": 0.10,
                                  "falseFriendRisk": 0.60,
                                  "defaultContextSupport": "high",
                                  "difficultyLevel": 3,
                                  "notes": "Suggestion ranking fixture",
                                  "source": "Test Fixture",
                                  "active": true,
                                  "knowledgeStatus": "ready",
                                  "embeddingStatus": "pending",
                                  "tags": ["suggestion-test"],
                                  "senses": [
                                    {
                                      "sortOrder": 1,
                                      "englishDefinition": "%s definition",
                                      "frenchDefinition": "%s definition",
                                      "chineseDefinition": "%s 释义",
                                      "examples": [
                                        {
                                          "sortOrder": 1,
                                          "englishExample": "%s example",
                                          "frenchExample": "%s exemple",
                                          "chineseTranslation": "%s 例句",
                                          "contextSupportLevel": "high",
                                          "source": "Test Fixture"
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """.formatted(englishWord, frenchWord, chineseGloss, englishWord, frenchWord, chineseGloss, englishWord, frenchWord, chineseGloss)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data").asLong();
    }
}
