package com.huashi.eftransfer.app.modules.user;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.modules.analytics.entity.LearningProfileSnapshotEntity;
import com.huashi.eftransfer.app.modules.analytics.mapper.LearningProfileSnapshotMapper;
import com.huashi.eftransfer.app.modules.analytics.support.AnalyticsJsonCodec;
import com.huashi.eftransfer.app.modules.analytics.support.StudentAnalyticsSnapshotPayload;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StudentProfileLearningGoalIntegrationTest extends AbstractWebIntegrationTest {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StudentProfileMapper studentProfileMapper;

    @Autowired
    private LearningProfileSnapshotMapper learningProfileSnapshotMapper;

    @Autowired
    private AnalyticsJsonCodec analyticsJsonCodec;

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

    @Test
    void shouldCreateMissingStudentProfileOnFirstSave() throws Exception {
        String adminToken = loginAndGetAccessToken("admin", "Admin@123456");

        mockMvc.perform(post("/api/admin/users")
                        .with(bearer(adminToken))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "profile.bootstrap",
                                  "email": "profile.bootstrap@example.com",
                                  "displayName": "Profile Bootstrap",
                                  "credentialMode": "MANUAL_PASSWORD",
                                  "initialPassword": "Profile@123456",
                                  "enabled": true,
                                  "roles": ["STUDENT"]
                                }
                                """))
                .andExpect(status().isOk());

        Long studentUserId = requireUserId("profile.bootstrap");
        assertThat(studentProfileMapper.selectOne(Wrappers.<StudentProfileEntity>lambdaQuery()
                .eq(StudentProfileEntity::getUserId, studentUserId)
                .last("LIMIT 1"))).isNull();

        String studentToken = loginAndGetAccessToken("profile.bootstrap", "Profile@123456");
        mockMvc.perform(put("/api/student/profile")
                        .with(bearer(studentToken))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "gradeName": "高一",
                                  "englishLevel": "b1",
                                  "frenchLevel": "a2",
                                  "courseStage": "foundation"
                                }
                                """))
                .andExpect(status().isOk());

        StudentProfileEntity profile = loadStudentProfile(studentUserId);
        assertThat(profile.getStudentNo()).startsWith("S");
        assertThat(profile.getCompositeScore()).isZero();
        assertThat(profile.getGradeName()).isEqualTo("高一");
        assertThat(profile.getEnglishLevel()).isEqualTo("B1");
        assertThat(profile.getFrenchLevel()).isEqualTo("A2");
        assertThat(profile.getCourseStage()).isEqualTo("FOUNDATION");
    }

    @Test
    void shouldRefreshAnalyticsSnapshotProfileFieldsAfterProfileUpdate() throws Exception {
        String studentToken = loginAndGetAccessToken("student.li", "Student@123456");
        Long studentUserId = requireUserId("student.li");

        LearningProfileSnapshotEntity snapshot = new LearningProfileSnapshotEntity();
        snapshot.setScope("STUDENT");
        snapshot.setStudentUserId(studentUserId);
        snapshot.setPrimaryRiskLevel("HIGH");
        snapshot.setRecommendedTrainingMode("FALSE_FRIEND_DISCRIM");
        snapshot.setPendingReviewCount(5);
        snapshot.setHighRiskPairCount(2);
        snapshot.setSnapshotJson(analyticsJsonCodec.write(new StudentAnalyticsSnapshotPayload(
                "Student Li",
                "旧年级",
                "A1",
                "A2",
                11L,
                22L,
                "HIGH",
                "FALSE_FRIEND_DISCRIM",
                5,
                2,
                0.73d,
                0.41d,
                980L,
                LocalDateTime.now().minusDays(1),
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of("focus-tag")
        )));
        learningProfileSnapshotMapper.insert(snapshot);

        mockMvc.perform(put("/api/student/profile")
                        .with(bearer(studentToken))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "gradeName": "高三",
                                  "englishLevel": "c1",
                                  "frenchLevel": "b2",
                                  "courseStage": "advanced"
                                }
                                """))
                .andExpect(status().isOk());

        LearningProfileSnapshotEntity refreshedSnapshot = learningProfileSnapshotMapper.selectOne(
                Wrappers.<LearningProfileSnapshotEntity>lambdaQuery()
                        .eq(LearningProfileSnapshotEntity::getScope, "STUDENT")
                        .eq(LearningProfileSnapshotEntity::getStudentUserId, studentUserId)
                        .last("LIMIT 1")
        );
        assertThat(refreshedSnapshot).isNotNull();
        StudentAnalyticsSnapshotPayload payload = analyticsJsonCodec.read(
                refreshedSnapshot.getSnapshotJson(),
                StudentAnalyticsSnapshotPayload.class
        );
        assertThat(payload.gradeName()).isEqualTo("高三");
        assertThat(payload.englishLevel()).isEqualTo("C1");
        assertThat(payload.frenchLevel()).isEqualTo("B2");
        assertThat(payload.primaryRiskLevel()).isEqualTo("HIGH");
        assertThat(payload.recommendedTrainingMode()).isEqualTo("FALSE_FRIEND_DISCRIM");

        MvcResult overviewResult = mockMvc.perform(get("/api/student/analytics/overview")
                        .with(bearer(studentToken)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(readJson(overviewResult).path("data").path("gradeName").asText()).isEqualTo("高三");
        assertThat(readJson(overviewResult).path("data").path("englishLevel").asText()).isEqualTo("C1");
        assertThat(readJson(overviewResult).path("data").path("frenchLevel").asText()).isEqualTo("B2");
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
