ALTER TABLE student_profile
    ADD COLUMN learning_profile_snapshot_json LONGTEXT NULL;

ALTER TABLE student_profile
    ADD COLUMN learning_profile_updated_at TIMESTAMP NULL;

CREATE TABLE IF NOT EXISTS training_plan
(
    id                         BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_user_id              BIGINT        NOT NULL,
    source_diagnosis_session_id BIGINT       NOT NULL,
    source_diagnosis_summary_id BIGINT       NOT NULL,
    status                     VARCHAR(32)   NOT NULL DEFAULT 'GENERATED',
    priority_mode              VARCHAR(32)   NOT NULL,
    recommended_difficulty     INT           NOT NULL,
    risk_level                 VARCHAR(16)   NOT NULL,
    estimated_training_volume  INT           NOT NULL,
    recommendation_reason      VARCHAR(1000) NOT NULL,
    target_metrics_json        LONGTEXT      NOT NULL,
    generated_at               TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at                 TIMESTAMP     NULL,
    completed_at               TIMESTAMP     NULL,
    created_at                 TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                 BIGINT        NULL,
    updated_at                 TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                 BIGINT        NULL,
    deleted                    BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_training_plan_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES users (id),
    CONSTRAINT fk_training_plan_source_diagnosis_session_id FOREIGN KEY (source_diagnosis_session_id) REFERENCES diagnosis_session (id),
    CONSTRAINT fk_training_plan_source_diagnosis_summary_id FOREIGN KEY (source_diagnosis_summary_id) REFERENCES diagnosis_summary (id),
    CONSTRAINT uk_training_plan_owner_summary UNIQUE (owner_user_id, source_diagnosis_summary_id, deleted)
);

CREATE TABLE IF NOT EXISTS training_plan_item
(
    id                     BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id                BIGINT        NOT NULL,
    lexical_pair_id        BIGINT        NOT NULL,
    recommended_mode       VARCHAR(32)   NOT NULL,
    recommended_difficulty INT           NOT NULL,
    risk_level             VARCHAR(16)   NOT NULL,
    priority_score         DECIMAL(10,4) NOT NULL,
    recommended_reason     VARCHAR(1000) NOT NULL,
    dominant_error_type    VARCHAR(64)   NULL,
    target_context_support VARCHAR(16)   NULL,
    expected_exposures     INT           NOT NULL DEFAULT 3,
    sort_order             INT           NOT NULL DEFAULT 1,
    created_at             TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by             BIGINT        NULL,
    updated_at             TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by             BIGINT        NULL,
    deleted                BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_training_plan_item_plan_id FOREIGN KEY (plan_id) REFERENCES training_plan (id),
    CONSTRAINT fk_training_plan_item_lexical_pair_id FOREIGN KEY (lexical_pair_id) REFERENCES lexical_pair (id)
);

