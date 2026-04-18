package com.huashi.eftransfer.app.common.session;

import com.huashi.eftransfer.app.modules.diagnosis.service.DiagnosisSessionCompletionService;
import com.huashi.eftransfer.app.modules.diagnosis.service.DiagnosisSessionService;
import com.huashi.eftransfer.app.modules.training.service.TrainingSessionCompletionService;
import com.huashi.eftransfer.app.modules.training.service.TrainingSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SessionLifecycleScheduler {

    private static final Logger log = LoggerFactory.getLogger(SessionLifecycleScheduler.class);

    private final SessionLifecycleProperties sessionLifecycleProperties;
    private final DiagnosisSessionService diagnosisSessionService;
    private final TrainingSessionService trainingSessionService;
    private final DiagnosisSessionCompletionService diagnosisSessionCompletionService;
    private final TrainingSessionCompletionService trainingSessionCompletionService;

    public SessionLifecycleScheduler(
            SessionLifecycleProperties sessionLifecycleProperties,
            DiagnosisSessionService diagnosisSessionService,
            TrainingSessionService trainingSessionService,
            DiagnosisSessionCompletionService diagnosisSessionCompletionService,
            TrainingSessionCompletionService trainingSessionCompletionService
    ) {
        this.sessionLifecycleProperties = sessionLifecycleProperties;
        this.diagnosisSessionService = diagnosisSessionService;
        this.trainingSessionService = trainingSessionService;
        this.diagnosisSessionCompletionService = diagnosisSessionCompletionService;
        this.trainingSessionCompletionService = trainingSessionCompletionService;
    }

    @Scheduled(fixedDelayString = "#{@sessionLifecycleProperties.pollInterval.toMillis()}")
    public void pollLifecycleTasks() {
        if (!sessionLifecycleProperties.isEnabled()) {
            return;
        }
        LocalDateTime abandonBefore = LocalDateTime.now().minus(sessionLifecycleProperties.getAbandonTimeout());
        int batchSize = sessionLifecycleProperties.getBatchSize();
        int diagnosisAbandoned = diagnosisSessionService.abandonTimedOutSessions(abandonBefore, batchSize);
        int trainingAbandoned = trainingSessionService.abandonTimedOutSessions(abandonBefore, batchSize);
        diagnosisSessionCompletionService.retryPendingCompletions(batchSize);
        trainingSessionCompletionService.retryPendingCompletions(batchSize);
        if (diagnosisAbandoned > 0 || trainingAbandoned > 0) {
            log.info("event=session_lifecycle_abandoned diagnosisCount={} trainingCount={}", diagnosisAbandoned, trainingAbandoned);
        }
    }
}
