package com.huashi.eftransfer.app.modules.auth;

import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiDocumentationSecurityIntegrationTest extends AbstractWebIntegrationTest {

    @Test
    void adminCanAccessOpenApiDocumentInNonLocalProfiles() throws Exception {
        String adminToken = loginAndGetAccessToken("admin", "Admin@123456");

        mockMvc.perform(get("/v3/api-docs").with(bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi", startsWith("3.")));
    }

    @Test
    void nonAdminCannotAccessOpenApiDocumentInNonLocalProfiles() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");

        mockMvc.perform(get("/v3/api-docs").with(bearer(teacherToken)))
                .andExpect(status().isForbidden());
    }
}
