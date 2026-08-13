-- Forward: start the public-assessment countdown only when answering begins.
-- Reverse: ALTER TABLE assessment_attempt DROP COLUMN answering_started_at;

ALTER TABLE `assessment_attempt`
  ADD COLUMN `answering_started_at` timestamp NULL DEFAULT NULL AFTER `started_at`;

-- Preserve the existing countdown state for historical and currently active attempts.
UPDATE `assessment_attempt`
SET `answering_started_at` = `started_at`
WHERE `answering_started_at` IS NULL;
