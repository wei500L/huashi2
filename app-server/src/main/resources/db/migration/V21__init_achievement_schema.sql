CREATE TABLE IF NOT EXISTS achievement
(
    id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_user_id      BIGINT       NOT NULL,
    achievement_code   VARCHAR(64)  NOT NULL,
    unlocked           BOOLEAN      NOT NULL DEFAULT FALSE,
    progress_value     INT          NOT NULL DEFAULT 0,
    target_value       INT          NOT NULL DEFAULT 0,
    awarded_at         TIMESTAMP    NULL,
    last_calculated_at TIMESTAMP    NULL,
    metadata_json      LONGTEXT     NULL,
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by         BIGINT       NULL,
    updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by         BIGINT       NULL,
    deleted            BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_achievement_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES users (id),
    CONSTRAINT uk_achievement_owner_code UNIQUE (owner_user_id, achievement_code, deleted)
);

CREATE INDEX idx_achievement_owner_unlocked ON achievement (owner_user_id, unlocked, awarded_at);
