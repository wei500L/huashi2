-- Forward: align V1 instruction copy with the 60-minute answering window.
-- Reverse: replace "约 60 分钟" with "约 40 分钟" in the same rows.

UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
JOIN assessment_questionnaire_version v ON v.id = i.questionnaire_version_id
JOIN assessment_questionnaire aq ON aq.id = v.questionnaire_id
SET q.stem_text = REPLACE(q.stem_text, '约 40 分钟', '约 60 分钟')
WHERE aq.questionnaire_code = 'LEXIBRIDGE_RESEARCH_V1'
  AND i.item_code = 'BASIC-INSTRUCTION'
  AND q.question_type = 'INSTRUCTION'
  AND q.deleted = FALSE
  AND i.deleted = FALSE
  AND q.stem_text LIKE '%约 40 分钟%';

UPDATE assessment_attempt_answer a
JOIN assessment_attempt t ON t.id = a.attempt_id
JOIN assessment_question q ON q.id = a.question_id
SET a.stem_text_snapshot = REPLACE(a.stem_text_snapshot, '约 40 分钟', '约 60 分钟')
WHERE t.status = 'IN_PROGRESS'
  AND t.deleted = FALSE
  AND a.deleted = FALSE
  AND a.question_type = 'INSTRUCTION'
  AND a.stem_text_snapshot LIKE '%约 40 分钟%';
