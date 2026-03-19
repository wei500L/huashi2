package com.huashi.eftransfer.app.modules.diagnosis.event;

import org.springframework.context.ApplicationEventPublisher;

public class CompositeDiagnosisCompletedEventPublisher implements DiagnosisCompletedEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final DiagnosisCompletedEventPublisher rabbitPublisher;
    private final boolean rabbitPublishEnabled;

    public CompositeDiagnosisCompletedEventPublisher(
            ApplicationEventPublisher applicationEventPublisher,
            DiagnosisCompletedEventPublisher rabbitPublisher,
            boolean rabbitPublishEnabled
    ) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.rabbitPublisher = rabbitPublisher;
        this.rabbitPublishEnabled = rabbitPublishEnabled;
    }

    @Override
    public void publish(DiagnosisCompletedEvent event) {
        applicationEventPublisher.publishEvent(event);
        if (rabbitPublishEnabled) {
            rabbitPublisher.publish(event);
        }
    }
}
