package com.huashi.eftransfer.ai.modules.health;

import com.huashi.eftransfer.ai.modules.health.controller.AiHealthController;
import com.huashi.eftransfer.ai.modules.health.dto.AiHealthPayload;
import com.huashi.eftransfer.ai.modules.health.service.AiHealthService;
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

@WebMvcTest(controllers = AiHealthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(com.huashi.eftransfer.ai.common.exception.GlobalExceptionHandler.class)
class AiHealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiHealthService aiHealthService;

    @Test
    void shouldReturnAiGatewayHealth() throws Exception {
        given(aiHealthService.getHealthPayload()).willReturn(new AiHealthPayload(
                "ai-gateway",
                "UP",
                "qwen",
                "deepseek",
                "qwen-max",
                "text-embedding-v4",
                true,
                true,
                false,
                "0.8.2",
                List.of("test"),
                OffsetDateTime.parse("2026-03-19T09:00:00Z")
        ));

        mockMvc.perform(get("/internal/ai/health").header("X-Trace-Id", "trace-ai-health-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.service").value("ai-gateway"))
                .andExpect(jsonPath("$.data.provider").value("qwen"))
                .andExpect(jsonPath("$.data.databaseReady").value(true));
    }
}
