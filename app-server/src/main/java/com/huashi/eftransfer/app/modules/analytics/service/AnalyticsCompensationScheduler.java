package com.huashi.eftransfer.app.modules.analytics.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@ConditionalOnProperty(value = "app.analytics.compensation.enabled", havingValue = "true", matchIfMissing = true)
public class AnalyticsCompensationScheduler {

    private final AnalyticsAggregationService analyticsAggregationService;

    public AnalyticsCompensationScheduler(AnalyticsAggregationService analyticsAggregationService) {
        this.analyticsAggregationService = analyticsAggregationService;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void rebuildRecentWindow() {
        LocalDate endDate = LocalDate.now();
        analyticsAggregationService.rebuildRange(endDate.minusDays(30), endDate);
    }
}
