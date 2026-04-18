ALTER TABLE teaching_class_student
    ADD COLUMN active_student_user_id BIGINT
        GENERATED ALWAYS AS (
            CASE
                WHEN deleted = FALSE AND active = TRUE THEN student_user_id
                ELSE NULL
            END
        )
/*!80000 , ALGORITHM=INSTANT */;

ALTER TABLE teaching_class_student
    ADD UNIQUE INDEX uk_teaching_class_student_active (teaching_class_id, active_student_user_id)
/*!80000 , ALGORITHM=INPLACE, LOCK=NONE */;

ALTER TABLE teaching_class_student
    DROP INDEX uk_teaching_class_student
/*!80000 , ALGORITHM=INPLACE, LOCK=NONE */;
