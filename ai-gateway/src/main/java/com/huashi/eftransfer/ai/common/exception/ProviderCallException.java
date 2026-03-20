package com.huashi.eftransfer.ai.common.exception;

import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;

public class ProviderCallException extends BusinessException {

    private final String operation;
    private final String provider;
    private final String model;
    private final String providerRequestId;
    private final Integer providerStatus;
    private final String providerCode;
    private final boolean retryable;
    private final String outcome;

    public ProviderCallException(
            ResultCode resultCode,
            String message,
            int httpStatus,
            String operation,
            String provider,
            String model,
            String providerRequestId,
            Integer providerStatus,
            String providerCode,
            boolean retryable,
            String outcome,
            Throwable cause
    ) {
        super(resultCode, message, httpStatus);
        this.operation = operation;
        this.provider = provider;
        this.model = model;
        this.providerRequestId = providerRequestId;
        this.providerStatus = providerStatus;
        this.providerCode = providerCode;
        this.retryable = retryable;
        this.outcome = outcome;
        if (cause != null) {
            initCause(cause);
        }
    }

    public String getOperation() {
        return operation;
    }

    public String getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    public String getProviderRequestId() {
        return providerRequestId;
    }

    public Integer getProviderStatus() {
        return providerStatus;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public String getOutcome() {
        return outcome;
    }
}
