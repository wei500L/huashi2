package com.huashi.eftransfer.ai.common.exception;

import com.huashi.eftransfer.shared.api.ApiResponse;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(exception.getResultCode(), exception.getMessage(), traceId()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(ResultCode.VALIDATION_ERROR, message, traceId()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(ResultCode.VALIDATION_ERROR, exception.getMessage(), traceId()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(
                        ResultCode.INTERNAL_ERROR,
                        "Unexpected error while handling request " + request.getRequestURI(),
                        traceId()
                ));
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }

    private String traceId() {
        return MDC.get("traceId");
    }
}
