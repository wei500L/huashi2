CREATE INDEX idx_audit_log_created_at ON audit_log (created_at);
CREATE INDEX idx_audit_log_actor_created_at ON audit_log (actor_user_id, created_at);
CREATE INDEX idx_audit_log_action_created_at ON audit_log (action_type, created_at);
CREATE INDEX idx_training_plan_item_lexical_pair_id ON training_plan_item (lexical_pair_id);
