ALTER TABLE idempotency_record
    ADD COLUMN request_hash VARCHAR(64) NULL;

CREATE INDEX idx_idempotency_record_expires_at ON idempotency_record (expires_at);

UPDATE diagnosis_session
SET status = 'ABANDONED',
    current_item_order = NULL,
    updated_at = CURRENT_TIMESTAMP
WHERE id IN (
    SELECT doomed.id
    FROM (
        SELECT session_row.id
        FROM diagnosis_session session_row
        WHERE session_row.deleted = FALSE
          AND session_row.status = 'IN_PROGRESS'
          AND EXISTS (
                SELECT 1
                FROM diagnosis_session newer
                WHERE newer.deleted = FALSE
                  AND newer.status = 'IN_PROGRESS'
                  AND newer.owner_user_id = session_row.owner_user_id
                  AND (
                        newer.started_at > session_row.started_at
                        OR (newer.started_at = session_row.started_at AND newer.id > session_row.id)
                      )
            )
    ) doomed
);

UPDATE training_session
SET status = 'ABANDONED',
    current_item_order = NULL,
    updated_at = CURRENT_TIMESTAMP
WHERE id IN (
    SELECT doomed.id
    FROM (
        SELECT session_row.id
        FROM training_session session_row
        WHERE session_row.deleted = FALSE
          AND session_row.status = 'IN_PROGRESS'
          AND EXISTS (
                SELECT 1
                FROM training_session newer
                WHERE newer.deleted = FALSE
                  AND newer.status = 'IN_PROGRESS'
                  AND newer.owner_user_id = session_row.owner_user_id
                  AND (
                        newer.started_at > session_row.started_at
                        OR (newer.started_at = session_row.started_at AND newer.id > session_row.id)
                      )
            )
    ) doomed
);

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
