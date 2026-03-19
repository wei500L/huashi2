package com.huashi.eftransfer.app.modules.training.event;

public interface TrainingCompletedEventPublisher {

    void publish(TrainingCompletedEvent event);
}
