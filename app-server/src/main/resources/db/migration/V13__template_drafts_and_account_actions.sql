CREATE TABLE IF NOT EXISTS diagnosis_template_draft
(
    id                         BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_user_id              BIGINT       NOT NULL,
    source_template_id         BIGINT       NULL,
    published_template_id      BIGINT       NULL,
    template_name              VARCHAR(128) NOT NULL,
    description                VARCHAR(500) NULL,
    publish_target             VARCHAR(32)  NOT NULL DEFAULT 'SELF',
    estimated_duration_minutes INT          NOT NULL DEFAULT 10,
    scoring_version            VARCHAR(32)  NOT NULL DEFAULT 'RULE_V1',
    sync_state                 VARCHAR(32)  NOT NULL DEFAULT 'DIRTY',
    version                    BIGINT       NOT NULL DEFAULT 1,
    schema_json                LONGTEXT     NOT NULL,
    created_at                 TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                 BIGINT       NULL,
    updated_at                 TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                 BIGINT       NULL,
    deleted                    BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_diagnosis_template_draft_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES users (id),
    CONSTRAINT fk_diagnosis_template_draft_source_template_id FOREIGN KEY (source_template_id) REFERENCES diagnosis_template (id),
    CONSTRAINT fk_diagnosis_template_draft_published_template_id FOREIGN KEY (published_template_id) REFERENCES diagnosis_template (id)
);

CREATE TABLE IF NOT EXISTS account_action_token
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    purpose         VARCHAR(32)  NOT NULL,
    token_hash      VARCHAR(128) NOT NULL,
    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    expires_at      TIMESTAMP    NOT NULL,
    consumed_at     TIMESTAMP    NULL,
    invalidated_at  TIMESTAMP    NULL,
    metadata_json   LONGTEXT     NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT       NULL,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT       NULL,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_account_action_token_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uk_account_action_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_diagnosis_template_draft_owner_updated
    ON diagnosis_template_draft (owner_user_id, updated_at);
CREATE INDEX idx_diagnosis_template_draft_owner_source
    ON diagnosis_template_draft (owner_user_id, source_template_id, deleted);
CREATE INDEX idx_account_action_token_user_purpose_status
    ON account_action_token (user_id, purpose, status, expires_at);
