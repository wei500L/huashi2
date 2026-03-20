ALTER TABLE student_profile
    ADD COLUMN IF NOT EXISTS course_stage VARCHAR(64) NOT NULL DEFAULT 'FOUNDATION';

UPDATE student_profile
SET course_stage = 'FOUNDATION'
WHERE course_stage IS NULL OR TRIM(course_stage) = '';

CREATE TABLE IF NOT EXISTS ai_generation_record
(
    id                   BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_id           VARCHAR(64)   NOT NULL,
    scene                VARCHAR(64)   NOT NULL,
    student_user_id      BIGINT        NULL,
    teacher_user_id      BIGINT        NULL,
    teaching_class_id    BIGINT        NULL,
    diagnosis_summary_id BIGINT        NULL,
    training_session_id  BIGINT        NULL,
    intervention_record_id BIGINT      NULL,
    prompt_version       VARCHAR(64)   NOT NULL,
    model                VARCHAR(128)  NULL,
    provider_request_id  VARCHAR(128)  NULL,
    latency_ms           BIGINT        NOT NULL DEFAULT 0,
    token_usage_json     LONGTEXT      NULL,
    input_payload_json   LONGTEXT      NOT NULL,
    raw_response_json    LONGTEXT      NULL,
    validated_output_json LONGTEXT     NOT NULL,
    generation_source    VARCHAR(32)   NOT NULL,
    fallback_reason      VARCHAR(128)  NULL,
    generated_at         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by           BIGINT        NULL,
    updated_at           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by           BIGINT        NULL,
    deleted              BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_ai_generation_record_student_user_id FOREIGN KEY (student_user_id) REFERENCES users (id),
    CONSTRAINT fk_ai_generation_record_teacher_user_id FOREIGN KEY (teacher_user_id) REFERENCES users (id),
    CONSTRAINT fk_ai_generation_record_class_id FOREIGN KEY (teaching_class_id) REFERENCES teaching_class (id),
    CONSTRAINT fk_ai_generation_record_diagnosis_summary_id FOREIGN KEY (diagnosis_summary_id) REFERENCES diagnosis_summary (id),
    CONSTRAINT fk_ai_generation_record_training_session_id FOREIGN KEY (training_session_id) REFERENCES training_session (id),
    CONSTRAINT fk_ai_generation_record_intervention_record_id FOREIGN KEY (intervention_record_id) REFERENCES intervention_record (id),
    CONSTRAINT uk_ai_generation_record_request_id UNIQUE (request_id)
);

CREATE INDEX idx_ai_generation_record_scene_generated_at ON ai_generation_record (scene, generated_at);
CREATE INDEX idx_ai_generation_record_student_scene ON ai_generation_record (student_user_id, scene, generated_at);
CREATE INDEX idx_ai_generation_record_teacher_scene ON ai_generation_record (teacher_user_id, scene, generated_at);
