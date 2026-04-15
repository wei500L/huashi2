package com.huashi.eftransfer.app.modules.user;

import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
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
                .andExpect(jsonPath("$.data.user.username").value("ops.viewer"))
                .andExpect(jsonPath("$.data.user.enabled").value(true))
                .andExpect(jsonPath("$.data.user.roles", hasItem("STUDENT")))
                .andExpect(jsonPath("$.data.user.roles", hasItem("TEACHER")))
                .andReturn();
        long userId = readJson(createResult).path("data").path("user").path("id").asLong();

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

    @Test
    void shouldBatchCreateUsersAndBulkUpdateAccess() throws Exception {
        String adminToken = loginAndGetAccessToken("admin", "Admin@123456");

        MvcResult createResult = mockMvc.perform(post("/api/admin/users/batch")
                        .with(bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operation": "IMPORT_CREATE",
                                  "createItems": [
                                    {
                                      "rowNumber": 2,
                                      "username": "bulk.student",
                                      "email": "bulk.student@example.com",
                                      "displayName": "Bulk Student",
                                      "enabled": true,
                                      "roles": ["STUDENT"]
                                    },
                                    {
                                      "rowNumber": 3,
                                      "username": "bulk.teacher",
                                      "email": "bulk.teacher@example.com",
                                      "displayName": "Bulk Teacher",
                                      "credentialMode": "MANUAL_PASSWORD",
                                      "initialPassword": "Teacher@123456",
                                      "enabled": true,
                                      "roles": ["TEACHER"]
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.operation").value("IMPORT_CREATE"))
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.createdUsers", hasSize(2)))
                .andExpect(jsonPath("$.data.createdUsers[0].user.username").value("bulk.student"))
                .andExpect(jsonPath("$.data.createdUsers[0].accountAction.linkUrl").exists())
                .andExpect(jsonPath("$.data.createdUsers[1].user.username").value("bulk.teacher"))
                .andReturn();

        long firstCreatedUserId = readJson(createResult).path("data").path("createdUsers").path(0).path("user").path("id").asLong();
        long secondCreatedUserId = readJson(createResult).path("data").path("createdUsers").path(1).path("user").path("id").asLong();

        MvcResult updateResult = mockMvc.perform(post("/api/admin/users/batch")
                        .with(bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operation": "BULK_ACCESS_UPDATE",
                                  "userIds": [%d, %d],
                                  "enabled": false,
                                  "roles": ["TEACHER", "ADMIN"]
                                }
                                """.formatted(firstCreatedUserId, secondCreatedUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.operation").value("BULK_ACCESS_UPDATE"))
                .andExpect(jsonPath("$.data.updatedUsers", hasSize(2)))
                .andExpect(jsonPath("$.data.updatedUsers[0].enabled").value(false))
                .andExpect(jsonPath("$.data.updatedUsers[0].roles", hasItem("TEACHER")))
                .andExpect(jsonPath("$.data.updatedUsers[0].roles", hasItem("ADMIN")))
                .andReturn();

        long firstUpdatedUserId = readJson(updateResult).path("data").path("updatedUsers").path(0).path("id").asLong();
        long secondUpdatedUserId = readJson(updateResult).path("data").path("updatedUsers").path(1).path("id").asLong();

        mockMvc.perform(put("/api/admin/users/{userId}/access", firstUpdatedUserId)
                        .with(bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": true,
                                  "roles": ["STUDENT"]
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/admin/users/{userId}/access", secondUpdatedUserId)
                        .with(bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": true,
                                  "roles": ["TEACHER"]
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectInvalidBatchPayloads() throws Exception {
        String adminToken = loginAndGetAccessToken("admin", "Admin@123456");

        mockMvc.perform(post("/api/admin/users/batch")
                        .with(bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operation": "IMPORT_CREATE",
                                  "createItems": [
                                    {
                                      "rowNumber": 2,
                                      "username": "duplicate.batch",
                                      "email": "duplicate.batch@example.com",
                                      "displayName": "Duplicate Batch",
                                      "enabled": true,
                                      "roles": ["STUDENT"]
                                    },
                                    {
                                      "rowNumber": 3,
                                      "username": "duplicate.batch",
                                      "email": "duplicate.batch+2@example.com",
                                      "displayName": "Duplicate Batch Two",
                                      "enabled": true,
                                      "roles": ["STUDENT"]
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/admin/users/batch")
                        .with(bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operation": "IMPORT_CREATE",
                                  "createItems": [null]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/admin/users/batch")
                        .with(bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operation": "BULK_ACCESS_UPDATE",
                                  "userIds": [],
                                  "enabled": true,
                                  "roles": ["TEACHER"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
