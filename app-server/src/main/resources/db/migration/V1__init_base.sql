CREATE TABLE IF NOT EXISTS sys_user
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    username      VARCHAR(64)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(128) NOT NULL,
    role          VARCHAR(32)  NOT NULL,
    enabled       TINYINT(1)   NOT NULL DEFAULT 1,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    BIGINT       NULL,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by    BIGINT       NULL,
    deleted       TINYINT(1)   NOT NULL DEFAULT 0,
    CONSTRAINT uk_sys_user_username UNIQUE (username)
);

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
    request_payload JSON         NULL,
    response_code   VARCHAR(32)  NOT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT       NULL,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by      BIGINT       NULL,
    deleted         TINYINT(1)   NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS idempotency_record
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_key     VARCHAR(128) NOT NULL,
    request_path    VARCHAR(255) NOT NULL,
    request_method  VARCHAR(16)  NOT NULL,
    response_code   VARCHAR(32)  NOT NULL,
    response_body   JSON         NULL,
    expires_at      DATETIME     NOT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT       NULL,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by      BIGINT       NULL,
    deleted         TINYINT(1)   NOT NULL DEFAULT 0,
    CONSTRAINT uk_idempotency_request_key UNIQUE (request_key)
);
