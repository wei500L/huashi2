package com.huashi.eftransfer.app.modules.diagnosis.event;

public interface DiagnosisCompletedEventPublisher {

    void publish(DiagnosisCompletedEvent event);
}
