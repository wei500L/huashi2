package com.huashi.eftransfer.app.common.idempotency;

import com.huashi.eftransfer.app.common.util.TokenGenerator;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

@Service
public class IdempotencyService {

    private static final Duration DEFAULT_TTL = Duration.ofHours(24);
    private static final Duration REPLAY_WAIT_TIMEOUT = Duration.ofSeconds(1);
    private static final Duration REPLAY_WAIT_INTERVAL = Duration.ofMillis(25);

    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyRecordRepository idempotencyRecordRepository, ObjectMapper objectMapper) {
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.objectMapper = objectMapper;
    }

    public String hashPayload(Object payload) {
        try {
            return TokenGenerator.sha256(objectMapper.writeValueAsString(payload));
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to serialize idempotency payload", exception);
        }
    }

    public <T> IdempotencyClaimResult<T> claimOrReplay(
            String requestKey,
            String requestHash,
            Long actorUserId,
            Class<T> responseType
    ) {
        IdempotencyRecord existing = idempotencyRecordRepository.findByRequestKey(requestKey);
        if (existing != null) {
            return IdempotencyClaimResult.replayed(resolveExisting(existing, requestHash, responseType));
        }

        HttpServletRequest request = currentRequest();
        try {
            idempotencyRecordRepository.insertProcessing(
                    requestKey,
                    request == null ? "N/A" : request.getRequestURI(),
                    request == null ? "N/A" : request.getMethod(),
                    requestHash,
                    OffsetDateTime.now(ZoneOffset.UTC).plus(DEFAULT_TTL),
                    actorUserId
            );
            return IdempotencyClaimResult.claimed(requestKey);
        } catch (DataIntegrityViolationException exception) {
            IdempotencyRecord duplicated = idempotencyRecordRepository.findByRequestKey(requestKey);
            if (duplicated == null) {
                throw exception;
            }
            return IdempotencyClaimResult.replayed(resolveExisting(duplicated, requestHash, responseType));
        }
    }

    public <T> void complete(String requestKey, T response, Long actorUserId) {
        try {
            idempotencyRecordRepository.markCompleted(
                    requestKey,
                    ResultCode.SUCCESS.code(),
                    objectMapper.writeValueAsString(response),
                    actorUserId
            );
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to serialize idempotent response", exception);
        }
    }

    public void release(String requestKey) {
        idempotencyRecordRepository.deleteByRequestKey(requestKey);
    }

    private <T> T resolveExisting(IdempotencyRecord existing, String requestHash, Class<T> responseType) {
        if (!Objects.equals(existing.requestHash(), requestHash)) {
            throw new BusinessException(ResultCode.CONFLICT, "clientRequestId already exists with a different answer payload", 409);
        }
        if (hasText(existing.responseBody())) {
            return readResponse(existing.responseBody(), responseType);
        }

        OffsetDateTime deadline = OffsetDateTime.now(ZoneOffset.UTC).plus(REPLAY_WAIT_TIMEOUT);
        while (OffsetDateTime.now(ZoneOffset.UTC).isBefore(deadline)) {
            sleep(REPLAY_WAIT_INTERVAL);
            IdempotencyRecord refreshed = idempotencyRecordRepository.findByRequestKey(existing.requestKey());
            if (refreshed != null && hasText(refreshed.responseBody())) {
                return readResponse(refreshed.responseBody(), responseType);
            }
        }

        throw new BusinessException(
                ResultCode.CONFLICT,
                "Answer request is already being processed. Retry with the same clientRequestId shortly.",
                409
        );
    }

    private <T> T readResponse(String responseBody, Class<T> responseType) {
        try {
            return objectMapper.readValue(responseBody, responseType);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to deserialize idempotent response", exception);
        }
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for idempotent response", exception);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record IdempotencyClaimResult<T>(String requestKey, boolean claimed, T replayedResponse) {

        public static <T> IdempotencyClaimResult<T> claimed(String requestKey) {
            return new IdempotencyClaimResult<>(requestKey, true, null);
        }

        public static <T> IdempotencyClaimResult<T> replayed(T response) {
            return new IdempotencyClaimResult<>(null, false, response);
        }
    }
}
