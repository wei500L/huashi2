package com.huashi.eftransfer.ai.modules.rag.config;

import com.huashi.eftransfer.ai.modules.rag.service.KnowledgeSearchService;
import com.huashi.eftransfer.ai.modules.rag.service.RagRetrievalCapture;
import com.huashi.eftransfer.ai.modules.rag.support.RagSearchFilter;
import com.huashi.eftransfer.ai.modules.rag.vector.RagAdvisorVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties(RagProperties.class)
public class RagConfiguration {

    @Bean
    @Qualifier("knowledgeSourceRestClient")
    public RestClient knowledgeSourceRestClient(RestClient.Builder builder, RagProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getAppServer().getConnectTimeout())
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.getAppServer().getReadTimeout());

        return builder.clone()
                .baseUrl(properties.getAppServer().getBaseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(requestFactory)
                .build();
    }

    @Bean
    public TaskExecutor ragTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("rag-index-");
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(8);
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
