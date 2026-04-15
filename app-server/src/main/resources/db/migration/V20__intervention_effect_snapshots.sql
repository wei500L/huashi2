ALTER TABLE intervention_record
    ADD COLUMN baseline_snapshot_id BIGINT NULL AFTER trigger_snapshot_json,
    ADD COLUMN completion_snapshot_id BIGINT NULL AFTER baseline_snapshot_id,
    ADD CONSTRAINT fk_intervention_record_baseline_snapshot
        FOREIGN KEY (baseline_snapshot_id) REFERENCES learning_profile_snapshot (id),
    ADD CONSTRAINT fk_intervention_record_completion_snapshot
        FOREIGN KEY (completion_snapshot_id) REFERENCES learning_profile_snapshot (id);

CREATE INDEX idx_intervention_record_student_snapshots
    ON intervention_record (student_user_id, baseline_snapshot_id, completion_snapshot_id);
