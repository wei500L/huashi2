ALTER TABLE diagnosis_session
    ADD COLUMN completion_hooks_status VARCHAR(16) NULL,
    ADD COLUMN completion_hooks_updated_at TIMESTAMP NULL,
    ADD COLUMN completion_hooks_error VARCHAR(512) NULL;

ALTER TABLE training_session
    ADD COLUMN completion_hooks_status VARCHAR(16) NULL,
    ADD COLUMN completion_hooks_updated_at TIMESTAMP NULL,
    ADD COLUMN completion_hooks_error VARCHAR(512) NULL;

UPDATE diagnosis_session
SET last_saved_at = COALESCE(last_saved_at, started_at)
WHERE last_saved_at IS NULL;

UPDATE training_session
SET last_saved_at = COALESCE(last_saved_at, started_at)
WHERE last_saved_at IS NULL;

UPDATE diagnosis_session
SET completion_hooks_status = 'DONE',
    completion_hooks_updated_at = COALESCE(completed_at, updated_at, started_at)
WHERE deleted = FALSE
  AND status = 'COMPLETED'
  AND completion_hooks_status IS NULL;

UPDATE training_session
SET completion_hooks_status = 'DONE',
    completion_hooks_updated_at = COALESCE(completed_at, updated_at, started_at)
WHERE deleted = FALSE
  AND status = 'COMPLETED'
  AND completion_hooks_status IS NULL;

CREATE INDEX idx_diagnosis_session_status_last_saved_at ON diagnosis_session (status, last_saved_at);
CREATE INDEX idx_training_session_status_last_saved_at ON training_session (status, last_saved_at);
CREATE INDEX idx_diagnosis_session_completion_hooks_status ON diagnosis_session (completion_hooks_status, completed_at);
CREATE INDEX idx_training_session_completion_hooks_status ON training_session (completion_hooks_status, completed_at);
