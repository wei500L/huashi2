ALTER TABLE assessment_attempt
    ADD COLUMN active_publish_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN deleted = FALSE THEN publish_id ELSE NULL END)
/*!80000 , ALGORITHM=INSTANT */;

ALTER TABLE assessment_attempt
    ADD UNIQUE INDEX uk_assessment_attempt_publish_student_active (active_publish_id, student_user_id)
/*!80000 , ALGORITHM=INPLACE, LOCK=NONE */;

ALTER TABLE assessment_attempt
    DROP INDEX uk_assessment_attempt_publish_student
/*!80000 , ALGORITHM=INPLACE, LOCK=NONE */;

ALTER TABLE assessment_attempt_answer
    ADD COLUMN active_attempt_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN deleted = FALSE THEN attempt_id ELSE NULL END)
/*!80000 , ALGORITHM=INSTANT */;

ALTER TABLE assessment_attempt_answer
    ADD UNIQUE INDEX uk_assessment_attempt_answer_order_active (active_attempt_id, question_order)
/*!80000 , ALGORITHM=INPLACE, LOCK=NONE */;

ALTER TABLE assessment_attempt_answer
    DROP INDEX uk_assessment_attempt_answer_order
/*!80000 , ALGORITHM=INPLACE, LOCK=NONE */;

ALTER TABLE assessment_publish_recipient
    ADD COLUMN active_publish_id BIGINT
        GENERATED ALWAYS AS (CASE WHEN deleted = FALSE THEN publish_id ELSE NULL END)
/*!80000 , ALGORITHM=INSTANT */;

ALTER TABLE assessment_publish_recipient
    ADD UNIQUE INDEX uk_assessment_publish_recipient_publish_student_active (active_publish_id, student_user_id)
/*!80000 , ALGORITHM=INPLACE, LOCK=NONE */;

ALTER TABLE assessment_publish_recipient
    DROP INDEX uk_assessment_publish_recipient_publish_student
/*!80000 , ALGORITHM=INPLACE, LOCK=NONE */;
