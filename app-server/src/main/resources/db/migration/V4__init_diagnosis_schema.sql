CREATE TABLE IF NOT EXISTS diagnosis_template
(
    id                         BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_name              VARCHAR(128) NOT NULL,
    description                VARCHAR(500) NULL,
    owner_user_id              BIGINT       NOT NULL,
    status                     VARCHAR(32)  NOT NULL DEFAULT 'DRAFT',
    estimated_duration_minutes INT          NOT NULL DEFAULT 10,
    scoring_version            VARCHAR(32)  NOT NULL DEFAULT 'RULE_V1',
    item_count                 INT          NOT NULL DEFAULT 0,
    metadata_json              TEXT         NULL,
    created_at                 TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                 BIGINT       NULL,
    updated_at                 TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                 BIGINT       NULL,
    deleted                    BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_diagnosis_template_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS diagnosis_template_item
(
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_id             BIGINT        NOT NULL,
    lexical_pair_id         BIGINT        NOT NULL,
    task_type               VARCHAR(32)   NOT NULL,
    block_code              VARCHAR(64)   NOT NULL,
    sort_order              INT           NOT NULL DEFAULT 1,
    context_support_level   VARCHAR(16)   NOT NULL,
    expected_semantic_match BOOLEAN       NOT NULL,
    stimulus_payload_json   LONGTEXT      NOT NULL,
    options_payload_json    LONGTEXT      NOT NULL,
    correct_answer_key      VARCHAR(64)   NOT NULL,
    scoring_profile_json    LONGTEXT      NULL,
    created_at              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              BIGINT        NULL,
    updated_at              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by              BIGINT        NULL,
    deleted                 BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_diagnosis_template_item_template_id FOREIGN KEY (template_id) REFERENCES diagnosis_template (id),
    CONSTRAINT fk_diagnosis_template_item_lexical_pair_id FOREIGN KEY (lexical_pair_id) REFERENCES lexical_pair (id)
);

CREATE TABLE IF NOT EXISTS diagnosis_session
(
    id                    BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_id           BIGINT       NOT NULL,
    owner_user_id         BIGINT       NOT NULL,
    status                VARCHAR(32)  NOT NULL DEFAULT 'IN_PROGRESS',
    session_seed          BIGINT       NOT NULL,
    total_items           INT          NOT NULL DEFAULT 0,
    answered_items        INT          NOT NULL DEFAULT 0,
    current_item_order    INT          NULL,
    last_saved_at         TIMESTAMP    NULL,
    progress_snapshot_json LONGTEXT    NULL,
    started_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at          TIMESTAMP    NULL,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by            BIGINT       NULL,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by            BIGINT       NULL,
    deleted               BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_diagnosis_session_template_id FOREIGN KEY (template_id) REFERENCES diagnosis_template (id),
    CONSTRAINT fk_diagnosis_session_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS diagnosis_item_result
(
    id                    BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id            BIGINT         NOT NULL,
    template_item_id      BIGINT         NOT NULL,
    lexical_pair_id       BIGINT         NOT NULL,
    task_type             VARCHAR(32)    NOT NULL,
    presentation_order    INT            NOT NULL,
    answer_state          VARCHAR(16)    NOT NULL DEFAULT 'PENDING',
    stimulus_started_at   TIMESTAMP      NULL,
    submitted_at          TIMESTAMP      NULL,
    reaction_time_ms      INT            NULL,
    hesitation_time_ms    INT            NULL,
    selected_answer_key   VARCHAR(64)    NULL,
    answer_payload_json   LONGTEXT       NULL,
    is_correct            BOOLEAN        NULL,
    detected_error_type   VARCHAR(64)    NULL,
    semantic_consistent   BOOLEAN        NULL,
    transfer_risk_score   DECIMAL(8, 4)  NULL,
    item_score            DECIMAL(8, 4)  NULL,
    created_at            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by            BIGINT         NULL,
    updated_at            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by            BIGINT         NULL,
    deleted               BOOLEAN        NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_diagnosis_item_result_session_id FOREIGN KEY (session_id) REFERENCES diagnosis_session (id),
    CONSTRAINT fk_diagnosis_item_result_template_item_id FOREIGN KEY (template_item_id) REFERENCES diagnosis_template_item (id),
    CONSTRAINT fk_diagnosis_item_result_lexical_pair_id FOREIGN KEY (lexical_pair_id) REFERENCES lexical_pair (id),
    CONSTRAINT uk_diagnosis_item_result_session_item UNIQUE (session_id, template_item_id)
);

CREATE TABLE IF NOT EXISTS diagnosis_summary
(
    id                           BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id                   BIGINT         NOT NULL,
    owner_user_id                BIGINT         NOT NULL,
    template_id                  BIGINT         NOT NULL,
    positive_transfer_score      DECIMAL(8, 4)  NOT NULL,
    negative_transfer_risk       DECIMAL(8, 4)  NOT NULL,
    context_sensitivity          DECIMAL(8, 4)  NOT NULL,
    semantic_discrimination      DECIMAL(8, 4)  NOT NULL,
    overall_accuracy             DECIMAL(8, 4)  NOT NULL,
    average_reaction_time_ms     BIGINT         NOT NULL,
    error_type_distribution_json LONGTEXT       NOT NULL,
    high_risk_lexical_pairs_json LONGTEXT       NOT NULL,
    chart_payload_json           LONGTEXT       NOT NULL,
    generated_at                 TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    scoring_version              VARCHAR(32)    NOT NULL DEFAULT 'RULE_V1',
    created_at                   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                   BIGINT         NULL,
    updated_at                   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                   BIGINT         NULL,
    deleted                      BOOLEAN        NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_diagnosis_summary_session_id FOREIGN KEY (session_id) REFERENCES diagnosis_session (id),
    CONSTRAINT fk_diagnosis_summary_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES users (id),
    CONSTRAINT fk_diagnosis_summary_template_id FOREIGN KEY (template_id) REFERENCES diagnosis_template (id),
    CONSTRAINT uk_diagnosis_summary_session UNIQUE (session_id)
);

CREATE INDEX idx_diagnosis_template_owner_status ON diagnosis_template (owner_user_id, status);
CREATE INDEX idx_diagnosis_template_item_template_order ON diagnosis_template_item (template_id, block_code, sort_order);
CREATE INDEX idx_diagnosis_session_owner_status ON diagnosis_session (owner_user_id, status, started_at);
CREATE INDEX idx_diagnosis_session_template_status ON diagnosis_session (template_id, status);
CREATE INDEX idx_diagnosis_item_result_session_order ON diagnosis_item_result (session_id, presentation_order);
CREATE INDEX idx_diagnosis_item_result_session_state ON diagnosis_item_result (session_id, answer_state);
CREATE INDEX idx_diagnosis_summary_owner_generated ON diagnosis_summary (owner_user_id, generated_at);
