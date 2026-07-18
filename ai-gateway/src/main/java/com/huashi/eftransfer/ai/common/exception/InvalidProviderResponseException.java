package com.huashi.eftransfer.ai.common.exception;

public class InvalidProviderResponseException extends RuntimeException {

    public InvalidProviderResponseException(String message) {
        super(message);
    }

    public InvalidProviderResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
