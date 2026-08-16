-- Forward: show V/F (not 正确/错误) on V1 true/false-with-justification options.
-- Reverse: JSON_REPLACE the same paths back to 正确 / 错误.

UPDATE assessment_question q
JOIN assessment_questionnaire_item i ON i.assessment_question_id = q.id
JOIN assessment_questionnaire_version v ON v.id = i.questionnaire_version_id
JOIN assessment_questionnaire aq ON aq.id = v.questionnaire_id
SET q.options_json = JSON_REPLACE(q.options_json, '$[0].label', 'V', '$[1].label', 'F')
WHERE aq.questionnaire_code = 'LEXIBRIDGE_RESEARCH_V1'
  AND q.question_type = 'TRUE_FALSE_WITH_JUSTIFICATION'
  AND q.deleted = FALSE
  AND i.deleted = FALSE
  AND UPPER(JSON_UNQUOTE(JSON_EXTRACT(q.options_json, '$[0].key'))) = 'V'
  AND UPPER(JSON_UNQUOTE(JSON_EXTRACT(q.options_json, '$[1].key'))) = 'F';

UPDATE assessment_question_version v
JOIN assessment_question_bank b ON b.id = v.question_bank_id
SET v.options_json = JSON_REPLACE(v.options_json, '$[0].label', 'V', '$[1].label', 'F')
WHERE b.bank_code = 'LEXIBRIDGE_SHARED'
  AND v.question_code LIKE 'P4T3-%'
  AND v.question_type = 'TRUE_FALSE_WITH_JUSTIFICATION'
  AND v.deleted = FALSE
  AND UPPER(JSON_UNQUOTE(JSON_EXTRACT(v.options_json, '$[0].key'))) = 'V'
  AND UPPER(JSON_UNQUOTE(JSON_EXTRACT(v.options_json, '$[1].key'))) = 'F';

UPDATE assessment_attempt_answer a
JOIN assessment_attempt t ON t.id = a.attempt_id
JOIN assessment_publish p ON p.id = t.publish_id
JOIN assessment_paper paper ON paper.id = p.paper_id
SET a.options_json_snapshot = JSON_REPLACE(a.options_json_snapshot, '$[0].label', 'V', '$[1].label', 'F')
WHERE paper.paper_code = 'LEXIBRIDGE_RESEARCH_V1'
  AND t.status = 'IN_PROGRESS'
  AND t.deleted = FALSE
  AND a.deleted = FALSE
  AND a.question_type = 'TRUE_FALSE_WITH_JUSTIFICATION'
  AND UPPER(JSON_UNQUOTE(JSON_EXTRACT(a.options_json_snapshot, '$[0].key'))) = 'V'
  AND UPPER(JSON_UNQUOTE(JSON_EXTRACT(a.options_json_snapshot, '$[1].key'))) = 'F';
