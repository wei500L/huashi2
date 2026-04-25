package com.huashi.eftransfer.app.common.exception;

import com.huashi.eftransfer.app.common.trace.TraceIdSupport;
import com.huashi.eftransfer.shared.api.ApiResponse;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception, HttpServletRequest request) {
        return ResponseEntity.status(exception.getHttpStatus())
                .body(ApiResponse.failure(exception.getResultCode(), exception.getMessage(), traceId(request)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception, HttpServletRequest request) {
        String message = formatFieldErrors(exception.getBindingResult().getFieldErrors());
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(ResultCode.VALIDATION_ERROR, message, traceId(request)));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException exception, HttpServletRequest request) {
        String message = formatFieldErrors(exception.getBindingResult().getFieldErrors());
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(ResultCode.VALIDATION_ERROR, message, traceId(request)));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(ResultCode.VALIDATION_ERROR, exception.getMessage(), traceId(request)));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException exception, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure(ResultCode.BAD_REQUEST, "Import file must not exceed 50MB", traceId(request)));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.failure(ResultCode.UNAUTHORIZED, exception.getMessage(), traceId(request)));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failure(ResultCode.FORBIDDEN, exception.getMessage(), traceId(request)));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(ResultCode.NOT_FOUND, "Requested resource was not found", traceId(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception, HttpServletRequest request) {
        log.error("event=unexpected_request_error method={} path={} message={}",
                request.getMethod(),
                request.getRequestURI(),
                exception.getMessage(),
                exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(
                        ResultCode.INTERNAL_ERROR,
                        "Unexpected error while handling request",
                        traceId(request)
                    ));
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }

    private String formatFieldErrors(java.util.List<FieldError> fieldErrors) {
        return fieldErrors.stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
    }

    private String traceId(HttpServletRequest request) {
        return TraceIdSupport.currentOrResolve(request);
    }
}
