package com.huashi.eftransfer.app.modules.diagnosis;

import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DiagnosisTemplateDraftIntegrationTest extends AbstractWebIntegrationTest {

    @Test
    void shouldFlagDuplicateOptionKeysDuringDraftValidation() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");
        long draftId = createDraftWithDuplicateOptionKeys(teacherToken);

        mockMvc.perform(post("/api/teacher/diagnosis-template-drafts/{draftId}/validate", draftId)
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(false))
                .andExpect(jsonPath("$.data.fieldErrors").isMap())
                .andExpect(jsonPath("$.data.itemErrors[0].fieldErrors.options").value("选项 key 不能重复：semantic_match。"));
    }

    @Test
    void shouldRejectPublishingDraftWithDuplicateOptionKeys() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");
        long draftId = createDraftWithDuplicateOptionKeys(teacherToken);

        mockMvc.perform(post("/api/teacher/diagnosis-template-drafts/{draftId}/publish", draftId)
                        .with(bearer(teacherToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("选项 key 不能重复：semantic_match。"));
    }

    private long createDraftWithDuplicateOptionKeys(String teacherToken) throws Exception {
        long lowContextPairId = createLexicalPair(teacherToken, "table", "table", "桌子", "low");
        long mediumContextPairId = createLexicalPair(teacherToken, "coin", "coin", "硬币；角落", "medium");
        long highContextPairId = createLexicalPair(teacherToken, "actually", "actuellement", "实际上；目前", "high");

        MvcResult createDraftResult = mockMvc.perform(post("/api/teacher/diagnosis-template-drafts")
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andReturn();
        long draftId = readJson(createDraftResult).path("data").path("draftId").asLong();
        long version = readJson(createDraftResult).path("data").path("version").asLong();

        mockMvc.perform(put("/api/teacher/diagnosis-template-drafts/{draftId}", draftId)
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": %d,
                                  "schema": {
                                    "basic": {
                                      "templateName": "Duplicate option key draft",
                                      "description": "Regression coverage for duplicate option keys",
                                      "publishTarget": "SELF",
                                      "estimatedDurationMinutes": 8,
                                      "scoringVersion": "RULE_V1"
                                    },
                                    "items": [
                                      {
                                        "draftItemId": "item-low",
                                        "lexicalPairId": %d,
                                        "taskType": "REACTION_TIME",
                                        "blockCode": "B1",
                                        "sortOrder": 1,
                                        "contextSupportLevel": "LOW",
                                        "expectedSemanticMatch": true,
                                        "stimulus": {
                                          "instruction": "Quickly decide whether the meanings align",
                                          "contextSentence": "",
                                          "promptText": "Semantic match?"
                                        },
                                        "options": [
                                          { "key": "semantic_match", "label": "语义一致", "semanticMatch": true, "ignoreContextTrap": false },
                                          { "key": "semantic_match", "label": "语义不一致", "semanticMatch": false, "ignoreContextTrap": false }
                                        ],
                                        "correctAnswerKey": "semantic_match",
                                        "scoringProfile": null
                                      },
                                      {
                                        "draftItemId": "item-medium",
                                        "lexicalPairId": %d,
                                        "taskType": "REACTION_TIME",
                                        "blockCode": "B2",
                                        "sortOrder": 2,
                                        "contextSupportLevel": "MEDIUM",
                                        "expectedSemanticMatch": false,
                                        "stimulus": {
                                          "instruction": "Trust your first response",
                                          "contextSentence": "He waited in the coin of the room.",
                                          "promptText": "Semantic match?"
                                        },
                                        "options": [
                                          { "key": "semantic_match", "label": "语义一致", "semanticMatch": true, "ignoreContextTrap": false },
                                          { "key": "semantic_mismatch", "label": "语义不一致", "semanticMatch": false, "ignoreContextTrap": false }
                                        ],
                                        "correctAnswerKey": "semantic_mismatch",
                                        "scoringProfile": null
                                      },
                                      {
                                        "draftItemId": "item-high",
                                        "lexicalPairId": %d,
                                        "taskType": "SEMANTIC_JUDGEMENT",
                                        "blockCode": "B3",
                                        "sortOrder": 3,
                                        "contextSupportLevel": "HIGH",
                                        "expectedSemanticMatch": false,
                                        "stimulus": {
                                          "instruction": "Use the sentence context to choose the correct gloss",
                                          "contextSentence": "Il travaille actuellement a Paris.",
                                          "promptText": "Which gloss fits the French word?"
                                        },
                                        "options": [
                                          { "key": "actually_trap", "label": "Actually (实际上)", "semanticMatch": true, "ignoreContextTrap": true },
                                          { "key": "currently_correct", "label": "Currently (目前)", "semanticMatch": false, "ignoreContextTrap": false },
                                          { "key": "occasionally", "label": "Occasionally (偶尔)", "semanticMatch": false, "ignoreContextTrap": false }
                                        ],
                                        "correctAnswerKey": "currently_correct",
                                        "scoringProfile": null
                                      }
                                    ]
                                  }
                                }
                                """.formatted(version, lowContextPairId, mediumContextPairId, highContextPairId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.draftId").value((int) draftId));
        return draftId;
    }

    private long createLexicalPair(
            String teacherToken,
            String englishWord,
            String frenchWord,
            String chineseGloss,
            String defaultContextSupport
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/lexical-pairs")
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "englishWord": "%s",
                                  "frenchWord": "%s",
                                  "chineseGloss": "%s",
                                  "lexicalPairType": "false_friend",
                                  "semanticOverlapScore": 0.20,
                                  "falseFriendRisk": 0.88,
                                  "defaultContextSupport": "%s",
                                  "difficultyLevel": 4,
                                  "active": true,
                                  "tags": ["false-friend"]
                                }
                                """.formatted(englishWord, frenchWord, chineseGloss, defaultContextSupport)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data").asLong();
    }
}
