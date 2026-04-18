CREATE TABLE IF NOT EXISTS platform_event_outbox_dlq
(
    outbox_id            BIGINT PRIMARY KEY,
    event_id             VARCHAR(128)  NOT NULL,
    event_type           VARCHAR(128)  NOT NULL,
    exchange_name        VARCHAR(128)  NOT NULL,
    routing_key          VARCHAR(255)  NOT NULL,
    payload_json         LONGTEXT      NOT NULL,
    headers_json         LONGTEXT      NULL,
    trace_id             VARCHAR(128)  NULL,
    attempt_count        INT           NOT NULL DEFAULT 0,
    next_attempt_at      DATETIME      NULL,
    last_error           VARCHAR(1000) NULL,
    processing_started_at DATETIME     NULL,
    published_at         DATETIME      NULL,
    created_at           DATETIME      NOT NULL,
    updated_at           DATETIME      NOT NULL,
    exhausted_at         DATETIME      NOT NULL,
    CONSTRAINT uk_platform_event_outbox_dlq_event_id UNIQUE (event_id)
);

CREATE INDEX idx_platform_event_outbox_dlq_event_type
    ON platform_event_outbox_dlq (event_type, updated_at);

CREATE INDEX idx_platform_event_outbox_dlq_exhausted_at
    ON platform_event_outbox_dlq (exhausted_at, outbox_id);
