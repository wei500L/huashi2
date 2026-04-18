package com.huashi.eftransfer.app.common.outbox;

import java.time.OffsetDateTime;

public interface PlatformEventOutboxRelayHandler {

    boolean supports(PlatformEventOutboxRecord record);

    String relay(PlatformEventOutboxRecord record) throws Exception;

    default boolean isRetryableFailure(Exception exception) {
        return !(exception instanceof NonRetryableOutboxException);
    }

    default void afterRetryScheduled(
            PlatformEventOutboxRecord record,
            int nextAttemptCount,
            OffsetDateTime nextAttemptAt,
            Exception exception
    ) {
    }

    default void afterMovedToDlq(
            PlatformEventOutboxRecord record,
            int finalAttemptCount,
            Exception exception
    ) {
    }

    default void afterPublished(
            PlatformEventOutboxRecord record,
            boolean recoveredFromFailure,
            String detail
    ) {
    }
}
