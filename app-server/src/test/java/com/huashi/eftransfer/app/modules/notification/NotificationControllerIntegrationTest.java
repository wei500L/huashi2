package com.huashi.eftransfer.app.modules.notification;

import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationControllerIntegrationTest extends AbstractWebIntegrationTest {

    @Test
    void shouldRejectInvalidPagingParameters() throws Exception {
        String studentToken = loginAndGetAccessToken("student.li", "Student@123456");

        mockMvc.perform(get("/api/notifications")
                        .with(bearer(studentToken))
                        .param("pageSize", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("pageSize")));

        mockMvc.perform(get("/api/notifications")
                        .with(bearer(studentToken))
                        .param("pageSize", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("pageSize")));
    }
}
