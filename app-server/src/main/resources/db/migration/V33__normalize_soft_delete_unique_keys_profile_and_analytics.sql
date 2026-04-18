ALTER TABLE learning_profile_snapshot
    ADD COLUMN active_student_user_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN deleted = FALSE THEN student_user_id ELSE NULL END)
/*!80000 , ALGORITHM=INSTANT */;

ALTER TABLE learning_profile_snapshot
    ADD COLUMN active_teaching_class_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN deleted = FALSE THEN teaching_class_id ELSE NULL END)
/*!80000 , ALGORITHM=INSTANT */;

ALTER TABLE learning_profile_snapshot
    ADD UNIQUE INDEX uk_learning_profile_snapshot_scope_student_active (scope, active_student_user_id)
/*!80000 , ALGORITHM=INPLACE, LOCK=NONE */;

ALTER TABLE learning_profile_snapshot
    ADD UNIQUE INDEX uk_learning_profile_snapshot_scope_class_active (scope, active_teaching_class_id)
/*!80000 , ALGORITHM=INPLACE, LOCK=NONE */;

ALTER TABLE learning_profile_snapshot
    DROP INDEX uk_learning_profile_snapshot_scope_student
/*!80000 , ALGORITHM=INPLACE, LOCK=NONE */;

ALTER TABLE learning_profile_snapshot
    DROP INDEX uk_learning_profile_snapshot_scope_class
/*!80000 , ALGORITHM=INPLACE, LOCK=NONE */;

ALTER TABLE analytics_daily_aggregate
    ADD COLUMN active_owner_user_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN deleted = FALSE THEN owner_user_id ELSE NULL END)
/*!80000 , ALGORITHM=INSTANT */;

ALTER TABLE analytics_daily_aggregate
    ADD UNIQUE INDEX uk_analytics_daily_aggregate_active (
        active_owner_user_id,
        stat_date,
        source_type,
        aggregation_level,
        lexical_pair_id,
        lexical_pair_type,
        training_mode,
        context_support_level
    )
/*!80000 , ALGORITHM=INPLACE, LOCK=NONE */;

ALTER TABLE analytics_daily_aggregate
    DROP INDEX uk_analytics_daily_aggregate
/*!80000 , ALGORITHM=INPLACE, LOCK=NONE */;

ALTER TABLE class_analytics_daily_aggregate
    ADD COLUMN active_teaching_class_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN deleted = FALSE THEN teaching_class_id ELSE NULL END)
/*!80000 , ALGORITHM=INSTANT */;

ALTER TABLE class_analytics_daily_aggregate
    ADD UNIQUE INDEX uk_class_analytics_daily_aggregate_active (
        active_teaching_class_id,
        stat_date,
        source_type,
        aggregation_level,
        lexical_pair_id,
        lexical_pair_type,
        training_mode,
        context_support_level
    )
/*!80000 , ALGORITHM=INPLACE, LOCK=NONE */;

ALTER TABLE class_analytics_daily_aggregate
    DROP INDEX uk_class_analytics_daily_aggregate
/*!80000 , ALGORITHM=INPLACE, LOCK=NONE */;
