-- Forward: allow multiple public-research attempts per participant.
-- Reverse: DROP INDEX uk_assessment_attempt_participant_no;
--          ALTER TABLE assessment_attempt DROP COLUMN active_participant_id, DROP COLUMN attempt_no;
--          ALTER TABLE assessment_public_release DROP COLUMN max_attempts;

ALTER TABLE `assessment_public_release`
  ADD COLUMN `max_attempts` int NOT NULL DEFAULT '1' AFTER `qr_entry_enabled`;

ALTER TABLE `assessment_attempt`
  ADD COLUMN `attempt_no` int NOT NULL DEFAULT '1' AFTER `submit_reason`,
  ADD COLUMN `active_participant_id` bigint GENERATED ALWAYS AS ((case when (`deleted` = false) then `participant_id` else NULL end)) STORED AFTER `active_publish_id`,
  ADD UNIQUE KEY `uk_assessment_attempt_participant_no` (`active_participant_id`,`attempt_no`);
