package com.huashi.eftransfer.app.integration.ai.client;

public enum AiGatewayFailureReason {
    TIMEOUT,
    HTTP_ERROR,
    PROVIDER_UNAVAILABLE,
    INVALID_RESPONSE,
    INVALID_JSON,
    SCHEMA_VALIDATION_FAILED,
    RAG_UNAVAILABLE,
    UNKNOWN
}
