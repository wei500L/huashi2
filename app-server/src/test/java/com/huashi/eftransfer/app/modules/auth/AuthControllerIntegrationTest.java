package com.huashi.eftransfer.app.modules.auth;

import com.huashi.eftransfer.app.modules.user.entity.UserEntity;
import com.huashi.eftransfer.app.modules.user.entity.UserRoleEntity;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestAuthTokenStoreConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

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
}
