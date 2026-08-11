-- Idempotent production schema upgrade for LEXIBRIDGE_RESEARCH_V2.
-- Run against the application database before deploying the V2 application.

DROP PROCEDURE IF EXISTS add_lexibridge_presentation_column;

DELIMITER //
CREATE PROCEDURE add_lexibridge_presentation_column(IN target_table VARCHAR(64))
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = target_table
          AND column_name = 'presentation_json'
    ) THEN
        SET @statement = CONCAT(
            'ALTER TABLE `', target_table,
            '` ADD COLUMN `presentation_json` longtext NULL'
        );
        PREPARE schema_statement FROM @statement;
        EXECUTE schema_statement;
        DEALLOCATE PREPARE schema_statement;
    END IF;
END//
DELIMITER ;

CALL add_lexibridge_presentation_column('assessment_question');
CALL add_lexibridge_presentation_column('assessment_question_version');
CALL add_lexibridge_presentation_column('assessment_questionnaire_item');

DROP PROCEDURE add_lexibridge_presentation_column;
