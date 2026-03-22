package com.huashi.eftransfer.app.modules.user;

import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminUserManagementIntegrationTest extends AbstractWebIntegrationTest {

    @Test
    void shouldCreateUserUpdateAccessAndRejectInvalidPayloads() throws Exception {
        String adminToken = loginAndGetAccessToken("admin", "Admin@123456");

        MvcResult createResult = mockMvc.perform(post("/api/admin/users")
                        .with(bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "ops.viewer",
                                  "email": "ops.viewer@example.com",
                                  "displayName": "Ops Viewer",
                                  "initialPassword": "Viewer@123456",
                                  "enabled": true,
                                  "roles": ["STUDENT", "TEACHER"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("ops.viewer"))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.roles", hasItem("STUDENT")))
                .andExpect(jsonPath("$.data.roles", hasItem("TEACHER")))
                .andReturn();
        long userId = readJson(createResult).path("data").path("id").asLong();

        mockMvc.perform(put("/api/admin/users/{userId}/access", userId)
                        .with(bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": false,
                                  "roles": ["TEACHER"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value((int) userId))
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.roles[0]").value("TEACHER"));

        mockMvc.perform(post("/api/admin/users")
                        .with(bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "ops.viewer",
                                  "email": "ops.viewer+duplicate@example.com",
                                  "displayName": "Ops Viewer Duplicate",
                                  "initialPassword": "Viewer@123456",
                                  "enabled": true,
                                  "roles": ["STUDENT"]
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        mockMvc.perform(put("/api/admin/users/{userId}/access", userId)
                        .with(bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": true,
                                  "roles": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
