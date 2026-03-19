package com.huashi.eftransfer.shared.exception;

import com.huashi.eftransfer.shared.api.ResultCode;

public class BusinessException extends RuntimeException {

    private final ResultCode resultCode;

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    public ResultCode getResultCode() {
        return resultCode;
    }
}
