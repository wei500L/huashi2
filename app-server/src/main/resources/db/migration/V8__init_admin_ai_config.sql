CREATE TABLE IF NOT EXISTS admin_ai_config
(
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key       VARCHAR(64)  NOT NULL,
    encrypted_config LONGTEXT     NOT NULL,
    version_number   BIGINT       NOT NULL DEFAULT 1,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by       BIGINT       NULL,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by       BIGINT       NULL,
    deleted          BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_admin_ai_config_key UNIQUE (config_key)
);
