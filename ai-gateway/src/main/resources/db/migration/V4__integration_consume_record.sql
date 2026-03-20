CREATE TABLE IF NOT EXISTS integration_consume_record
(
    id            BIGSERIAL PRIMARY KEY,
    consumer_name VARCHAR(128) NOT NULL,
    event_id      VARCHAR(128) NOT NULL,
    event_type    VARCHAR(128) NOT NULL,
    status        VARCHAR(32)  NOT NULL,
    error_message TEXT         NULL,
    processed_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_integration_consume_record UNIQUE (consumer_name, event_id)
);

CREATE INDEX IF NOT EXISTS idx_integration_consume_record_status
    ON integration_consume_record (consumer_name, status, processed_at DESC);
