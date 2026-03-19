package com.huashi.eftransfer.app.modules.health;

import com.huashi.eftransfer.app.modules.health.controller.HealthController;
import com.huashi.eftransfer.app.modules.health.dto.AppHealthPayload;
import com.huashi.eftransfer.app.modules.health.service.AppHealthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HealthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(com.huashi.eftransfer.app.common.exception.GlobalExceptionHandler.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppHealthService appHealthService;

    @Test
    void shouldReturnHealthPayload() throws Exception {
        given(appHealthService.getHealthPayload()).willReturn(new AppHealthPayload(
                "app-server",
                "UP",
                List.of("test"),
                "http://localhost:8090",
                OffsetDateTime.parse("2026-03-19T09:00:00Z")
        ));

        mockMvc.perform(get("/api/health").header("X-Trace-Id", "trace-health-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.service").value("app-server"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.aiGatewayBaseUrl").value("http://localhost:8090"));
    }
}
