package com.huashi.eftransfer.app.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties(AiGatewayClientProperties.class)
public class AiGatewayClientConfig {

    @Bean
    public RestClient aiGatewayRestClient(RestClient.Builder builder, AiGatewayClientProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.getReadTimeout());

        return builder
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(requestFactory)
                .build();
    }
}
