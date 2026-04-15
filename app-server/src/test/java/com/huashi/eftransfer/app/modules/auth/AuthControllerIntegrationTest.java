package com.huashi.eftransfer.app.modules.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.modules.analytics.entity.TeachingClassEntity;
import com.huashi.eftransfer.app.modules.analytics.entity.TeachingClassStudentEntity;
import com.huashi.eftransfer.app.modules.analytics.mapper.TeachingClassMapper;
import com.huashi.eftransfer.app.modules.analytics.mapper.TeachingClassStudentMapper;
import com.huashi.eftransfer.app.modules.user.entity.StudentProfileEntity;
import com.huashi.eftransfer.app.modules.user.entity.UserEntity;
import com.huashi.eftransfer.app.modules.user.entity.UserRoleEntity;
import com.huashi.eftransfer.app.modules.user.mapper.StudentProfileMapper;
import com.huashi.eftransfer.app.modules.user.mapper.UserMapper;
import com.huashi.eftransfer.app.modules.user.mapper.UserRoleMapper;
import com.huashi.eftransfer.app.support.MockMvcTestSupport;
import com.huashi.eftransfer.app.support.TestAuthTokenStoreConfiguration;
import com.huashi.eftransfer.app.common.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest(properties = {
        "app.security.rate-limit.auth.login.ip.limit=10",
        "app.security.rate-limit.auth.login.ip.window=PT1H",
        "app.security.rate-limit.auth.login.principal.limit=10",
        "app.security.rate-limit.auth.login.principal.window=PT1H",
        "app.security.rate-limit.auth.refresh.ip.limit=5",
        "app.security.rate-limit.auth.refresh.ip.window=PT1H",
        "app.security.rate-limit.auth.refresh.session.limit=2",
        "app.security.rate-limit.auth.refresh.session.window=PT1H"
})
@ActiveProfiles("test")
@Import(TestAuthTokenStoreConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthControllerIntegrationTest {

    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/122.0.0.0 Safari/537.36";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private StudentProfileMapper studentProfileMapper;

    @Autowired
    private TeachingClassMapper teachingClassMapper;

    @Autowired
    private TeachingClassStudentMapper teachingClassStudentMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcTestSupport.build(webApplicationContext);
    }

    @Test
    void shouldLoginRefreshGetCurrentUserAndLogout() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usernameOrEmail": "admin",
                                  "password": "Admin@123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userInfo.primaryRole").value("ADMIN"))
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginJson.path("data").path("accessToken").asText();
        String refreshToken = loginJson.path("data").path("refreshToken").asText();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.primaryRole").value("ADMIN"))
                .andExpect(jsonPath("$.data.capabilities").isArray())
                .andExpect(jsonPath("$.data.capabilities[0]").value("ADMIN_CONSOLE"))
                .andExpect(jsonPath("$.data.capabilities[1]").value("TEACHING_WORKSPACE"))
                .andExpect(jsonPath("$.data.capabilities[2]").value("STUDENT_WORKSPACE"));

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userInfo.username").value("admin"))
                .andReturn();

        JsonNode refreshJson = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String rotatedAccessToken = refreshJson.path("data").path("accessToken").asText();
        String rotatedRefreshToken = refreshJson.path("data").path("refreshToken").asText();

        assertNotNull(rotatedAccessToken);
        assertNotNull(rotatedRefreshToken);
        assertNotEquals(accessToken, rotatedAccessToken);
        assertNotEquals(refreshToken, rotatedRefreshToken);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + rotatedAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout succeeded"));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + rotatedAccessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
    }

    @Test
    void shouldLoginWithEmail() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usernameOrEmail": "teacher.zhang@ef.local",
                                  "password": "Teacher@123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userInfo.username").value("teacher.zhang"))
                .andExpect(jsonPath("$.data.userInfo.primaryRole").value("TEACHER"));
    }

    @Test
    void shouldExposeCapabilityUnionForMultiRoleUser() throws Exception {
        UserEntity user = new UserEntity();
        user.setUsername("hybrid.user");
        user.setEmail("hybrid.user@ef.local");
        user.setPasswordHash(passwordEncoder.encode("Hybrid@123456"));
        user.setDisplayName("Hybrid User");
        user.setEnabled(true);
        userMapper.insert(user);

        UserRoleEntity studentRole = new UserRoleEntity();
        studentRole.setUserId(user.getId());
        studentRole.setRoleCode("STUDENT");
        userRoleMapper.insert(studentRole);

        UserRoleEntity teacherRole = new UserRoleEntity();
        teacherRole.setUserId(user.getId());
        teacherRole.setRoleCode("TEACHER");
        userRoleMapper.insert(teacherRole);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usernameOrEmail": "hybrid.user",
                                  "password": "Hybrid@123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userInfo.roles.length()").value(2))
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginJson.path("data").path("accessToken").asText();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("hybrid.user"))
                .andExpect(jsonPath("$.data.primaryRole").value("TEACHER"))
                .andExpect(jsonPath("$.data.capabilities.length()").value(2))
                .andExpect(jsonPath("$.data.capabilities[0]").value("STUDENT_WORKSPACE"))
                .andExpect(jsonPath("$.data.capabilities[1]").value("TEACHING_WORKSPACE"));
    }

    @Test
    void shouldRejectLoginWhenUserHasNoAssignedRoles() throws Exception {
        UserEntity user = new UserEntity();
        user.setUsername("roleless.user");
        user.setEmail("roleless.user@ef.local");
        user.setPasswordHash(passwordEncoder.encode("Roleless@123456"));
        user.setDisplayName("Roleless User");
        user.setEnabled(true);
        userMapper.insert(user);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usernameOrEmail": "roleless.user",
                                  "password": "Roleless@123456"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("User account has no assigned roles"));
    }

    @Test
    void shouldRejectCurrentUserWhenAccountHasNoAssignedRoles() throws Exception {
        UserEntity user = new UserEntity();
        user.setUsername("roleless.me");
        user.setEmail("roleless.me@ef.local");
        user.setPasswordHash(passwordEncoder.encode("Roleless@123456"));
        user.setDisplayName("Roleless Me");
        user.setEnabled(true);
        userMapper.insert(user);

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                java.util.Set.of()
        ).token();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("User account has no assigned roles"));
    }

    @Test
    void shouldAllowStudentSelfRegistrationAndAutomaticallyJoinClass() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "student.self",
                                  "email": "student.self@ef.local",
                                  "displayName": "自主注册学生",
                                  "password": "Student@123456",
                                  "classCode": "CLS-0001",
                                  "englishLevel": "B1",
                                  "frenchLevel": "A2",
                                  "courseStage": "FOUNDATION"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userInfo.username").value("student.self"))
                .andExpect(jsonPath("$.data.userInfo.primaryRole").value("STUDENT"))
                .andExpect(jsonPath("$.data.userInfo.studentProfile.gradeName").value("Pilot Grade"))
                .andExpect(jsonPath("$.data.userInfo.studentProfile.englishLevel").value("B1"))
                .andExpect(jsonPath("$.data.userInfo.studentProfile.frenchLevel").value("A2"))
                .andExpect(jsonPath("$.data.userInfo.studentProfile.courseStage").value("FOUNDATION"));

        UserEntity user = userMapper.selectByUsernameOrEmail("student.self");
        assertNotNull(user);

        assertEquals(1L, userRoleMapper.selectCount(Wrappers.<UserRoleEntity>lambdaQuery()
                .eq(UserRoleEntity::getUserId, user.getId())
                .eq(UserRoleEntity::getRoleCode, "STUDENT")));

        StudentProfileEntity studentProfile = studentProfileMapper.selectOne(Wrappers.<StudentProfileEntity>lambdaQuery()
                .eq(StudentProfileEntity::getUserId, user.getId())
                .last("LIMIT 1"));
        assertNotNull(studentProfile);
        assertEquals("Pilot Grade", studentProfile.getGradeName());

        TeachingClassEntity teachingClass = teachingClassMapper.selectOne(Wrappers.<TeachingClassEntity>lambdaQuery()
                .eq(TeachingClassEntity::getClassCode, "CLS-0001")
                .last("LIMIT 1"));
        assertNotNull(teachingClass);

        assertEquals(1L, teachingClassStudentMapper.selectCount(Wrappers.<TeachingClassStudentEntity>lambdaQuery()
                .eq(TeachingClassStudentEntity::getTeachingClassId, teachingClass.getId())
                .eq(TeachingClassStudentEntity::getStudentUserId, user.getId())
                .eq(TeachingClassStudentEntity::getActive, Boolean.TRUE)));
    }

    @Test
    void shouldRejectStudentRegistrationWhenClassInviteCodeIsInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "student.invalid",
                                  "email": "student.invalid@ef.local",
                                  "displayName": "邀请码错误学生",
                                  "password": "Student@123456",
                                  "classCode": "MISSING-CLASS",
                                  "englishLevel": "B1",
                                  "frenchLevel": "A2",
                                  "courseStage": "FOUNDATION"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Class invite code is invalid"));
    }

    @Test
    void shouldExposeRegistrationContextForValidClassInviteCode() throws Exception {
        mockMvc.perform(get("/api/auth/register/context/CLS-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.classCode").value("CLS-0001"))
                .andExpect(jsonPath("$.data.className").value("2024级英法迁移试点1班"))
                .andExpect(jsonPath("$.data.gradeName").value("Pilot Grade"));
    }

    @Test
    void shouldLockAccountAfterFiveFailedAttemptsAcrossUsernameAndEmail() throws Exception {
        for (int attempt = 0; attempt < 3; attempt++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "usernameOrEmail": "teacher.zhang",
                                      "password": "WrongPassword@123"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }

        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "usernameOrEmail": "teacher.zhang@ef.local",
                                      "password": "WrongPassword@123"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usernameOrEmail": "teacher.zhang@ef.local",
                                  "password": "Teacher@123456"
                                }
                                """))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
    }

    @Test
    void shouldClearFailedLoginCounterAfterSuccessfulAuthentication() throws Exception {
        for (int attempt = 0; attempt < 4; attempt++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "usernameOrEmail": "teacher.zhang",
                                      "password": "WrongPassword@123"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usernameOrEmail": "teacher.zhang@ef.local",
                                  "password": "Teacher@123456"
                                }
                                """))
                .andExpect(status().isOk());

        for (int attempt = 0; attempt < 4; attempt++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "usernameOrEmail": "teacher.zhang",
                                      "password": "WrongPassword@123"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usernameOrEmail": "teacher.zhang",
                                  "password": "Teacher@123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userInfo.username").value("teacher.zhang"));
    }

    @Test
    void shouldRateLimitRepeatedLoginAttempts() throws Exception {
        for (int attempt = 0; attempt < 10; attempt++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "usernameOrEmail": "missing-user-%s",
                                      "password": "WrongPassword@123"
                                    }
                                    """.formatted(attempt)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usernameOrEmail": "missing-user-overflow",
                                  "password": "WrongPassword@123"
                                }
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
                .andExpect(jsonPath("$.message").value("Too many login attempts"));
    }

    @Test
    void shouldRateLimitRefreshAttemptsPerSessionOwner() throws Exception {
        String refreshToken = loginAndGetRefreshToken();
        String rotatedRefreshToken = refreshAndGetRotatedRefreshToken(refreshToken);
        String nextRefreshToken = refreshAndGetRotatedRefreshToken(rotatedRefreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(nextRefreshToken)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
                .andExpect(jsonPath("$.message").value("Too many refresh attempts"));
    }

    @Test
    void shouldRejectRefreshWhenUserAgentChanges() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(remoteAddress("10.0.0.11"))
                        .header("User-Agent", DEFAULT_USER_AGENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usernameOrEmail": "admin",
                                  "password": "Admin@123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String refreshToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data")
                .path("refreshToken")
                .asText();

        mockMvc.perform(post("/api/auth/refresh")
                        .with(remoteAddress("10.0.0.12"))
                        .header("User-Agent", "curl/8.7.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"))
                .andExpect(jsonPath("$.message").value("Refresh token is invalid"));
    }

    @Test
    void shouldAllowRefreshWhenIpChangesButUserAgentMatches() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(remoteAddress("10.0.0.21"))
                        .header("User-Agent", DEFAULT_USER_AGENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usernameOrEmail": "admin",
                                  "password": "Admin@123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String refreshToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data")
                .path("refreshToken")
                .asText();

        mockMvc.perform(post("/api/auth/refresh")
                        .with(remoteAddress("10.0.0.99"))
                        .header("User-Agent", DEFAULT_USER_AGENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userInfo.username").value("admin"));
    }

    @Test
    void shouldRevokeExistingAccessTokenWhenPasswordResetCompletes() throws Exception {
        UserEntity teacher = userMapper.selectByUsernameOrEmail("teacher.zhang");
        String adminToken = loginAndGetAccessToken("admin", "Admin@123456");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usernameOrEmail": "teacher.zhang",
                                  "password": "Teacher@123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String oldAccessToken = loginJson.path("data").path("accessToken").asText();
        String oldRefreshToken = loginJson.path("data").path("refreshToken").asText();

        MvcResult resetLinkResult = mockMvc.perform(post("/api/admin/users/{userId}/password-reset-link", teacher.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String resetToken = extractAccountActionToken(objectMapper.readTree(resetLinkResult.getResponse().getContentAsString())
                .path("data")
                .path("linkUrl")
                .asText());
        String newPassword = "TeacherReset@123456";

        mockMvc.perform(post("/api/auth/account-actions/{token}/complete", resetToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "%s"
                                }
                                """.formatted(newPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account action completed"));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + oldAccessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(oldRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usernameOrEmail": "teacher.zhang",
                                  "password": "%s"
                                }
                                """.formatted(newPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userInfo.username").value("teacher.zhang"));
    }

    @Test
    void shouldRejectOversizedLoginRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usernameOrEmail": "admin",
                                  "password": "%s"
                                }
                                """.formatted("x".repeat(129))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("password: password must be at most 128 characters"));
    }

    @Test
    void shouldAllowConfiguredCorsPreflightForAuthLogin() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("Authorization")));
    }

    @Test
    void shouldExposeConfiguredCorsHeadersOnApiResponse() throws Exception {
        mockMvc.perform(get("/api/health")
                        .header("Origin", "http://127.0.0.1:3000"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:3000"))
                .andExpect(header().string("Access-Control-Expose-Headers", containsString("X-Trace-Id")));
    }

    @Test
    void shouldRejectUnconfiguredCorsOrigin() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "https://malicious.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    private String loginAndGetRefreshToken() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usernameOrEmail": "admin",
                                  "password": "Admin@123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return loginJson.path("data").path("refreshToken").asText();
    }

    private String loginAndGetAccessToken(String usernameOrEmail, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usernameOrEmail": "%s",
                                  "password": "%s"
                                }
                                """.formatted(usernameOrEmail, password)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return loginJson.path("data").path("accessToken").asText();
    }

    private String refreshAndGetRotatedRefreshToken(String refreshToken) throws Exception {
        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode refreshJson = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        return refreshJson.path("data").path("refreshToken").asText();
    }

    private RequestPostProcessor remoteAddress(String remoteAddress) {
        return request -> {
            request.setRemoteAddr(remoteAddress);
            return request;
        };
    }

    private String extractAccountActionToken(String linkUrl) {
        int separatorIndex = linkUrl.lastIndexOf('/');
        return separatorIndex >= 0 ? linkUrl.substring(separatorIndex + 1) : linkUrl;
    }
}
