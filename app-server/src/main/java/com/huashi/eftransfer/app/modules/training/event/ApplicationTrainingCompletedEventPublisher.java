package com.huashi.eftransfer.app.modules.training.event;

import org.springframework.context.ApplicationEventPublisher;

public class ApplicationTrainingCompletedEventPublisher implements TrainingCompletedEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public ApplicationTrainingCompletedEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(TrainingCompletedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
