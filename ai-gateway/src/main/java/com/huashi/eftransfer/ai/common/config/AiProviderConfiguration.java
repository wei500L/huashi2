package com.huashi.eftransfer.ai.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties(AiProviderProperties.class)
public class AiProviderConfiguration {

    @Bean
    public RestClient rerankRestClient(RestClient.Builder builder, AiProviderProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.getReadTimeout());

        RestClient.Builder clientBuilder = builder
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        if (StringUtils.hasText(properties.getApiKey())) {
            clientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey());
        }
        return clientBuilder.build();
    }
}
