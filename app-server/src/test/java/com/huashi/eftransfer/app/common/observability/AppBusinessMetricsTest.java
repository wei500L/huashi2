package com.huashi.eftransfer.app.common.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppBusinessMetricsTest {

    @Test
    void shouldRecordCoreBusinessCounters() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AppBusinessMetrics metrics = new AppBusinessMetrics(registry);

        metrics.recordLoginAttempt("success", "success");
        metrics.recordLoginAttempt("failure", "TOKEN_INVALID");
        metrics.recordDiagnosisSessionCreated();
        metrics.recordTrainingSessionStarted();

        assertThat(registry.get("app.auth.login.attempts").tag("outcome", "success").tag("reason", "success").counter().count()).isEqualTo(1.0d);
        assertThat(registry.get("app.auth.login.attempts").tag("outcome", "failure").tag("reason", "TOKEN_INVALID").counter().count()).isEqualTo(1.0d);
        assertThat(registry.get("app.diagnosis.sessions.created").counter().count()).isEqualTo(1.0d);
        assertThat(registry.get("app.training.sessions.started").counter().count()).isEqualTo(1.0d);
    }
}
