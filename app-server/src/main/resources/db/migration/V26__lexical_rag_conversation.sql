CREATE TABLE IF NOT EXISTS ai_conversation_session
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id VARCHAR(64)  NOT NULL,
    scene           VARCHAR(64)  NOT NULL,
    student_user_id BIGINT       NOT NULL,
    title           VARCHAR(128) NOT NULL,
    last_message_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT       NULL,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT       NULL,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_ai_conversation_session_student_user_id FOREIGN KEY (student_user_id) REFERENCES users (id),
    CONSTRAINT uk_ai_conversation_session_conversation_id UNIQUE (conversation_id)
);

CREATE INDEX idx_ai_conversation_session_student_scene_last_message
    ON ai_conversation_session (student_user_id, scene, last_message_at);

CREATE TABLE IF NOT EXISTS ai_conversation_message
(
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_session_id BIGINT        NOT NULL,
    role                    VARCHAR(16)   NOT NULL,
    content_text            LONGTEXT      NOT NULL,
    payload_json            LONGTEXT      NULL,
    request_id              VARCHAR(64)   NULL,
    generation_source       VARCHAR(32)   NULL,
    model                   VARCHAR(128)  NULL,
    grounded                BOOLEAN       NULL,
    fallback_reason         VARCHAR(128)  NULL,
    created_at              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              BIGINT        NULL,
    updated_at              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by              BIGINT        NULL,
    deleted                 BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_ai_conversation_message_session_id FOREIGN KEY (conversation_session_id) REFERENCES ai_conversation_session (id)
);

CREATE INDEX idx_ai_conversation_message_session_created_at
    ON ai_conversation_message (conversation_session_id, created_at);
