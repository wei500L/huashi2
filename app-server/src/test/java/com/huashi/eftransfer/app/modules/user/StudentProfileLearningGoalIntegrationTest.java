package com.huashi.eftransfer.app.modules.user;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.modules.user.entity.StudentProfileEntity;
import com.huashi.eftransfer.app.modules.user.entity.UserEntity;
import com.huashi.eftransfer.app.modules.user.mapper.StudentProfileMapper;
import com.huashi.eftransfer.app.modules.user.mapper.UserMapper;
import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StudentProfileLearningGoalIntegrationTest extends AbstractWebIntegrationTest {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StudentProfileMapper studentProfileMapper;

    @Test
    void shouldKeepGoalTimestampStableAcrossUnrelatedProfileUpdates() throws Exception {
        String studentToken = loginAndGetAccessToken("student.li", "Student@123456");
        Long studentUserId = requireUserId("student.li");

        MvcResult initialResult = mockMvc.perform(get("/api/student/profile/goals")
                        .with(bearer(studentToken)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(readJson(initialResult).path("data").path("updatedAt").isMissingNode()
                || readJson(initialResult).path("data").path("updatedAt").isNull()).isTrue();

        mockMvc.perform(put("/api/student/profile/goals")
                        .with(bearer(studentToken))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "dailyTrainingTarget": 3,
                                  "weeklyAccuracyTarget": 88
                                }
                                """))
                .andExpect(status().isOk());

        StudentProfileEntity profileAfterGoalUpdate = loadStudentProfile(studentUserId);
        LocalDateTime firstGoalUpdatedAt = profileAfterGoalUpdate.getLearningGoalsUpdatedAt();
        assertThat(firstGoalUpdatedAt).isNotNull();

        String apiTimestampAfterGoalUpdate = loadGoalTimestamp(studentToken);

        mockMvc.perform(put("/api/student/profile")
                        .with(bearer(studentToken))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "gradeName": "高二",
                                  "englishLevel": "B2",
                                  "frenchLevel": "A2",
                                  "courseStage": "INTERMEDIATE"
                                }
                                """))
                .andExpect(status().isOk());

        StudentProfileEntity profileAfterUnrelatedUpdate = loadStudentProfile(studentUserId);
        assertThat(profileAfterUnrelatedUpdate.getUpdatedAt()).isAfterOrEqualTo(firstGoalUpdatedAt);
        assertThat(profileAfterUnrelatedUpdate.getLearningGoalsUpdatedAt()).isEqualTo(firstGoalUpdatedAt);
        assertThat(loadGoalTimestamp(studentToken)).isEqualTo(apiTimestampAfterGoalUpdate);

        mockMvc.perform(put("/api/student/profile/goals")
                        .with(bearer(studentToken))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "dailyTrainingTarget": 3,
                                  "weeklyAccuracyTarget": 88
                                }
                                """))
                .andExpect(status().isOk());

        assertThat(loadStudentProfile(studentUserId).getLearningGoalsUpdatedAt()).isEqualTo(firstGoalUpdatedAt);

        mockMvc.perform(put("/api/student/profile/goals")
                        .with(bearer(studentToken))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "dailyTrainingTarget": 4,
                                  "weeklyAccuracyTarget": 90
                                }
                                """))
                .andExpect(status().isOk());

        assertThat(loadStudentProfile(studentUserId).getLearningGoalsUpdatedAt()).isAfter(firstGoalUpdatedAt);
    }

    @Test
    void shouldUpdateCurrentStudentProfile() throws Exception {
        String studentToken = loginAndGetAccessToken("student.li", "Student@123456");
        Long studentUserId = requireUserId("student.li");

        MvcResult result = mockMvc.perform(put("/api/student/profile")
                        .with(bearer(studentToken))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "gradeName": "  高三  ",
                                  "englishLevel": "c1",
                                  "frenchLevel": "b2",
                                  "courseStage": "advanced"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(readJson(result).path("data").path("gradeName").asText()).isEqualTo("高三");
        assertThat(readJson(result).path("data").path("englishLevel").asText()).isEqualTo("C1");
        assertThat(readJson(result).path("data").path("frenchLevel").asText()).isEqualTo("B2");
        assertThat(readJson(result).path("data").path("courseStage").asText()).isEqualTo("ADVANCED");

        StudentProfileEntity profile = loadStudentProfile(studentUserId);
        assertThat(profile.getGradeName()).isEqualTo("高三");
        assertThat(profile.getEnglishLevel()).isEqualTo("C1");
        assertThat(profile.getFrenchLevel()).isEqualTo("B2");
        assertThat(profile.getCourseStage()).isEqualTo("ADVANCED");
    }

    private Long requireUserId(String username) {
        UserEntity user = userMapper.selectOne(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getUsername, username)
                .last("LIMIT 1"));
        assertThat(user).isNotNull();
        return user.getId();
    }

    private StudentProfileEntity loadStudentProfile(Long studentUserId) {
        StudentProfileEntity profile = studentProfileMapper.selectOne(Wrappers.<StudentProfileEntity>lambdaQuery()
                .eq(StudentProfileEntity::getUserId, studentUserId)
                .last("LIMIT 1"));
        assertThat(profile).isNotNull();
        return profile;
    }

    private String loadGoalTimestamp(String studentToken) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/student/profile/goals")
                        .with(bearer(studentToken)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data").path("updatedAt").asText();
    }
}
