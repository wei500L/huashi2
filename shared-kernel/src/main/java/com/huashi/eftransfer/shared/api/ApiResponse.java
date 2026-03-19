package com.huashi.eftransfer.shared.api;

import java.time.OffsetDateTime;

public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        OffsetDateTime timestamp,
        String traceId
) {

    public static <T> ApiResponse<T> success(T data, String traceId) {
        return new ApiResponse<>(
                true,
                ResultCode.SUCCESS.code(),
                ResultCode.SUCCESS.message(),
                data,
                OffsetDateTime.now(),
                traceId
        );
    }

    public static ApiResponse<Void> success(String message, String traceId) {
        return new ApiResponse<>(
                true,
                ResultCode.SUCCESS.code(),
                message,
                null,
                OffsetDateTime.now(),
                traceId
        );
    }

    public static <T> ApiResponse<T> failure(ResultCode resultCode, String message, String traceId) {
        return new ApiResponse<>(
                false,
                resultCode.code(),
                message,
                null,
                OffsetDateTime.now(),
                traceId
        );
    }
}
