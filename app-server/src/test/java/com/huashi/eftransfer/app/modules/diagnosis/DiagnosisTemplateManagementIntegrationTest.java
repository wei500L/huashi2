package com.huashi.eftransfer.app.modules.diagnosis;

import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DiagnosisTemplateManagementIntegrationTest extends AbstractWebIntegrationTest {

    @Test
    void shouldDeleteUnusedTemplateAndArchiveUsedTemplate() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");
        String studentToken = loginAndGetAccessToken("student.li", "Student@123456");

        long lexicalPairId = createLexicalPair(teacherToken);

        long unusedTemplateId = createTemplate(teacherToken, lexicalPairId, "Unused template");
        mockMvc.perform(delete("/api/teacher/diagnosis-templates/{templateId}", unusedTemplateId)
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateId").value((int) unusedTemplateId))
                .andExpect(jsonPath("$.data.outcome").value("DELETED"));

        mockMvc.perform(get("/api/teacher/diagnosis-templates/{templateId}", unusedTemplateId)
                        .with(bearer(teacherToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        long usedTemplateId = createTemplate(teacherToken, lexicalPairId, "Used template");
        mockMvc.perform(post("/api/diagnosis/sessions")
                        .with(bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateId": %d
                                }
                                """.formatted(usedTemplateId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        mockMvc.perform(delete("/api/teacher/diagnosis-templates/{templateId}", usedTemplateId)
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateId").value((int) usedTemplateId))
                .andExpect(jsonPath("$.data.outcome").value("ARCHIVED"))
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));

        mockMvc.perform(get("/api/teacher/diagnosis-templates/{templateId}", usedTemplateId)
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));
    }

    private long createLexicalPair(String teacherToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/lexical-pairs")
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
                                  "defaultContextSupport": "medium",
                                  "difficultyLevel": 4,
                                  "active": true,
                                  "tags": ["false-friend"]
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data").asLong();
    }

    private long createTemplate(String teacherToken, long lexicalPairId, String templateName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/teacher/diagnosis-templates")
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateName": "%s",
                                  "description": "Template lifecycle test",
                                  "status": "published",
                                  "estimatedDurationMinutes": 8,
                                  "scoringVersion": "RULE_V1",
                                  "items": [
                                    {
                                      "lexicalPairId": %d,
                                      "taskType": "reaction_time_task",
                                      "blockCode": "block_1",
                                      "sortOrder": 1,
                                      "contextSupportLevel": "medium",
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
                                      "correctAnswerKey": "semantic_mismatch"
                                    }
                                  ]
                                }
                                """.formatted(templateName, lexicalPairId)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data").asLong();
    }
}
