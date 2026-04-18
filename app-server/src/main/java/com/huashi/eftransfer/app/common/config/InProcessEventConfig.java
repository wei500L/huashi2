package com.huashi.eftransfer.app.common.config;

import com.huashi.eftransfer.app.modules.diagnosis.event.DiagnosisCompletedEventPublisher;
import com.huashi.eftransfer.app.modules.training.event.ApplicationTrainingCompletedEventPublisher;
import com.huashi.eftransfer.app.modules.training.event.TrainingCompletedEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InProcessEventConfig {

    @Bean
    public DiagnosisCompletedEventPublisher diagnosisCompletedEventPublisher(
            ApplicationEventPublisher applicationEventPublisher
    ) {
        return applicationEventPublisher::publishEvent;
    }

    @Bean
    public TrainingCompletedEventPublisher trainingCompletedEventPublisher(
            ApplicationEventPublisher applicationEventPublisher
    ) {
        return new ApplicationTrainingCompletedEventPublisher(applicationEventPublisher);
    }
}
