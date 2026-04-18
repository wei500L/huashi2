CREATE TABLE IF NOT EXISTS admin_ai_config_history
(
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key              VARCHAR(64)  NOT NULL,
    version_number          BIGINT       NOT NULL,
    previous_version_number BIGINT       NULL,
    encrypted_config        LONGTEXT     NOT NULL,
    change_summary_json     LONGTEXT     NOT NULL,
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              BIGINT       NULL,
    updated_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by              BIGINT       NULL,
    deleted                 BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_admin_ai_config_history_key_version UNIQUE (config_key, version_number)
);
