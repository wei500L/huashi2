package com.huashi.eftransfer.shared.exception;

import com.huashi.eftransfer.shared.api.ResultCode;

public class BusinessException extends RuntimeException {

    private final ResultCode resultCode;
    private final int httpStatus;

    public BusinessException(ResultCode resultCode, String message) {
        this(resultCode, message, 400);
    }

    public BusinessException(ResultCode resultCode, String message, int httpStatus) {
        super(message);
        this.resultCode = resultCode;
        this.httpStatus = httpStatus;
    }

    public ResultCode getResultCode() {
        return resultCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
