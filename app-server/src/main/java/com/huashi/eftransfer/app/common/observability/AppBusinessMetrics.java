package com.huashi.eftransfer.app.common.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class AppBusinessMetrics {

    private final MeterRegistry meterRegistry;

    public AppBusinessMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordLoginAttempt(String outcome, String reason) {
        meterRegistry.counter(
                "app.auth.login.attempts",
                "outcome", normalize(outcome),
                "reason", normalize(reason)
        ).increment();
    }

    public void recordDiagnosisSessionCreated() {
        meterRegistry.counter("app.diagnosis.sessions.created").increment();
    }

    public void recordTrainingSessionStarted() {
        meterRegistry.counter("app.training.sessions.started").increment();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value;
    }
}
