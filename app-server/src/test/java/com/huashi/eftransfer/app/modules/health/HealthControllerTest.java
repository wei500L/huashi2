package com.huashi.eftransfer.app.modules.health;

import com.huashi.eftransfer.app.support.MockMvcTestSupport;
import com.huashi.eftransfer.app.support.TestAuthTokenStoreConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestAuthTokenStoreConfiguration.class)
class HealthControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcTestSupport.build(webApplicationContext);
    }

    @Test
    void shouldReturnHealthPayload() throws Exception {
        mockMvc.perform(get("/api/health").header("X-Trace-Id", "trace-health-test"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", "trace-health-test"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.service").value("app-server"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.aiGatewayBaseUrl").isNotEmpty());
    }
}
