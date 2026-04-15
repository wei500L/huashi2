ALTER TABLE student_profile
    ADD COLUMN daily_training_target INT NULL AFTER composite_score,
    ADD COLUMN weekly_accuracy_target INT NULL AFTER daily_training_target;
