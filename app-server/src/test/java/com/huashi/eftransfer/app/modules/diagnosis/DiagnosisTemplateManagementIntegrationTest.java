package com.huashi.eftransfer.app.modules.diagnosis;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.modules.analytics.entity.TeachingClassEntity;
import com.huashi.eftransfer.app.modules.analytics.mapper.TeachingClassMapper;
import com.huashi.eftransfer.app.modules.analytics.entity.TeachingClassStudentEntity;
import com.huashi.eftransfer.app.modules.analytics.mapper.TeachingClassStudentMapper;
import com.huashi.eftransfer.app.modules.user.entity.UserEntity;
import com.huashi.eftransfer.app.modules.user.mapper.UserMapper;
import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DiagnosisTemplateManagementIntegrationTest extends AbstractWebIntegrationTest {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TeachingClassMapper teachingClassMapper;

    @Autowired
    private TeachingClassStudentMapper teachingClassStudentMapper;

    @Test
    void shouldDeleteUnusedTemplateAndArchiveUsedTemplate() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");
        String studentToken = loginAndGetAccessToken("student.li", "Student@123456");

        long tablePairId = createLexicalPair(teacherToken, """
                {
                  "englishWord": "table",
                  "frenchWord": "table",
                  "chineseGloss": "桌子",
                  "lexicalPairType": "cognate",
                  "semanticOverlapScore": 0.95,
                  "falseFriendRisk": 0.05,
                  "defaultContextSupport": "low",
                  "difficultyLevel": 1,
                  "active": true,
                  "tags": ["basic"]
                }
                """);
        long coinPairId = createLexicalPair(teacherToken, """
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
                """);
        long actuallyPairId = createLexicalPair(teacherToken, """
                {
                  "englishWord": "actually",
                  "frenchWord": "actuellement",
                  "chineseGloss": "实际上；目前",
                  "lexicalPairType": "false_friend",
                  "semanticOverlapScore": 0.20,
                  "falseFriendRisk": 0.88,
                  "defaultContextSupport": "high",
                  "difficultyLevel": 4,
                  "active": true,
                  "tags": ["false-friend", "context"]
                }
                """);

        long unusedTemplateId = createDraftTemplate(teacherToken, tablePairId, "Unused template");
        mockMvc.perform(delete("/api/teacher/diagnosis-templates/{templateId}", unusedTemplateId)
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateId").value((int) unusedTemplateId))
                .andExpect(jsonPath("$.data.outcome").value("DELETED"));

        mockMvc.perform(get("/api/teacher/diagnosis-templates/{templateId}", unusedTemplateId)
                        .with(bearer(teacherToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        long usedTemplateId = createPublishedTemplate(teacherToken, tablePairId, coinPairId, actuallyPairId, "Used template");
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

    @Test
    void shouldExposePublicTemplatesInMarketAndCloneWithoutOverwritingSource() throws Exception {
        String adminToken = loginAndGetAccessToken("admin", "Admin@123456");
        String ownerToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");

        mockMvc.perform(post("/api/admin/users")
                        .with(bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "teacher.market",
                                  "email": "teacher.market@example.com",
                                  "displayName": "市场教师",
                                  "initialPassword": "Teacher@123456",
                                  "enabled": true,
                                  "roles": ["TEACHER"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("teacher.market"));
        String marketTeacherToken = loginAndGetAccessToken("teacher.market", "Teacher@123456");

        long tablePairId = createLexicalPair(ownerToken, """
                {
                  "englishWord": "table",
                  "frenchWord": "table",
                  "chineseGloss": "桌子",
                  "lexicalPairType": "cognate",
                  "semanticOverlapScore": 0.95,
                  "falseFriendRisk": 0.05,
                  "defaultContextSupport": "low",
                  "difficultyLevel": 1,
                  "active": true,
                  "tags": ["basic"]
                }
                """);
        long coinPairId = createLexicalPair(ownerToken, """
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
                """);
        long actuallyPairId = createLexicalPair(ownerToken, """
                {
                  "englishWord": "actually",
                  "frenchWord": "actuellement",
                  "chineseGloss": "实际上；目前",
                  "lexicalPairType": "false_friend",
                  "semanticOverlapScore": 0.20,
                  "falseFriendRisk": 0.88,
                  "defaultContextSupport": "high",
                  "difficultyLevel": 4,
                  "active": true,
                  "tags": ["false-friend", "context"]
                }
                """);

        long sourceTemplateId = createPublishedTemplate(
                ownerToken,
                tablePairId,
                coinPairId,
                actuallyPairId,
                "Shared template",
                null,
                "PUBLIC"
        );
        long classId = loadFirstAccessibleClassId(ownerToken);
        createPublishedTemplate(
                ownerToken,
                tablePairId,
                coinPairId,
                actuallyPairId,
                "Class scoped shared template",
                classId,
                "PUBLIC"
        );

        mockMvc.perform(get("/api/teacher/diagnosis-templates/market")
                        .with(bearer(marketTeacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value((int) sourceTemplateId))
                .andExpect(jsonPath("$.data.records[0].shareScope").value("PUBLIC"))
                .andExpect(jsonPath("$.data.records[0].ownerDisplayName").value("张老师"));

        MvcResult draftResult = mockMvc.perform(post("/api/teacher/diagnosis-template-drafts/from-template/{templateId}", sourceTemplateId)
                        .with(bearer(marketTeacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sourceTemplateId").value((int) sourceTemplateId))
                .andReturn();
        long draftId = readJson(draftResult).path("data").path("draftId").asLong();
        assertTrue(readJson(draftResult).path("data").path("publishedTemplateId").isNull());

        MvcResult publishResult = mockMvc.perform(post("/api/teacher/diagnosis-template-drafts/{draftId}/publish", draftId)
                        .with(bearer(marketTeacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ownerDisplayName").value("市场教师"))
                .andReturn();
        long clonedTemplateId = readJson(publishResult).path("data").path("id").asLong();
        assertNotEquals(sourceTemplateId, clonedTemplateId);

        mockMvc.perform(get("/api/teacher/diagnosis-templates/{templateId}", sourceTemplateId)
                        .with(bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateName").value("Shared template"))
                .andExpect(jsonPath("$.data.ownerDisplayName").value("张老师"))
                .andExpect(jsonPath("$.data.shareScope").value("PUBLIC"));

        mockMvc.perform(get("/api/teacher/diagnosis-templates/{templateId}", clonedTemplateId)
                        .with(bearer(marketTeacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateName").value("Shared template"))
                .andExpect(jsonPath("$.data.ownerDisplayName").value("市场教师"));
    }

    @Test
    void shouldFilterClassTargetedTemplatesForStudentsAndBlockUnauthorizedSessionCreation() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");
        String studentToken = loginAndGetAccessToken("student.li", "Student@123456");
        String otherStudentToken = loginAndGetAccessToken("student.wang", "Student@123456");

        long classId = loadFirstAccessibleClassId(teacherToken);
        UserEntity otherStudent = userMapper.selectByUsernameOrEmail("student.wang");
        TeachingClassStudentEntity otherMembership = teachingClassStudentMapper.selectOne(
                Wrappers.<TeachingClassStudentEntity>lambdaQuery()
                        .eq(TeachingClassStudentEntity::getStudentUserId, otherStudent.getId())
                        .eq(TeachingClassStudentEntity::getTeachingClassId, classId)
                        .last("LIMIT 1")
        );
        otherMembership.setActive(Boolean.FALSE);
        otherMembership.setLeftAt(LocalDateTime.now().minusDays(1));
        teachingClassStudentMapper.updateById(otherMembership);

        long tablePairId = createLexicalPair(teacherToken, """
                {
                  "englishWord": "table",
                  "frenchWord": "table",
                  "chineseGloss": "桌子",
                  "lexicalPairType": "cognate",
                  "semanticOverlapScore": 0.95,
                  "falseFriendRisk": 0.05,
                  "defaultContextSupport": "low",
                  "difficultyLevel": 1,
                  "active": true,
                  "tags": ["basic"]
                }
                """);
        long coinPairId = createLexicalPair(teacherToken, """
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
                """);
        long actuallyPairId = createLexicalPair(teacherToken, """
                {
                  "englishWord": "actually",
                  "frenchWord": "actuellement",
                  "chineseGloss": "实际上；目前",
                  "lexicalPairType": "false_friend",
                  "semanticOverlapScore": 0.20,
                  "falseFriendRisk": 0.88,
                  "defaultContextSupport": "high",
                  "difficultyLevel": 4,
                  "active": true,
                  "tags": ["false-friend", "context"]
                }
                """);

        long targetedTemplateId = createPublishedTemplate(
                teacherToken,
                tablePairId,
                coinPairId,
                actuallyPairId,
                "Class targeted template",
                classId
        );

        mockMvc.perform(get("/api/student/diagnosis-templates")
                        .with(bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].id").value((int) targetedTemplateId))
                .andExpect(jsonPath("$.data.records[0].targetClassId").value((int) classId));

        mockMvc.perform(get("/api/student/diagnosis-templates")
                        .with(bearer(otherStudentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(post("/api/diagnosis/sessions")
                        .with(bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateId": %d
                                }
                                """.formatted(targetedTemplateId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        mockMvc.perform(post("/api/diagnosis/sessions")
                        .with(bearer(otherStudentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateId": %d
                                }
                                """.formatted(targetedTemplateId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Student is not in the teaching class"));
    }

    @Test
    void shouldHideArchivedClassTemplatesAndBlockSessionCreation() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");
        String studentToken = loginAndGetAccessToken("student.li", "Student@123456");

        long classId = loadFirstAccessibleClassId(teacherToken);
        long tablePairId = createLexicalPair(teacherToken, """
                {
                  "englishWord": "table",
                  "frenchWord": "table",
                  "chineseGloss": "桌子",
                  "lexicalPairType": "cognate",
                  "semanticOverlapScore": 0.95,
                  "falseFriendRisk": 0.05,
                  "defaultContextSupport": "low",
                  "difficultyLevel": 1,
                  "active": true,
                  "tags": ["basic"]
                }
                """);
        long coinPairId = createLexicalPair(teacherToken, """
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
                """);
        long actuallyPairId = createLexicalPair(teacherToken, """
                {
                  "englishWord": "actually",
                  "frenchWord": "actuellement",
                  "chineseGloss": "实际上；目前",
                  "lexicalPairType": "false_friend",
                  "semanticOverlapScore": 0.20,
                  "falseFriendRisk": 0.88,
                  "defaultContextSupport": "high",
                  "difficultyLevel": 4,
                  "active": true,
                  "tags": ["false-friend", "context"]
                }
                """);
        long targetedTemplateId = createPublishedTemplate(
                teacherToken,
                tablePairId,
                coinPairId,
                actuallyPairId,
                "Archived class template",
                classId
        );

        TeachingClassEntity teachingClass = teachingClassMapper.selectById(classId);
        teachingClass.setActive(Boolean.FALSE);
        teachingClassMapper.updateById(teachingClass);

        mockMvc.perform(get("/api/student/diagnosis-templates")
                        .with(bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(post("/api/diagnosis/sessions")
                        .with(bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateId": %d
                                }
                                """.formatted(targetedTemplateId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Teaching class was not found"));
    }

    private long createLexicalPair(String teacherToken, String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/lexical-pairs")
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data").asLong();
    }

    private long createDraftTemplate(String teacherToken, long lexicalPairId, String templateName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/teacher/diagnosis-templates")
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateName": "%s",
                                  "description": "Template lifecycle test",
                                  "status": "draft",
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
                                      "correctAnswerKey": "semantic_mismatch",
                                      "scoringProfile": {
                                        "formulaKey": "RULE_V1",
                                        "pairWeight": 0.6,
                                        "riskAmplifier": 1.92,
                                        "maxReactionTimeMs": 1500
                                      }
                                    }
                                  ]
                                }
                                """.formatted(templateName, lexicalPairId)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data").asLong();
    }

    private long createPublishedTemplate(
            String teacherToken,
            long tablePairId,
            long coinPairId,
            long actuallyPairId,
            String templateName
    ) throws Exception {
        return createPublishedTemplate(teacherToken, tablePairId, coinPairId, actuallyPairId, templateName, null, null);
    }

    private long createPublishedTemplate(
            String teacherToken,
            long tablePairId,
            long coinPairId,
            long actuallyPairId,
            String templateName,
            Long targetClassId
    ) throws Exception {
        return createPublishedTemplate(teacherToken, tablePairId, coinPairId, actuallyPairId, templateName, targetClassId, null);
    }

    private long createPublishedTemplate(
            String teacherToken,
            long tablePairId,
            long coinPairId,
            long actuallyPairId,
            String templateName,
            Long targetClassId,
            String shareScope
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/teacher/diagnosis-templates")
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateName": "%s",
                                  "description": "Template lifecycle test",
                                  "status": "published",
                                  "estimatedDurationMinutes": 8,
                                  "targetClassId": %s,
                                  "shareScope": %s,
                                  "scoringVersion": "RULE_V1",
                                  "items": [
                                    {
                                      "lexicalPairId": %d,
                                      "taskType": "reaction_time_task",
                                      "blockCode": "block_1",
                                      "sortOrder": 1,
                                      "contextSupportLevel": "low",
                                      "expectedSemanticMatch": true,
                                      "stimulus": {
                                        "instruction": "Quickly decide whether the core meaning matches",
                                        "contextSentence": "",
                                        "promptText": "Semantic match?"
                                      },
                                      "options": [
                                        { "key": "semantic_match", "label": "语义一致", "semanticMatch": true, "ignoreContextTrap": false },
                                        { "key": "semantic_mismatch", "label": "语义不一致", "semanticMatch": false, "ignoreContextTrap": false }
                                      ],
                                      "correctAnswerKey": "semantic_match"
                                    },
                                    {
                                      "lexicalPairId": %d,
                                      "taskType": "reaction_time_task",
                                      "blockCode": "block_2",
                                      "sortOrder": 2,
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
                                    },
                                    {
                                      "lexicalPairId": %d,
                                      "taskType": "semantic_judgement_task",
                                      "blockCode": "block_3",
                                      "sortOrder": 3,
                                      "contextSupportLevel": "high",
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
                                      "correctAnswerKey": "currently_correct"
                                    }
                                  ]
                                }
                                """.formatted(
                                        templateName,
                                        targetClassId == null ? "null" : targetClassId,
                                        shareScope == null ? "null" : "\"" + shareScope + "\"",
                                        tablePairId,
                                        coinPairId,
                                        actuallyPairId
                                )))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data").asLong();
    }

    private long loadFirstAccessibleClassId(String teacherToken) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/teacher/analytics/classes")
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data").path(0).path("classId").asLong();
    }
}
