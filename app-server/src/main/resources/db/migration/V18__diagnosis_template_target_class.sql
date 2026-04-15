ALTER TABLE diagnosis_template
    ADD COLUMN target_class_id BIGINT NULL;

ALTER TABLE diagnosis_template
    ADD CONSTRAINT fk_diagnosis_template_target_class_id
        FOREIGN KEY (target_class_id) REFERENCES teaching_class (id);

CREATE INDEX idx_diagnosis_template_status_target_class
    ON diagnosis_template (status, target_class_id, updated_at);
