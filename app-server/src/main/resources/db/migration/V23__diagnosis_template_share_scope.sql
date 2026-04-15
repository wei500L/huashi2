ALTER TABLE diagnosis_template
    ADD COLUMN share_scope VARCHAR(16) NOT NULL DEFAULT 'PRIVATE' AFTER target_class_id;

CREATE INDEX idx_diagnosis_template_share_status_updated
    ON diagnosis_template (share_scope, status, updated_at);
