ALTER TABLE training_plan
    ADD COLUMN active_owner_user_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN deleted = FALSE THEN owner_user_id ELSE NULL END)
/*!80000 , ALGORITHM=INSTANT */;

ALTER TABLE training_plan
    ADD UNIQUE INDEX uk_training_plan_owner_summary_active (active_owner_user_id, source_diagnosis_summary_id)
/*!80000 , ALGORITHM=INPLACE, LOCK=NONE */;

ALTER TABLE training_plan
    DROP INDEX uk_training_plan_owner_summary
/*!80000 , ALGORITHM=INPLACE, LOCK=NONE */;

ALTER TABLE wrong_book
    ADD COLUMN active_owner_user_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN deleted = FALSE THEN owner_user_id ELSE NULL END)
/*!80000 , ALGORITHM=INSTANT */;

ALTER TABLE wrong_book
    ADD UNIQUE INDEX uk_wrong_book_owner_pair_active (active_owner_user_id, lexical_pair_id)
/*!80000 , ALGORITHM=INPLACE, LOCK=NONE */;

ALTER TABLE wrong_book
    DROP INDEX uk_wrong_book_owner_pair
/*!80000 , ALGORITHM=INPLACE, LOCK=NONE */;

ALTER TABLE achievement
    ADD COLUMN active_owner_user_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN deleted = FALSE THEN owner_user_id ELSE NULL END)
/*!80000 , ALGORITHM=INSTANT */;

ALTER TABLE achievement
    ADD UNIQUE INDEX uk_achievement_owner_code_active (active_owner_user_id, achievement_code)
/*!80000 , ALGORITHM=INPLACE, LOCK=NONE */;

ALTER TABLE achievement
    DROP INDEX uk_achievement_owner_code
/*!80000 , ALGORITHM=INPLACE, LOCK=NONE */;