CREATE TABLE IF NOT EXISTS training_session
(
    id                    BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id               BIGINT       NOT NULL,
    owner_user_id         BIGINT       NOT NULL,
    mode                  VARCHAR(32)  NOT NULL,
    status                VARCHAR(32)  NOT NULL DEFAULT 'IN_PROGRESS',
    session_seed          BIGINT       NOT NULL,
    total_items           INT          NOT NULL DEFAULT 0,
    answered_items        INT          NOT NULL DEFAULT 0,
    current_item_order    INT          NULL,
    planned_difficulty    INT          NOT NULL,
    risk_level            VARCHAR(16)  NOT NULL,
    progress_snapshot_json LONGTEXT    NULL,
    summary_snapshot_json LONGTEXT     NULL,
    started_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at          TIMESTAMP    NULL,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by            BIGINT       NULL,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by            BIGINT       NULL,
    deleted               BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_training_session_plan_id FOREIGN KEY (plan_id) REFERENCES training_plan (id),
    CONSTRAINT fk_training_session_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS training_item_result
(
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id          BIGINT        NOT NULL,
    plan_item_id        BIGINT        NOT NULL,
    lexical_pair_id     BIGINT        NOT NULL,
    mode                VARCHAR(32)   NOT NULL,
    item_type           VARCHAR(32)   NOT NULL,
    presentation_order  INT           NOT NULL,
    answer_state        VARCHAR(16)   NOT NULL DEFAULT 'PENDING',
    cognitive_tag       VARCHAR(16)   NOT NULL,
    stimulus_json       LONGTEXT      NOT NULL,
    options_json        LONGTEXT      NOT NULL,
    correct_answer_key  VARCHAR(64)   NOT NULL,
    selected_answer_key VARCHAR(64)   NULL,
    answer_payload_json LONGTEXT      NULL,
    submitted_at        TIMESTAMP     NULL,
    reaction_time_ms    INT           NULL,
    hesitation_time_ms  INT           NULL,
    is_correct          BOOLEAN       NULL,
    detected_error_type VARCHAR(64)   NULL,
    review_required     BOOLEAN       NOT NULL DEFAULT FALSE,
    adaptation_action   VARCHAR(64)   NULL,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          BIGINT        NULL,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          BIGINT        NULL,
    deleted             BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_training_item_result_session_id FOREIGN KEY (session_id) REFERENCES training_session (id),
    CONSTRAINT fk_training_item_result_plan_item_id FOREIGN KEY (plan_item_id) REFERENCES training_plan_item (id),
    CONSTRAINT fk_training_item_result_lexical_pair_id FOREIGN KEY (lexical_pair_id) REFERENCES lexical_pair (id),
    CONSTRAINT uk_training_item_result_session_order UNIQUE (session_id, presentation_order)
);

CREATE TABLE IF NOT EXISTS wrong_book
(
    id                         BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_user_id              BIGINT       NOT NULL,
    lexical_pair_id            BIGINT       NOT NULL,
    source_training_session_id BIGINT       NULL,
    source_item_result_id      BIGINT       NULL,
    wrong_count                INT          NOT NULL DEFAULT 1,
    first_wrong_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_wrong_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_error_type            VARCHAR(64)  NULL,
    mastery_status             VARCHAR(32)  NOT NULL DEFAULT 'REVIEWING',
    next_review_at             TIMESTAMP    NULL,
    latest_snapshot_json       LONGTEXT     NULL,
    created_at                 TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                 BIGINT       NULL,
    updated_at                 TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                 BIGINT       NULL,
    deleted                    BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_wrong_book_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES users (id),
    CONSTRAINT fk_wrong_book_lexical_pair_id FOREIGN KEY (lexical_pair_id) REFERENCES lexical_pair (id),
    CONSTRAINT fk_wrong_book_source_training_session_id FOREIGN KEY (source_training_session_id) REFERENCES training_session (id),
    CONSTRAINT fk_wrong_book_source_item_result_id FOREIGN KEY (source_item_result_id) REFERENCES training_item_result (id),
    CONSTRAINT uk_wrong_book_owner_pair UNIQUE (owner_user_id, lexical_pair_id, deleted)
);

CREATE TABLE IF NOT EXISTS review_schedule
(
    id                         BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_user_id              BIGINT       NOT NULL,
    lexical_pair_id            BIGINT       NOT NULL,
    wrong_book_id              BIGINT       NOT NULL,
    source_training_session_id BIGINT       NULL,
    schedule_stage             INT          NOT NULL,
    interval_days              INT          NOT NULL,
    due_at                     TIMESTAMP    NOT NULL,
    status                     VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    review_mode                VARCHAR(32)  NOT NULL,
    trigger_reason             VARCHAR(255) NOT NULL,
    completed_at               TIMESTAMP    NULL,
    snapshot_json              LONGTEXT     NULL,
    created_at                 TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                 BIGINT       NULL,
    updated_at                 TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                 BIGINT       NULL,
    deleted                    BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_review_schedule_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES users (id),
    CONSTRAINT fk_review_schedule_lexical_pair_id FOREIGN KEY (lexical_pair_id) REFERENCES lexical_pair (id),
    CONSTRAINT fk_review_schedule_wrong_book_id FOREIGN KEY (wrong_book_id) REFERENCES wrong_book (id),
    CONSTRAINT fk_review_schedule_source_training_session_id FOREIGN KEY (source_training_session_id) REFERENCES training_session (id)
);

CREATE INDEX idx_training_plan_owner_generated ON training_plan (owner_user_id, generated_at);
CREATE INDEX idx_training_plan_status ON training_plan (owner_user_id, status, generated_at);
CREATE INDEX idx_training_plan_item_plan_order ON training_plan_item (plan_id, recommended_mode, sort_order);
CREATE INDEX idx_training_session_owner_status ON training_session (owner_user_id, status, started_at);
CREATE INDEX idx_training_item_result_session_order ON training_item_result (session_id, presentation_order);
CREATE INDEX idx_training_item_result_session_state ON training_item_result (session_id, answer_state);
CREATE INDEX idx_wrong_book_owner_next_review ON wrong_book (owner_user_id, next_review_at);
CREATE INDEX idx_review_schedule_owner_due ON review_schedule (owner_user_id, status, due_at);
