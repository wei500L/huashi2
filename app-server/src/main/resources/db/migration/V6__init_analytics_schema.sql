CREATE TABLE IF NOT EXISTS teaching_class
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    class_code      VARCHAR(64)  NOT NULL,
    class_name      VARCHAR(128) NOT NULL,
    grade_name      VARCHAR(64)  NOT NULL,
    teacher_user_id BIGINT       NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT       NULL,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT       NULL,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_teaching_class_teacher_user_id FOREIGN KEY (teacher_user_id) REFERENCES users (id),
    CONSTRAINT uk_teaching_class_code UNIQUE (class_code)
);

CREATE TABLE IF NOT EXISTS teaching_class_student
(
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    teaching_class_id BIGINT    NOT NULL,
    student_user_id   BIGINT    NOT NULL,
    joined_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    left_at           TIMESTAMP NULL,
    active            BOOLEAN   NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by        BIGINT    NULL,
    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by        BIGINT    NULL,
    deleted           BOOLEAN   NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_teaching_class_student_class_id FOREIGN KEY (teaching_class_id) REFERENCES teaching_class (id),
    CONSTRAINT fk_teaching_class_student_student_user_id FOREIGN KEY (student_user_id) REFERENCES users (id),
    CONSTRAINT uk_teaching_class_student UNIQUE (teaching_class_id, student_user_id, deleted)
);

CREATE TABLE IF NOT EXISTS learning_profile_snapshot
(
    id                           BIGINT PRIMARY KEY AUTO_INCREMENT,
    scope                        VARCHAR(16)   NOT NULL,
    student_user_id              BIGINT        NULL,
    teaching_class_id            BIGINT        NULL,
    teacher_user_id              BIGINT        NULL,
    last_diagnosis_summary_id    BIGINT        NULL,
    last_training_session_id     BIGINT        NULL,
    primary_risk_level           VARCHAR(16)   NOT NULL,
    recommended_training_mode    VARCHAR(32)   NOT NULL,
    pending_review_count         INT           NOT NULL DEFAULT 0,
    high_risk_pair_count         INT           NOT NULL DEFAULT 0,
    recent_accuracy              DECIMAL(8, 4) NOT NULL DEFAULT 0,
    recent_negative_transfer_risk DECIMAL(8, 4) NOT NULL DEFAULT 0,
    recent_avg_reaction_time_ms  BIGINT        NOT NULL DEFAULT 0,
    last_active_at               TIMESTAMP     NULL,
    snapshot_json                LONGTEXT      NOT NULL,
    snapshot_at                  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version                      INT           NOT NULL DEFAULT 1,
    created_at                   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                   BIGINT        NULL,
    updated_at                   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                   BIGINT        NULL,
    deleted                      BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_learning_profile_snapshot_student_user_id FOREIGN KEY (student_user_id) REFERENCES users (id),
    CONSTRAINT fk_learning_profile_snapshot_class_id FOREIGN KEY (teaching_class_id) REFERENCES teaching_class (id),
    CONSTRAINT fk_learning_profile_snapshot_teacher_user_id FOREIGN KEY (teacher_user_id) REFERENCES users (id),
    CONSTRAINT uk_learning_profile_snapshot_scope_student UNIQUE (scope, student_user_id, deleted),
    CONSTRAINT uk_learning_profile_snapshot_scope_class UNIQUE (scope, teaching_class_id, deleted)
);

CREATE TABLE IF NOT EXISTS analytics_daily_aggregate
(
    id                               BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_user_id                    BIGINT         NOT NULL,
    stat_date                        DATE           NOT NULL,
    week_start_date                  DATE           NOT NULL,
    source_type                      VARCHAR(16)    NOT NULL,
    aggregation_level                VARCHAR(16)    NOT NULL,
    lexical_pair_id                  BIGINT         NOT NULL DEFAULT 0,
    lexical_pair_type                VARCHAR(32)    NOT NULL DEFAULT 'ALL',
    training_mode                    VARCHAR(32)    NOT NULL DEFAULT 'ALL',
    context_support_level            VARCHAR(16)    NOT NULL DEFAULT 'ALL',
    attempt_count                    INT            NOT NULL DEFAULT 0,
    correct_count                    INT            NOT NULL DEFAULT 0,
    incorrect_count                  INT            NOT NULL DEFAULT 0,
    total_reaction_time_ms           BIGINT         NOT NULL DEFAULT 0,
    total_hesitation_time_ms         BIGINT         NOT NULL DEFAULT 0,
    positive_transfer_score_sum      DECIMAL(14, 4) NOT NULL DEFAULT 0,
    negative_transfer_risk_sum       DECIMAL(14, 4) NOT NULL DEFAULT 0,
    context_sensitivity_sum          DECIMAL(14, 4) NOT NULL DEFAULT 0,
    semantic_discrimination_sum      DECIMAL(14, 4) NOT NULL DEFAULT 0,
    high_risk_count                  INT            NOT NULL DEFAULT 0,
    pending_review_count             INT            NOT NULL DEFAULT 0,
    completion_count                 INT            NOT NULL DEFAULT 0,
    false_friend_confusion_count     INT            NOT NULL DEFAULT 0,
    context_ignored_count            INT            NOT NULL DEFAULT 0,
    over_transfer_count              INT            NOT NULL DEFAULT 0,
    under_transfer_count             INT            NOT NULL DEFAULT 0,
    orthographic_interference_count  INT            NOT NULL DEFAULT 0,
    semantic_misfire_count           INT            NOT NULL DEFAULT 0,
    last_event_at                    TIMESTAMP      NULL,
    created_at                       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                       BIGINT         NULL,
    updated_at                       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                       BIGINT         NULL,
    deleted                          BOOLEAN        NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_analytics_daily_aggregate_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES users (id),
    CONSTRAINT uk_analytics_daily_aggregate UNIQUE (
        owner_user_id,
        stat_date,
        source_type,
        aggregation_level,
        lexical_pair_id,
        lexical_pair_type,
        training_mode,
        context_support_level,
        deleted
    )
);

