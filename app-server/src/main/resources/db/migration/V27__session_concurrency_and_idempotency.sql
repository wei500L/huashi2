ALTER TABLE idempotency_record
    ADD COLUMN request_hash VARCHAR(64) NULL;

CREATE INDEX idx_idempotency_record_expires_at ON idempotency_record (expires_at);

ALTER TABLE diagnosis_session
    ADD COLUMN active_owner_user_id BIGINT
        GENERATED ALWAYS AS (
            CASE
                WHEN deleted = FALSE AND status = 'IN_PROGRESS' THEN owner_user_id
                ELSE NULL
            END
        );

ALTER TABLE training_session
    ADD COLUMN active_owner_user_id BIGINT
        GENERATED ALWAYS AS (
            CASE
                WHEN deleted = FALSE AND status = 'IN_PROGRESS' THEN owner_user_id
                ELSE NULL
            END
        );

CREATE UNIQUE INDEX uk_diagnosis_session_active_owner ON diagnosis_session (active_owner_user_id);
CREATE UNIQUE INDEX uk_training_session_active_owner ON training_session (active_owner_user_id);
