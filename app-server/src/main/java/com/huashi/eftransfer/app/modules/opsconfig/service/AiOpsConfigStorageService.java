package com.huashi.eftransfer.app.modules.opsconfig.service;

import com.huashi.eftransfer.app.modules.opsconfig.entity.AiOpsConfigEntity;
import com.huashi.eftransfer.app.modules.opsconfig.mapper.AiOpsConfigMapper;
import com.huashi.eftransfer.app.modules.opsconfig.support.StoredAiOpsConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigPayload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
public class AiOpsConfigStorageService {

    public static final String CONFIG_KEY = "AI_GATEWAY_RUNTIME";

    private final AiOpsConfigMapper aiOpsConfigMapper;
    private final AiOpsConfigCryptoService cryptoService;
    private final ObjectMapper objectMapper;

    public AiOpsConfigStorageService(
            AiOpsConfigMapper aiOpsConfigMapper,
            AiOpsConfigCryptoService cryptoService,
            ObjectMapper objectMapper
    ) {
        this.aiOpsConfigMapper = aiOpsConfigMapper;
        this.cryptoService = cryptoService;
        this.objectMapper = objectMapper;
    }

    public Optional<StoredAiOpsConfig> load() {
        AiOpsConfigEntity entity = aiOpsConfigMapper.selectByConfigKey(CONFIG_KEY);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(toStoredConfig(entity));
    }

    @Transactional
    public StoredAiOpsConfig save(AiOpsConfigPayload payload, Long actorUserId) {
        AiOpsConfigEntity existing = aiOpsConfigMapper.selectByConfigKey(CONFIG_KEY);
        AiOpsConfigEntity entity = existing == null ? new AiOpsConfigEntity() : existing;
        entity.setConfigKey(CONFIG_KEY);
        entity.setEncryptedConfig(cryptoService.encrypt(writeJson(payload)));
        entity.setVersionNumber(existing == null ? 1L : existing.getVersionNumber() + 1L);
        if (actorUserId != null) {
            entity.setUpdatedBy(actorUserId);
            if (existing == null) {
                entity.setCreatedBy(actorUserId);
            }
        }

        if (existing == null) {
            aiOpsConfigMapper.insert(entity);
        } else {
            aiOpsConfigMapper.updateById(entity);
        }
        return toStoredConfig(entity);
    }

    private StoredAiOpsConfig toStoredConfig(AiOpsConfigEntity entity) {
        return new StoredAiOpsConfig(
                readJson(cryptoService.decrypt(entity.getEncryptedConfig())),
                entity.getVersionNumber(),
                entity.getUpdatedAt() == null ? null : entity.getUpdatedAt().atOffset(ZoneOffset.UTC)
        );
    }

    private String writeJson(AiOpsConfigPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize AI ops config", ex);
        }
    }

    private AiOpsConfigPayload readJson(String value) {
        try {
            return objectMapper.readValue(value, AiOpsConfigPayload.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to deserialize AI ops config", ex);
        }
    }
}
