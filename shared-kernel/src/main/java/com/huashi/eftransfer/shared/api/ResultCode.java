package com.huashi.eftransfer.shared.api;

public enum ResultCode {
    SUCCESS("SUCCESS", "Request succeeded"),
    BAD_REQUEST("BAD_REQUEST", "Invalid request payload"),
    UNAUTHORIZED("UNAUTHORIZED", "Authentication required"),
    FORBIDDEN("FORBIDDEN", "Access denied"),
    NOT_FOUND("NOT_FOUND", "Requested resource was not found"),
    VALIDATION_ERROR("VALIDATION_ERROR", "Validation failed"),
    CONFLICT("CONFLICT", "Resource conflict"),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "Invalid username/email or password"),
    CURRENT_PASSWORD_INCORRECT("CURRENT_PASSWORD_INCORRECT", "Current password is incorrect"),
    ACCOUNT_DISABLED("ACCOUNT_DISABLED", "Account is disabled"),
    ACCOUNT_LOCKED("ACCOUNT_LOCKED", "Account is temporarily locked"),
    TOKEN_INVALID("TOKEN_INVALID", "Token is invalid"),
    TOKEN_EXPIRED("TOKEN_EXPIRED", "Token has expired"),
    REGISTRATION_CONTEXT_INVALID("REGISTRATION_CONTEXT_INVALID", "Registration context is invalid or expired"),
    REGISTRATION_CONTEXT_BUSY("REGISTRATION_CONTEXT_BUSY", "Registration context is already in use"),
    ACTIVE_SESSION_EXISTS("ACTIVE_SESSION_EXISTS", "An active session already exists"),
    ASSESSMENT_NOT_STARTED("ASSESSMENT_NOT_STARTED", "Assessment has not started"),
    ASSESSMENT_CLOSED("ASSESSMENT_CLOSED", "Assessment is closed"),
    ATTEMPT_SUBMITTED("ATTEMPT_SUBMITTED", "Assessment attempt has been submitted"),
    RESULT_NOT_RELEASED("RESULT_NOT_RELEASED", "Assessment result has not been released"),
    VERSION_CONFLICT("VERSION_CONFLICT", "Resource version conflict"),
    SESSION_OUT_OF_SEQUENCE("SESSION_OUT_OF_SEQUENCE", "Session answer is out of sequence"),
    ANSWER_ALREADY_ACCEPTED("ANSWER_ALREADY_ACCEPTED", "Answer has already been accepted"),
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
