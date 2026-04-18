package com.huashi.eftransfer.ai.common.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huashi.eftransfer.ai.common.observability.SensitiveDataRedactor;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.TimeoutException;

import static com.huashi.eftransfer.shared.api.ResultCode.AI_PROVIDER_UNAVAILABLE;
import static com.huashi.eftransfer.shared.api.ResultCode.BAD_REQUEST;
import static com.huashi.eftransfer.shared.api.ResultCode.RATE_LIMITED;

@Component
public class ProviderErrorSupport {

    private static final Pattern STATUS_MESSAGE_PATTERN = Pattern.compile("^(\\d{3})\\s*-\\s*(.*)$", Pattern.DOTALL);

    private final ObjectMapper objectMapper;
    private final SensitiveDataRedactor sensitiveDataRedactor;

    public ProviderErrorSupport(ObjectMapper objectMapper, SensitiveDataRedactor sensitiveDataRedactor) {
        this.objectMapper = objectMapper;
        this.sensitiveDataRedactor = sensitiveDataRedactor;
    }

    public ProviderCallException map(
            Throwable throwable,
            String operation,
            String provider,
            String model,
            String fallbackRequestId
    ) {
        Throwable cause = unwrap(throwable);

        if (cause instanceof ProviderCallException providerCallException) {
            return providerCallException;
        }

        if (cause instanceof CallNotPermittedException ex) {
            return new ProviderCallException(
                    AI_PROVIDER_UNAVAILABLE,
                    "Provider circuit breaker is open for " + operation,
                    503,
                    operation,
                    provider,
                    model,
                    fallbackRequestId,
                    null,
                    null,
                    false,
                    "circuit_open",
                    ex
            );
        }

        if (isTimeout(cause)) {
            return new ProviderCallException(
                    AI_PROVIDER_UNAVAILABLE,
                    "Provider call timed out for " + operation,
                    504,
                    operation,
                    provider,
                    model,
                    fallbackRequestId,
                    null,
                    null,
                    true,
                    "timeout",
                    cause
            );
        }

        if (cause instanceof RestClientResponseException ex) {
            return mapHttpStatus(
                    ex.getStatusCode().value(),
                    sensitiveDataRedactor.redact(ex.getResponseBodyAsString()),
                    operation,
                    provider,
                    model,
                    fallbackRequestId,
                    ex
            );
        }

        ProviderCallException statusMessageMapping = mapStatusMessage(cause, operation, provider, model, fallbackRequestId);
        if (statusMessageMapping != null) {
            return statusMessageMapping;
        }

        return new ProviderCallException(
                AI_PROVIDER_UNAVAILABLE,
                "Provider call failed for " + operation + ": " + sensitiveDataRedactor.redact(cause.getMessage()),
                503,
                operation,
                provider,
                model,
                fallbackRequestId,
                null,
                null,
                isRetryable(cause),
                "provider_error",
                cause
        );
    }

    public boolean isRetryable(Throwable throwable) {
        Throwable cause = unwrap(throwable);
        if (cause instanceof ProviderCallException ex) {
            return ex.isRetryable();
        }
        if (cause instanceof CallNotPermittedException) {
            return false;
        }
        if (isTimeout(cause)) {
            return true;
        }
        if (cause instanceof RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            return status == 429 || status >= 500;
        }
        return cause instanceof ResourceAccessException;
    }

    public boolean shouldRecordForCircuitBreaker(Throwable throwable) {
        Throwable cause = unwrap(throwable);
        if (cause instanceof ProviderCallException ex) {
            return !"bad_request".equals(ex.getOutcome());
        }
        if (cause instanceof RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            return status != 400 && status != 422;
        }
        return !(cause instanceof IllegalArgumentException);
    }

    private boolean isTimeout(Throwable throwable) {
        return throwable instanceof HttpTimeoutException
                || throwable instanceof SocketTimeoutException
                || throwable instanceof TimeoutException
                || throwable instanceof ResourceAccessException
                || throwable instanceof ConnectException
                || throwable instanceof UnknownHostException;
    }

    private Throwable unwrap(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor.getCause() != null && cursor.getCause() != cursor) {
            if (cursor instanceof RestClientResponseException
                    || cursor instanceof ResourceAccessException
                    || cursor instanceof CallNotPermittedException
                    || cursor instanceof ProviderCallException) {
                return cursor;
            }
            cursor = cursor.getCause();
        }
        return cursor;
    }

    private String buildMessage(String providerMessage, HttpStatusCode status, String provider) {
        if (providerMessage != null && !providerMessage.isBlank()) {
            return providerMessage;
        }
        return "Provider " + provider + " returned HTTP " + status.value();
    }

    private ProviderCallException mapStatusMessage(
            Throwable throwable,
            String operation,
            String provider,
            String model,
            String fallbackRequestId
    ) {
        String message = throwable.getMessage();
        if (message == null) {
            return null;
        }

        Matcher matcher = STATUS_MESSAGE_PATTERN.matcher(sensitiveDataRedactor.redact(message).trim());
        if (!matcher.matches()) {
            return null;
        }

        int status = Integer.parseInt(matcher.group(1));
        String body = matcher.group(2);
        return mapHttpStatus(status, body, operation, provider, model, fallbackRequestId, throwable);
    }

    private ProviderCallException mapHttpStatus(
            int status,
            String body,
            String operation,
            String provider,
            String model,
            String fallbackRequestId,
            Throwable cause
    ) {
        ProviderErrorBody errorBody = parse(body, fallbackRequestId);
        if (status == 400 || status == 422) {
            return new ProviderCallException(
                    BAD_REQUEST,
                    buildMessage(errorBody.message(), HttpStatusCode.valueOf(status), provider),
                    400,
                    operation,
                    provider,
                    model,
                    errorBody.requestId(),
                    status,
                    errorBody.code(),
                    false,
                    "bad_request",
                    cause
            );
        }
        if (status == 429) {
            return new ProviderCallException(
                    RATE_LIMITED,
                    buildMessage(errorBody.message(), HttpStatusCode.valueOf(status), provider),
                    429,
                    operation,
                    provider,
                    model,
                    errorBody.requestId(),
                    status,
                    errorBody.code(),
                    true,
                    "rate_limited",
                    cause
            );
        }
        return new ProviderCallException(
                AI_PROVIDER_UNAVAILABLE,
                buildMessage(errorBody.message(), HttpStatusCode.valueOf(status), provider),
                status == 401 || status == 403 ? 502 : 503,
                operation,
                provider,
                model,
                errorBody.requestId(),
                status,
                errorBody.code(),
                status >= 500,
                "provider_error",
                cause
        );
    }

    private ProviderErrorBody parse(String body, String fallbackRequestId) {
        if (body == null || body.isBlank()) {
            return new ProviderErrorBody(null, null, fallbackRequestId);
        }

        try {
            JsonNode node = objectMapper.readTree(body);
            String requestId = text(node, "request_id");
            if (requestId == null) {
                requestId = text(node, "requestId");
            }

            JsonNode errorNode = node.path("error");
            if (!errorNode.isMissingNode()) {
                return new ProviderErrorBody(
                        text(errorNode, "code"),
                        text(errorNode, "message"),
                        requestId != null ? requestId : fallbackRequestId
                );
            }

            return new ProviderErrorBody(
                    text(node, "code"),
                    text(node, "message"),
                    requestId != null ? requestId : fallbackRequestId
            );
        } catch (Exception ex) {
            return new ProviderErrorBody(null, sensitiveDataRedactor.redact(body), fallbackRequestId);
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private record ProviderErrorBody(String code, String message, String requestId) {
    }
}
