package com.huashi.eftransfer.app.common.idempotency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class IdempotencyCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyCleanupScheduler.class);

    private final IdempotencyRecordRepository idempotencyRecordRepository;

    public IdempotencyCleanupScheduler(IdempotencyRecordRepository idempotencyRecordRepository) {
        this.idempotencyRecordRepository = idempotencyRecordRepository;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpired() {
        int deleted = idempotencyRecordRepository.deleteExpired(OffsetDateTime.now(ZoneOffset.UTC));
        if (deleted > 0) {
            log.info("event=idempotency_record_cleanup deleted={}", deleted);
        }
    }
}
