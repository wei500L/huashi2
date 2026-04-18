ALTER TABLE lexical_pair
    DROP INDEX uk_lexical_pair_word_pair,
    ADD COLUMN active_english_word VARCHAR(128)
        GENERATED ALWAYS AS (CASE WHEN deleted = FALSE THEN english_word ELSE NULL END) STORED,
    ADD UNIQUE INDEX uk_lexical_pair_word_pair (active_english_word, french_word);

ALTER TABLE lexical_tag
    DROP INDEX uk_lexical_tag_name,
    ADD COLUMN active_tag_name VARCHAR(64)
        GENERATED ALWAYS AS (CASE WHEN deleted = FALSE THEN tag_name ELSE NULL END) STORED,
    ADD UNIQUE INDEX uk_lexical_tag_name (active_tag_name);

ALTER TABLE lexical_pair_tag_rel
    DROP INDEX uk_lexical_pair_tag_rel,
    ADD COLUMN active_lexical_pair_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN deleted = FALSE THEN lexical_pair_id ELSE NULL END) STORED,
    ADD UNIQUE INDEX uk_lexical_pair_tag_rel (active_lexical_pair_id, lexical_tag_id);

ALTER TABLE lexical_list_item
    DROP INDEX uk_lexical_list_item,
    ADD COLUMN active_lexical_list_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN deleted = FALSE THEN lexical_list_id ELSE NULL END) STORED,
    ADD UNIQUE INDEX uk_lexical_list_item (active_lexical_list_id, lexical_pair_id);

ALTER TABLE training_plan
    DROP INDEX uk_training_plan_owner_summary,
    ADD COLUMN active_owner_user_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN deleted = FALSE THEN owner_user_id ELSE NULL END) STORED,
    ADD UNIQUE INDEX uk_training_plan_owner_summary (active_owner_user_id, source_diagnosis_summary_id);

ALTER TABLE wrong_book
    DROP INDEX uk_wrong_book_owner_pair,
    ADD COLUMN active_owner_user_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN deleted = FALSE THEN owner_user_id ELSE NULL END) STORED,
    ADD UNIQUE INDEX uk_wrong_book_owner_pair (active_owner_user_id, lexical_pair_id);

ALTER TABLE assessment_attempt
    DROP INDEX uk_assessment_attempt_publish_student,
    ADD COLUMN active_publish_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN deleted = FALSE THEN publish_id ELSE NULL END) STORED,
    ADD UNIQUE INDEX uk_assessment_attempt_publish_student (active_publish_id, student_user_id);

ALTER TABLE assessment_attempt_answer
    DROP INDEX uk_assessment_attempt_answer_order,
    ADD COLUMN active_attempt_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN deleted = FALSE THEN attempt_id ELSE NULL END) STORED,
    ADD UNIQUE INDEX uk_assessment_attempt_answer_order (active_attempt_id, question_order);

ALTER TABLE assessment_publish_recipient
    DROP INDEX uk_assessment_publish_recipient_publish_student,
    ADD COLUMN active_publish_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN deleted = FALSE THEN publish_id ELSE NULL END) STORED,
    ADD UNIQUE INDEX uk_assessment_publish_recipient_publish_student (active_publish_id, student_user_id);

ALTER TABLE achievement
    DROP INDEX uk_achievement_owner_code,
    ADD COLUMN active_owner_user_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN deleted = FALSE THEN owner_user_id ELSE NULL END) STORED,
    ADD UNIQUE INDEX uk_achievement_owner_code (active_owner_user_id, achievement_code);

ALTER TABLE teaching_class_student
    DROP INDEX uk_teaching_class_student,
    ADD COLUMN active_student_user_id BIGINT
        GENERATED ALWAYS AS (CASE
            WHEN deleted = FALSE AND active = TRUE THEN student_user_id
            ELSE NULL
        END) STORED,
    ADD UNIQUE INDEX uk_teaching_class_student (teaching_class_id, active_student_user_id);

ALTER TABLE learning_profile_snapshot
    DROP INDEX uk_learning_profile_snapshot_scope_student,
    DROP INDEX uk_learning_profile_snapshot_scope_class,
    ADD COLUMN active_student_user_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN deleted = FALSE THEN student_user_id ELSE NULL END) STORED,
    ADD COLUMN active_teaching_class_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN deleted = FALSE THEN teaching_class_id ELSE NULL END) STORED,
    ADD UNIQUE INDEX uk_learning_profile_snapshot_scope_student (scope, active_student_user_id),
    ADD UNIQUE INDEX uk_learning_profile_snapshot_scope_class (scope, active_teaching_class_id);

ALTER TABLE analytics_daily_aggregate
    DROP INDEX uk_analytics_daily_aggregate,
    ADD COLUMN active_owner_user_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN deleted = FALSE THEN owner_user_id ELSE NULL END) STORED,
    ADD UNIQUE INDEX uk_analytics_daily_aggregate (
        active_owner_user_id,
        stat_date,
        source_type,
        aggregation_level,
        lexical_pair_id,
        lexical_pair_type,
        training_mode,
        context_support_level
    );

ALTER TABLE class_analytics_daily_aggregate
    DROP INDEX uk_class_analytics_daily_aggregate,
    ADD COLUMN active_teaching_class_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN deleted = FALSE THEN teaching_class_id ELSE NULL END) STORED,
    ADD UNIQUE INDEX uk_class_analytics_daily_aggregate (
        active_teaching_class_id,
        stat_date,
        source_type,
        aggregation_level,
        lexical_pair_id,
        lexical_pair_type,
        training_mode,
        context_support_level
    );

ALTER TABLE lexical_import_file
    DROP INDEX uk_lexical_import_file_batch,
    ADD COLUMN active_batch_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN deleted = FALSE THEN batch_id ELSE NULL END) STORED,
    ADD UNIQUE INDEX uk_lexical_import_file_batch (active_batch_id);

ALTER TABLE lexical_import_row
    DROP INDEX uk_lexical_import_row_batch_row,
    ADD COLUMN active_batch_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN deleted = FALSE THEN batch_id ELSE NULL END) STORED,
    ADD UNIQUE INDEX uk_lexical_import_row_batch_row (active_batch_id, import_row_number);
