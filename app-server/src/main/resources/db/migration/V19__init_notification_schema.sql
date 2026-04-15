CREATE TABLE IF NOT EXISTS notification
(
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    recipient_user_id BIGINT       NOT NULL,
    category          VARCHAR(64)  NOT NULL,
    level             VARCHAR(32)  NOT NULL,
    title             VARCHAR(160) NOT NULL,
    content           TEXT         NOT NULL,
    action_url        VARCHAR(255) NULL,
    action_label      VARCHAR(64)  NULL,
    status            VARCHAR(32)  NOT NULL DEFAULT 'UNREAD',
    read_at           TIMESTAMP    NULL,
    payload_json      TEXT         NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by        BIGINT       NULL,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by        BIGINT       NULL,
    deleted           BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_notification_recipient_user_id FOREIGN KEY (recipient_user_id) REFERENCES users (id)
);

CREATE INDEX idx_notification_recipient_created ON notification (recipient_user_id, created_at DESC, id DESC);
CREATE INDEX idx_notification_recipient_status ON notification (recipient_user_id, status, created_at DESC, id DESC);
