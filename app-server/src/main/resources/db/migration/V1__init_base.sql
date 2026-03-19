CREATE TABLE IF NOT EXISTS audit_log
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    actor_user_id   BIGINT       NULL,
    action_type     VARCHAR(64)  NOT NULL,
    target_type     VARCHAR(64)  NOT NULL,
    target_id       VARCHAR(128) NULL,
    request_path    VARCHAR(255) NOT NULL,
    request_method  VARCHAR(16)  NOT NULL,
    trace_id        VARCHAR(64)  NOT NULL,
    request_payload TEXT         NULL,
    response_code   VARCHAR(32)  NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT       NULL,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT       NULL,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS idempotency_record
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_key     VARCHAR(128) NOT NULL,
    request_path    VARCHAR(255) NOT NULL,
    request_method  VARCHAR(16)  NOT NULL,
    response_code   VARCHAR(32)  NOT NULL,
    response_body   TEXT         NULL,
    expires_at      TIMESTAMP    NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT       NULL,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT       NULL,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_idempotency_request_key UNIQUE (request_key)
);
