package com.huashi.eftransfer.ai.modules.rag.service;

import com.huashi.eftransfer.ai.common.runtime.AiRuntimeBundle;
import com.huashi.eftransfer.ai.common.runtime.AiRuntimeConfigService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Service
public class RagSchemaStartupGuard {

    private final AiRuntimeConfigService runtimeConfigService;
    private final RagSchemaDimensionGuard ragSchemaDimensionGuard;

    public RagSchemaStartupGuard(
            AiRuntimeConfigService runtimeConfigService,
            RagSchemaDimensionGuard ragSchemaDimensionGuard
    ) {
        this.runtimeConfigService = runtimeConfigService;
        this.ragSchemaDimensionGuard = ragSchemaDimensionGuard;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(Ordered.LOWEST_PRECEDENCE)
    public void verifyOnStartup() {
        AiRuntimeBundle bundle = runtimeConfigService.current();
        ragSchemaDimensionGuard.verifyConfig(bundle == null ? null : bundle.config());
    }
}
