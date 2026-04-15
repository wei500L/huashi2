package com.huashi.eftransfer.app.modules.analytics;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.modules.analytics.entity.TeachingClassEntity;
import com.huashi.eftransfer.app.modules.analytics.mapper.TeachingClassMapper;
import com.huashi.eftransfer.app.modules.user.entity.UserEntity;
import com.huashi.eftransfer.app.modules.user.mapper.UserMapper;
import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeacherClassControllerIntegrationTest extends AbstractWebIntegrationTest {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TeachingClassMapper teachingClassMapper;

    @Test
    void shouldSupportTeacherClassCrudAndStudentAssignmentFlow() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");
        UserEntity studentLi = userMapper.selectByUsernameOrEmail("student.li");
        UserEntity studentWang = userMapper.selectByUsernameOrEmail("student.wang");
        assertThat(studentLi).isNotNull();
        assertThat(studentWang).isNotNull();

        MvcResult inviteCodeResult = mockMvc.perform(post("/api/teacher/classes/invite-code")
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.classCode").isString())
                .andReturn();
        String generatedInviteCode = readJson(inviteCodeResult).path("data").path("classCode").asText();
        assertThat(generatedInviteCode).startsWith("CLS-");
        assertThat(generatedInviteCode).hasSize(10);

        MvcResult createResult = mockMvc.perform(post("/api/teacher/classes")
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "classCode": "%s",
                                  "className": "2026 教学实验班",
                                  "gradeName": "Grade 12"
                                }
                                """.formatted(generatedInviteCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.classCode").value(generatedInviteCode))
                .andExpect(jsonPath("$.data.className").value("2026 教学实验班"))
                .andExpect(jsonPath("$.data.studentCount").value(0))
                .andReturn();
        JsonNode createdPayload = readJson(createResult).path("data");
        long classId = createdPayload.path("classId").asLong();

        mockMvc.perform(get("/api/teacher/classes/{classId}/student-candidates", classId)
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(post("/api/teacher/classes/{classId}/students", classId)
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentUserIds": [%d, %d]
                                }
                                """.formatted(studentLi.getId(), studentWang.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentCount").value(2))
                .andExpect(jsonPath("$.data.students.length()").value(2));

        mockMvc.perform(post("/api/teacher/classes/{classId}/students/remove", classId)
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentUserIds": [%d]
                                }
                                """.formatted(studentWang.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentCount").value(1))
                .andExpect(jsonPath("$.data.students.length()").value(1))
                .andExpect(jsonPath("$.data.students[0].studentUserId").value(studentLi.getId()));

        mockMvc.perform(put("/api/teacher/classes/{classId}", classId)
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "classCode": "CLS-2026-UPDATED",
                                  "className": "2026 教学实验班-更新",
                                  "gradeName": "Grade 12 Advanced"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.classCode").value("CLS-2026-UPDATED"))
                .andExpect(jsonPath("$.data.className").value("2026 教学实验班-更新"))
                .andExpect(jsonPath("$.data.gradeName").value("Grade 12 Advanced"));

        mockMvc.perform(delete("/api/teacher/classes/{classId}", classId)
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk());

        MvcResult listResult = mockMvc.perform(get("/api/teacher/classes")
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode listPayload = readJson(listResult).path("data");
        assertThat(listPayload.isArray()).isTrue();
        for (JsonNode item : listPayload) {
            assertThat(item.path("classId").asLong()).isNotEqualTo(classId);
        }

        TeachingClassEntity archivedClass = teachingClassMapper.selectOne(Wrappers.<TeachingClassEntity>lambdaQuery()
                .eq(TeachingClassEntity::getId, classId));
        assertThat(archivedClass).isNotNull();
        assertThat(archivedClass.getActive()).isFalse();
    }
}
