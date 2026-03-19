package com.huashi.eftransfer.shared.api;

public enum ResultCode {
    SUCCESS("SUCCESS", "Request succeeded"),
    BAD_REQUEST("BAD_REQUEST", "Invalid request payload"),
    UNAUTHORIZED("UNAUTHORIZED", "Authentication required"),
    FORBIDDEN("FORBIDDEN", "Access denied"),
    NOT_FOUND("NOT_FOUND", "Requested resource was not found"),
    VALIDATION_ERROR("VALIDATION_ERROR", "Validation failed"),
    CONFLICT("CONFLICT", "Resource conflict"),
    RATE_LIMITED("RATE_LIMITED", "Too many requests"),
    AI_PROVIDER_UNAVAILABLE("AI_PROVIDER_UNAVAILABLE", "AI provider unavailable"),
    INTERNAL_ERROR("INTERNAL_ERROR", "Internal server error");

    private final String code;
    private final String message;

    ResultCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}
