CREATE TABLE IF NOT EXISTS assessment_publish_recipient
(
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    publish_id        BIGINT    NOT NULL,
    paper_id          BIGINT    NOT NULL,
    teaching_class_id BIGINT    NOT NULL,
    student_user_id   BIGINT    NOT NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by        BIGINT    NULL,
    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by        BIGINT    NULL,
    deleted           BOOLEAN   NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_assessment_publish_recipient_publish_id FOREIGN KEY (publish_id) REFERENCES assessment_publish (id),
    CONSTRAINT fk_assessment_publish_recipient_paper_id FOREIGN KEY (paper_id) REFERENCES assessment_paper (id),
    CONSTRAINT fk_assessment_publish_recipient_class_id FOREIGN KEY (teaching_class_id) REFERENCES teaching_class (id),
    CONSTRAINT fk_assessment_publish_recipient_student_user_id FOREIGN KEY (student_user_id) REFERENCES users (id),
    CONSTRAINT uk_assessment_publish_recipient_publish_student UNIQUE (publish_id, student_user_id, deleted)
);

INSERT INTO assessment_publish_recipient (
    publish_id,
    paper_id,
    teaching_class_id,
    student_user_id,
    created_at,
    created_by,
    updated_at,
    updated_by,
    deleted
)
SELECT p.id,
       p.paper_id,
       p.teaching_class_id,
       tcs.student_user_id,
       COALESCE(p.published_at, CURRENT_TIMESTAMP),
       p.published_by,
       COALESCE(p.published_at, CURRENT_TIMESTAMP),
       p.published_by,
       FALSE
FROM assessment_publish p
         JOIN teaching_class_student tcs
              ON tcs.teaching_class_id = p.teaching_class_id
                  AND tcs.deleted = FALSE
                  AND tcs.active = TRUE
                  AND tcs.joined_at <= COALESCE(p.published_at, CURRENT_TIMESTAMP)
                  AND (tcs.left_at IS NULL OR tcs.left_at > COALESCE(p.published_at, CURRENT_TIMESTAMP))
WHERE p.deleted = FALSE
  AND NOT EXISTS(
        SELECT 1
        FROM assessment_publish_recipient existing
        WHERE existing.publish_id = p.id
          AND existing.student_user_id = tcs.student_user_id
          AND existing.deleted = FALSE
    );

CREATE INDEX idx_assessment_publish_recipient_publish ON assessment_publish_recipient (publish_id, student_user_id);
CREATE INDEX idx_assessment_publish_recipient_student ON assessment_publish_recipient (student_user_id, publish_id);
