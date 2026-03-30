package com.huashi.eftransfer.ai.common.config;

import com.huashi.eftransfer.ai.common.observability.ProviderRequestCaptureInterceptor;
import com.huashi.eftransfer.ai.common.observability.ProviderRequestContextHolder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({AiProviderProperties.class, AiResilienceProperties.class})
public class AiProviderConfiguration {

    @Bean
    @ConditionalOnMissingBean(RestClient.Builder.class)
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public ProviderRequestContextHolder providerRequestContextHolder() {
        return new ProviderRequestContextHolder();
    }

    @Bean
    public ClientHttpRequestInterceptor providerRequestCaptureInterceptor(ProviderRequestContextHolder contextHolder) {
        return new ProviderRequestCaptureInterceptor(contextHolder);
    }
}
