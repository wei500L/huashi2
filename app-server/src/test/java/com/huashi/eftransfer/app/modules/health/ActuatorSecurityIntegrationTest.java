package com.huashi.eftransfer.app.modules.health;

import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ActuatorSecurityIntegrationTest extends AbstractWebIntegrationTest {

    @Test
    void shouldExposeHealthWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRequireAuthenticationForActuatorInfo() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldForbidStudentFromReadingActuatorMetrics() throws Exception {
        String studentToken = loginAndGetAccessToken("student.li", "Student@123456");

        mockMvc.perform(get("/actuator/prometheus").with(bearer(studentToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/actuator/metrics").with(bearer(studentToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/actuator/info").with(bearer(studentToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdminToReadActuatorMetrics() throws Exception {
        String adminToken = loginAndGetAccessToken("admin", "Admin@123456");

        mockMvc.perform(get("/actuator/prometheus").with(bearer(adminToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/actuator/metrics").with(bearer(adminToken)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldHideEnvironmentInfoFromAuthenticatedActuatorInfoResponse() throws Exception {
        String adminToken = loginAndGetAccessToken("admin", "Admin@123456");

        MvcResult result = mockMvc.perform(get("/actuator/info").with(bearer(adminToken)))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("env");
    }
}
