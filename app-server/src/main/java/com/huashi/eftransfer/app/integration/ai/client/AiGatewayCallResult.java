package com.huashi.eftransfer.app.integration.ai.client;

public record AiGatewayCallResult<T>(
        boolean success,
        T data,
        AiGatewayFailureReason failureReason,
        String failureMessage,
        int attempts,
        long latencyMs,
        String endpoint
) {

    public static <T> AiGatewayCallResult<T> success(T data, int attempts, long latencyMs, String endpoint) {
        return new AiGatewayCallResult<>(true, data, null, null, attempts, latencyMs, endpoint);
    }

    public static <T> AiGatewayCallResult<T> failure(
            AiGatewayFailureReason failureReason,
            String failureMessage,
            int attempts,
            long latencyMs,
            String endpoint
    ) {
        return new AiGatewayCallResult<>(false, null, failureReason, failureMessage, attempts, latencyMs, endpoint);
    }
}
