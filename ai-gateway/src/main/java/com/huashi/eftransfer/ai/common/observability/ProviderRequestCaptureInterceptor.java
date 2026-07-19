package com.huashi.eftransfer.ai.common.observability;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.List;

public class ProviderRequestCaptureInterceptor implements ClientHttpRequestInterceptor {

    private static final List<String> CANDIDATE_HEADERS = List.of(
            "x-request-id",
            "x-dashscope-request-id",
            "x-siliconcloud-trace-id",
            "request-id"
    );

    private final ProviderRequestContextHolder contextHolder;

    public ProviderRequestCaptureInterceptor(ProviderRequestContextHolder contextHolder) {
        this.contextHolder = contextHolder;
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {
        ClientHttpResponse response = execution.execute(request, body);
        String requestId = extractRequestId(response.getHeaders());
        if (requestId != null && !requestId.isBlank()) {
            contextHolder.setRequestId(requestId);
        }
        return response;
    }

    private String extractRequestId(HttpHeaders headers) {
        for (String name : CANDIDATE_HEADERS) {
            String value = headers.getFirst(name);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
