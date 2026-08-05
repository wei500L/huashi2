package com.huashi.eftransfer.app.modules.assessment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AssessmentTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(AssessmentTimeoutScheduler.class);

    private final AssessmentService assessmentService;
    private final PublicAssessmentService publicAssessmentService;
    private final AssessmentTimeoutProperties assessmentTimeoutProperties;

    public AssessmentTimeoutScheduler(
            AssessmentService assessmentService,
            PublicAssessmentService publicAssessmentService,
            AssessmentTimeoutProperties assessmentTimeoutProperties
    ) {
        this.assessmentService = assessmentService;
        this.publicAssessmentService = publicAssessmentService;
        this.assessmentTimeoutProperties = assessmentTimeoutProperties;
    }

    @Scheduled(fixedDelayString = "#{@assessmentTimeoutProperties.pollInterval.toMillis()}")
    public void submitExpiredAttempts() {
        if (!assessmentTimeoutProperties.isEnabled()) {
            return;
        }
        int submittedCount = assessmentService.submitExpiredAttemptsBatch(assessmentTimeoutProperties.getBatchSize());
        int publicSubmittedCount = publicAssessmentService.submitExpiredAttemptsBatch(assessmentTimeoutProperties.getBatchSize());
        if (submittedCount > 0 || publicSubmittedCount > 0) {
            log.info("event=assessment_timeout_auto_submit classSubmittedCount={} publicSubmittedCount={}",
                    submittedCount, publicSubmittedCount);
        }
    }
}
