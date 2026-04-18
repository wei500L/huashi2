package com.huashi.eftransfer.ai.modules.rag.config;

import com.huashi.eftransfer.ai.modules.rag.service.KnowledgeSearchService;
import com.huashi.eftransfer.ai.modules.rag.service.RagRetrievalCapture;
import com.huashi.eftransfer.ai.modules.rag.support.RagSearchFilter;
import com.huashi.eftransfer.ai.modules.rag.vector.RagAdvisorVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableConfigurationProperties(RagProperties.class)
public class RagConfiguration {

    @Bean
    public TaskExecutor ragTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("rag-index-");
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(64);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Bean
    @Primary
    public VectorStore ragHealthVectorStore(
            KnowledgeSearchService knowledgeSearchService,
            RagRetrievalCapture ragRetrievalCapture
    ) {
        return new RagAdvisorVectorStore(
                knowledgeSearchService,
                ragRetrievalCapture,
                RagSearchFilter.empty()
        );
    }
}
