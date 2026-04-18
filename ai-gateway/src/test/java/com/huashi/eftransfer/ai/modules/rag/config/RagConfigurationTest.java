package com.huashi.eftransfer.ai.modules.rag.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;

class RagConfigurationTest {

    @Test
    void shouldConfigureRagTaskExecutorForBurstToleranceAndGracefulShutdown() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) new RagConfiguration().ragTaskExecutor();

        assertThat(executor.getCorePoolSize()).isEqualTo(1);
        assertThat(executor.getMaxPoolSize()).isEqualTo(2);
        assertThat(executor.getQueueCapacity()).isEqualTo(64);
        assertThat(executor.getThreadPoolExecutor().getRejectedExecutionHandler()).isInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class);

        executor.shutdown();
    }
}
