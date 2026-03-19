package com.huashi.eftransfer.app.modules.analytics.event;

import com.huashi.eftransfer.app.modules.analytics.service.AnalyticsAggregationService;
import com.huashi.eftransfer.app.modules.diagnosis.event.DiagnosisCompletedEvent;
import com.huashi.eftransfer.app.modules.training.event.TrainingCompletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsCompletedEventListener {

    private final AnalyticsAggregationService analyticsAggregationService;

    public AnalyticsCompletedEventListener(AnalyticsAggregationService analyticsAggregationService) {
        this.analyticsAggregationService = analyticsAggregationService;
    }

    @EventListener
    public void onDiagnosisCompleted(DiagnosisCompletedEvent event) {
        analyticsAggregationService.aggregateFromDiagnosisSummary(event.summaryId());
    }

    @EventListener
    public void onTrainingCompleted(TrainingCompletedEvent event) {
        analyticsAggregationService.aggregateFromTrainingSession(event.sessionId());
    }
}
