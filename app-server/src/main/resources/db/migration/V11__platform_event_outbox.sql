CREATE TABLE IF NOT EXISTS platform_event_outbox
(
    id                    BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id              VARCHAR(128)  NOT NULL,
    event_type            VARCHAR(128)  NOT NULL,
    exchange_name         VARCHAR(128)  NOT NULL,
    routing_key           VARCHAR(255)  NOT NULL,
    payload_json          LONGTEXT      NOT NULL,
    headers_json          LONGTEXT      NULL,
    trace_id              VARCHAR(128)  NULL,
    status                VARCHAR(32)   NOT NULL DEFAULT 'PENDING',
    attempt_count         INT           NOT NULL DEFAULT 0,
    next_attempt_at       DATETIME      NULL DEFAULT CURRENT_TIMESTAMP,
    last_error            VARCHAR(1000) NULL,
    processing_started_at DATETIME      NULL,
    published_at          DATETIME      NULL,
    created_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by            BIGINT        NOT NULL DEFAULT 0,
    updated_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by            BIGINT        NOT NULL DEFAULT 0,
    deleted               TINYINT(1)    NOT NULL DEFAULT 0,
    CONSTRAINT uk_platform_event_outbox_event_id UNIQUE (event_id)
);

CREATE INDEX idx_platform_event_outbox_status_due
    ON platform_event_outbox (status, next_attempt_at, id);

CREATE INDEX idx_platform_event_outbox_processing_started
    ON platform_event_outbox (status, processing_started_at, id);

CREATE INDEX idx_platform_event_outbox_type_status
    ON platform_event_outbox (event_type, status, updated_at);
