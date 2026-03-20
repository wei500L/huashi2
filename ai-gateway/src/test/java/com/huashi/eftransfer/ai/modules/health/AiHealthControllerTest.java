package com.huashi.eftransfer.ai.modules.health;

import com.huashi.eftransfer.ai.modules.health.controller.AiHealthController;
import com.huashi.eftransfer.ai.modules.health.dto.AiHealthPayload;
import com.huashi.eftransfer.ai.modules.health.service.AiHealthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiHealthControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AiHealthPayload payload = new AiHealthPayload(
                "ai-gateway",
                "UP",
                "qwen",
                "deepseek",
                "qwen-max",
                "text-embedding-v4",
                "gte-rerank-v2",
                true,
                true,
                false,
                true,
                "0.8.2",
                List.of("test"),
                OffsetDateTime.parse("2026-03-19T09:00:00Z")
        );
        AiHealthService aiHealthService = new AiHealthService(null, null, null, null, null, null, null, null) {
            @Override
            public AiHealthPayload getHealthPayload() {
                return payload;
            }
        };
        mockMvc = MockMvcBuilders.standaloneSetup(new AiHealthController(aiHealthService)).build();
    }

    @Test
    void shouldReturnAiGatewayHealth() throws Exception {
        mockMvc.perform(get("/internal/ai/health").header("X-Trace-Id", "trace-ai-health-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.service").value("ai-gateway"))
                .andExpect(jsonPath("$.data.provider").value("qwen"))
                .andExpect(jsonPath("$.data.databaseReady").value(true));
    }
}