CREATE TABLE IF NOT EXISTS class_analytics_daily_aggregate
(
    id                               BIGINT PRIMARY KEY AUTO_INCREMENT,
    teaching_class_id                BIGINT         NOT NULL,
    stat_date                        DATE           NOT NULL,
    week_start_date                  DATE           NOT NULL,
    source_type                      VARCHAR(16)    NOT NULL,
    aggregation_level                VARCHAR(16)    NOT NULL,
    lexical_pair_id                  BIGINT         NOT NULL DEFAULT 0,
    lexical_pair_type                VARCHAR(32)    NOT NULL DEFAULT 'ALL',
    training_mode                    VARCHAR(32)    NOT NULL DEFAULT 'ALL',
    context_support_level            VARCHAR(16)    NOT NULL DEFAULT 'ALL',
    attempt_count                    INT            NOT NULL DEFAULT 0,
    correct_count                    INT            NOT NULL DEFAULT 0,
    incorrect_count                  INT            NOT NULL DEFAULT 0,
    total_reaction_time_ms           BIGINT         NOT NULL DEFAULT 0,
    total_hesitation_time_ms         BIGINT         NOT NULL DEFAULT 0,
    positive_transfer_score_sum      DECIMAL(14, 4) NOT NULL DEFAULT 0,
    negative_transfer_risk_sum       DECIMAL(14, 4) NOT NULL DEFAULT 0,
    context_sensitivity_sum          DECIMAL(14, 4) NOT NULL DEFAULT 0,
    semantic_discrimination_sum      DECIMAL(14, 4) NOT NULL DEFAULT 0,
    high_risk_count                  INT            NOT NULL DEFAULT 0,
    pending_review_count             INT            NOT NULL DEFAULT 0,
    completion_count                 INT            NOT NULL DEFAULT 0,
    false_friend_confusion_count     INT            NOT NULL DEFAULT 0,
    context_ignored_count            INT            NOT NULL DEFAULT 0,
    over_transfer_count              INT            NOT NULL DEFAULT 0,
    under_transfer_count             INT            NOT NULL DEFAULT 0,
    orthographic_interference_count  INT            NOT NULL DEFAULT 0,
    semantic_misfire_count           INT            NOT NULL DEFAULT 0,
    last_event_at                    TIMESTAMP      NULL,
    created_at                       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                       BIGINT         NULL,
    updated_at                       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                       BIGINT         NULL,
    deleted                          BOOLEAN        NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_class_analytics_daily_aggregate_class_id FOREIGN KEY (teaching_class_id) REFERENCES teaching_class (id),
    CONSTRAINT uk_class_analytics_daily_aggregate UNIQUE (
        teaching_class_id,
        stat_date,
        source_type,
        aggregation_level,
        lexical_pair_id,
        lexical_pair_type,
        training_mode,
        context_support_level,
        deleted
    )
);

CREATE TABLE IF NOT EXISTS intervention_record
(
    id                   BIGINT PRIMARY KEY AUTO_INCREMENT,
    teacher_user_id      BIGINT       NOT NULL,
    teaching_class_id    BIGINT       NULL,
    student_user_id      BIGINT       NULL,
    intervention_type    VARCHAR(64)  NOT NULL,
    status               VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    priority             VARCHAR(16)  NOT NULL DEFAULT 'NORMAL',
    trigger_source       VARCHAR(32)  NOT NULL,
    trigger_snapshot_json LONGTEXT    NULL,
    note                 VARCHAR(1000) NULL,
    planned_at           TIMESTAMP    NULL,
    completed_at         TIMESTAMP    NULL,
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by           BIGINT       NULL,
    updated_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by           BIGINT       NULL,
    deleted              BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_intervention_record_teacher_user_id FOREIGN KEY (teacher_user_id) REFERENCES users (id),
    CONSTRAINT fk_intervention_record_class_id FOREIGN KEY (teaching_class_id) REFERENCES teaching_class (id),
    CONSTRAINT fk_intervention_record_student_user_id FOREIGN KEY (student_user_id) REFERENCES users (id)
);

CREATE INDEX idx_teaching_class_teacher_active ON teaching_class (teacher_user_id, active);
CREATE INDEX idx_teaching_class_student_class_active ON teaching_class_student (teaching_class_id, active);
CREATE INDEX idx_teaching_class_student_student_active ON teaching_class_student (student_user_id, active);
CREATE INDEX idx_learning_profile_snapshot_student ON learning_profile_snapshot (scope, student_user_id, snapshot_at);
CREATE INDEX idx_learning_profile_snapshot_class ON learning_profile_snapshot (scope, teaching_class_id, snapshot_at);
CREATE INDEX idx_analytics_daily_aggregate_owner_date ON analytics_daily_aggregate (owner_user_id, stat_date);
CREATE INDEX idx_analytics_daily_aggregate_owner_week ON analytics_daily_aggregate (owner_user_id, week_start_date);
CREATE INDEX idx_analytics_daily_aggregate_owner_pair_type_date ON analytics_daily_aggregate (owner_user_id, lexical_pair_type, stat_date);
CREATE INDEX idx_class_analytics_daily_aggregate_class_date ON class_analytics_daily_aggregate (teaching_class_id, stat_date);
CREATE INDEX idx_class_analytics_daily_aggregate_class_week ON class_analytics_daily_aggregate (teaching_class_id, week_start_date);
CREATE INDEX idx_intervention_record_teacher_status_priority ON intervention_record (teacher_user_id, status, priority, planned_at);
