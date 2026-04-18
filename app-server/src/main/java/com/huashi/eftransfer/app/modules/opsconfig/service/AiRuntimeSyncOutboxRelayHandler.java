package com.huashi.eftransfer.app.modules.opsconfig.service;

import com.huashi.eftransfer.app.common.outbox.NonRetryableOutboxException;
import com.huashi.eftransfer.app.common.outbox.PlatformEventOutboxRecord;
import com.huashi.eftransfer.app.common.outbox.PlatformEventOutboxRelayHandler;
import com.huashi.eftransfer.app.integration.ai.client.AiGatewayCallResult;
import com.huashi.eftransfer.app.integration.ai.client.AiGatewayClient;
import com.huashi.eftransfer.app.modules.notification.service.NotificationScenarioService;
import com.huashi.eftransfer.app.modules.opsconfig.support.AiRuntimeSyncOutboxPayload;
import com.huashi.eftransfer.app.modules.opsconfig.support.AiRuntimeSyncOutboxSupport;
import com.huashi.eftransfer.app.modules.opsconfig.support.StoredAiOpsConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigApplyResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigPayload;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;

@Component
@Order(0)
public class AiRuntimeSyncOutboxRelayHandler implements PlatformEventOutboxRelayHandler {

    private final AiOpsConfigStorageService storageService;
    private final AiOpsConfigPayloadNormalizer payloadNormalizer;
    private final AiGatewayClient aiGatewayClient;
    private final NotificationScenarioService notificationScenarioService;
    private final ObjectMapper objectMapper;

    public AiRuntimeSyncOutboxRelayHandler(
            AiOpsConfigStorageService storageService,
            AiOpsConfigPayloadNormalizer payloadNormalizer,
            AiGatewayClient aiGatewayClient,
            NotificationScenarioService notificationScenarioService,
            ObjectMapper objectMapper
    ) {
        this.storageService = storageService;
        this.payloadNormalizer = payloadNormalizer;
        this.aiGatewayClient = aiGatewayClient;
        this.notificationScenarioService = notificationScenarioService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(PlatformEventOutboxRecord record) {
        return record != null && AiRuntimeSyncOutboxSupport.EVENT_TYPE.equals(record.eventType());
    }

    @Override
    public String relay(PlatformEventOutboxRecord record) {
        AiRuntimeSyncOutboxPayload payload = parsePayload(record.payloadJson());
        StoredAiOpsConfig stored = storageService.load()
                .orElseThrow(() -> new NonRetryableOutboxException("Stored AI ops config was not found"));
        if (stored.version() == null || payload.targetVersion() == null) {
            throw new NonRetryableOutboxException("AI runtime sync target version is missing");
        }
        if (stored.version() > payload.targetVersion()) {
            return "superseded_by_version_" + stored.version();
        }
        if (stored.version() < payload.targetVersion()) {
            throw new NonRetryableOutboxException("Stored AI ops config is older than the requested runtime sync version");
        }

        AiOpsConfigPayload normalizedPayload = payloadNormalizer.normalize(stored.config());
        AiGatewayCallResult<AiOpsConfigApplyResponse> result = aiGatewayClient.applyConfigResult(
                normalizedPayload,
                "DATABASE",
                stored.version()
        );
        if (!result.success() || result.data() == null) {
            if (result.retryable()) {
                throw new IllegalStateException(result.failureMessage());
            }
            throw new NonRetryableOutboxException(result.failureMessage());
        }
        return "runtime_applied_version_" + stored.version();
    }

    @Override
    public void afterRetryScheduled(
            PlatformEventOutboxRecord record,
            int nextAttemptCount,
            OffsetDateTime nextAttemptAt,
            Exception exception
    ) {
        if (record.attemptCount() == 0) {
            notificationScenarioService.notifyAiRuntimeSyncRetrying(targetVersion(record), exception.getMessage(), nextAttemptAt);
        }
    }

    @Override
    public void afterMovedToDlq(PlatformEventOutboxRecord record, int finalAttemptCount, Exception exception) {
        notificationScenarioService.notifyAiRuntimeSyncDlq(targetVersion(record), exception.getMessage());
    }

    @Override
    public void afterPublished(PlatformEventOutboxRecord record, boolean recoveredFromFailure, String detail) {
        if (recoveredFromFailure) {
            notificationScenarioService.notifyAiRuntimeSyncRecovered(targetVersion(record));
        }
    }

    private Long targetVersion(PlatformEventOutboxRecord record) {
        return parsePayload(record.payloadJson()).targetVersion();
    }

    private AiRuntimeSyncOutboxPayload parsePayload(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, AiRuntimeSyncOutboxPayload.class);
        } catch (Exception ex) {
            throw new NonRetryableOutboxException("Failed to parse AI runtime sync outbox payload", ex);
        }
    }
}
