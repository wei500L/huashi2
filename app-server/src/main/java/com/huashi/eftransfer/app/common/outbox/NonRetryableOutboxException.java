package com.huashi.eftransfer.app.common.outbox;

public class NonRetryableOutboxException extends RuntimeException {

    public NonRetryableOutboxException(String message) {
        super(message);
    }

    public NonRetryableOutboxException(String message, Throwable cause) {
        super(message, cause);
    }
}
