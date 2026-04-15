package com.huashi.eftransfer.app.modules.achievement.event;

import com.huashi.eftransfer.app.modules.achievement.service.AchievementService;
import com.huashi.eftransfer.app.modules.diagnosis.event.DiagnosisCompletedEvent;
import com.huashi.eftransfer.app.modules.training.event.TrainingCompletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AchievementEventListener {

    private final AchievementService achievementService;

    public AchievementEventListener(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    @EventListener
    public void onDiagnosisCompleted(DiagnosisCompletedEvent event) {
        achievementService.refreshAchievementsForUser(event.ownerUserId());
    }

    @EventListener
    public void onTrainingCompleted(TrainingCompletedEvent event) {
        achievementService.refreshAchievementsForUser(event.ownerUserId());
    }
}
