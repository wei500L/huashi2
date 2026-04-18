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
SELECT DISTINCT p.id,
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
                  AND tcs.joined_at <= COALESCE(p.published_at, CURRENT_TIMESTAMP)
                  AND (tcs.left_at IS NULL OR tcs.left_at > COALESCE(p.published_at, CURRENT_TIMESTAMP))
WHERE p.deleted = FALSE
ON DUPLICATE KEY UPDATE
    updated_at = updated_at,
    updated_by = updated_by;
