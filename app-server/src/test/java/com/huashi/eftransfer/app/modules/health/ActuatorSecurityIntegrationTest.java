package com.huashi.eftransfer.app.modules.health;

import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ActuatorSecurityIntegrationTest extends AbstractWebIntegrationTest {

    @Test
    void shouldRequireAuthenticationForActuatorInfo() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isUnauthorized());
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
