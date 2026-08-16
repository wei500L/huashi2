-- Forward: restore V1 expected-time copy and answering window to 40 minutes.
-- Does not rewrite in-progress attempt expires_at; already-started sessions keep their original countdown.
-- Reverse: replace "约 40 分钟" with "约 60 分钟" in the same instruction rows,
--          and set LEXIBRIDGE_RESEARCH_V1 paper/publish duration_minutes back to 60.

UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
JOIN assessment_questionnaire_version v ON v.id = i.questionnaire_version_id
JOIN assessment_questionnaire aq ON aq.id = v.questionnaire_id
SET q.stem_text = REPLACE(q.stem_text, '约 60 分钟', '约 40 分钟')
WHERE aq.questionnaire_code = 'LEXIBRIDGE_RESEARCH_V1'
  AND i.item_code = 'BASIC-INSTRUCTION'
  AND q.question_type = 'INSTRUCTION'
  AND q.deleted = FALSE
  AND i.deleted = FALSE
  AND q.stem_text LIKE '%约 60 分钟%';

UPDATE assessment_question_version v
JOIN assessment_question_bank b ON b.id = v.question_bank_id
SET v.stem_text = REPLACE(v.stem_text, '约 60 分钟', '约 40 分钟')
WHERE b.bank_code = 'LEXIBRIDGE_SHARED'
  AND v.question_code = 'BASIC-INSTRUCTION'
  AND v.deleted = FALSE
  AND v.stem_text LIKE '%约 60 分钟%';

UPDATE assessment_attempt_answer a
JOIN assessment_attempt t ON t.id = a.attempt_id
JOIN assessment_question q ON q.id = a.question_id
SET a.stem_text_snapshot = REPLACE(a.stem_text_snapshot, '约 60 分钟', '约 40 分钟')
WHERE t.status = 'IN_PROGRESS'
  AND t.deleted = FALSE
  AND a.deleted = FALSE
  AND a.question_type = 'INSTRUCTION'
  AND a.stem_text_snapshot LIKE '%约 60 分钟%';

UPDATE assessment_paper
SET duration_minutes = 40
WHERE paper_code = 'LEXIBRIDGE_RESEARCH_V1'
  AND deleted = FALSE
  AND duration_minutes = 60;

UPDATE assessment_publish p
JOIN assessment_paper paper ON paper.id = p.paper_id
SET p.duration_minutes = 40
WHERE paper.paper_code = 'LEXIBRIDGE_RESEARCH_V1'
  AND paper.deleted = FALSE
  AND p.deleted = FALSE
  AND p.duration_minutes = 60;
