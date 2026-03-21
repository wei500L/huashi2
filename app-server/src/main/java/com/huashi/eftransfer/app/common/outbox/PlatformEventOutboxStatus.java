package com.huashi.eftransfer.app.common.outbox;

public enum PlatformEventOutboxStatus {
    PENDING,
    IN_PROGRESS,
    FAILED,
    PUBLISHED
}
