package com.huashi.eftransfer.app.modules.opsconfig.service;

import com.huashi.eftransfer.app.modules.opsconfig.entity.AiOpsConfigEntity;
import com.huashi.eftransfer.app.modules.opsconfig.entity.AiOpsConfigHistoryEntity;
import com.huashi.eftransfer.app.modules.opsconfig.mapper.AiOpsConfigMapper;
import com.huashi.eftransfer.app.modules.opsconfig.mapper.AiOpsConfigHistoryMapper;
import com.huashi.eftransfer.app.modules.opsconfig.support.StoredAiOpsConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigPayload;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.springframework.dao.DuplicateKeyException;
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
    private final AiOpsConfigHistoryMapper aiOpsConfigHistoryMapper;
    private final AiOpsConfigCryptoService cryptoService;
    private final ObjectMapper objectMapper;

    public AiOpsConfigStorageService(
            AiOpsConfigMapper aiOpsConfigMapper,
            AiOpsConfigHistoryMapper aiOpsConfigHistoryMapper,
            AiOpsConfigCryptoService cryptoService,
            ObjectMapper objectMapper
    ) {
        this.aiOpsConfigMapper = aiOpsConfigMapper;
        this.aiOpsConfigHistoryMapper = aiOpsConfigHistoryMapper;
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
    public StoredAiOpsConfig save(AiOpsConfigPayload payload, Long expectedVersion, Long version, Long actorUserId) {
        AiOpsConfigEntity existing = aiOpsConfigMapper.selectByConfigKey(CONFIG_KEY);
        String encryptedConfig = encryptPayload(payload);
        Long nextVersion = version == null ? (existing == null ? 1L : existing.getVersionNumber() + 1L) : version;
        if (expectedVersion == null) {
            if (existing != null) {
                throw versionConflict();
            }
            AiOpsConfigEntity entity = new AiOpsConfigEntity();
            entity.setConfigKey(CONFIG_KEY);
            entity.setEncryptedConfig(encryptedConfig);
            entity.setVersionNumber(nextVersion);
            if (actorUserId != null) {
                entity.setCreatedBy(actorUserId);
                entity.setUpdatedBy(actorUserId);
            }
            try {
                aiOpsConfigMapper.insert(entity);
            } catch (DuplicateKeyException ex) {
                throw versionConflict();
            }
        } else {
            if (existing == null) {
                throw versionConflict();
            }
            int updated = aiOpsConfigMapper.updateByConfigKeyAndVersion(
                    CONFIG_KEY,
                    encryptedConfig,
                    nextVersion,
                    expectedVersion,
                    actorUserId
            );
            if (updated == 0) {
                throw versionConflict();
            }
        }
        AiOpsConfigEntity refreshed = aiOpsConfigMapper.selectByConfigKey(CONFIG_KEY);
        if (refreshed == null) {
            throw new IllegalStateException("Failed to reload stored AI ops config");
        }
        return toStoredConfig(refreshed);
    }

    public void saveHistory(AiOpsConfigPayload payload, Long version, Long previousVersion, Long actorUserId, Object changeSummary) {
        AiOpsConfigHistoryEntity entity = new AiOpsConfigHistoryEntity();
        entity.setConfigKey(CONFIG_KEY);
        entity.setVersionNumber(version);
        entity.setPreviousVersionNumber(previousVersion);
        entity.setEncryptedConfig(encryptPayload(payload));
        entity.setChangeSummaryJson(writeJson(changeSummary));
        entity.setCreatedBy(actorUserId);
        entity.setUpdatedBy(actorUserId);
        aiOpsConfigHistoryMapper.insert(entity);
    }

    private StoredAiOpsConfig toStoredConfig(AiOpsConfigEntity entity) {
        return new StoredAiOpsConfig(
                readJson(cryptoService.decrypt(entity.getEncryptedConfig())),
                entity.getVersionNumber(),
                entity.getUpdatedAt() == null ? null : entity.getUpdatedAt().atOffset(ZoneOffset.UTC)
        );
    }

    private String writeJson(AiOpsConfigPayload payload) {
        return writeJson((Object) payload);
    }

    private String writeJson(Object payload) {
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

    private String encryptPayload(AiOpsConfigPayload payload) {
        return cryptoService.encrypt(writeJson(payload));
    }

    private BusinessException versionConflict() {
        return new BusinessException(
                ResultCode.BAD_REQUEST,
                "AI ops config was updated by another administrator. Refresh the page and retry.",
                409
        );
    }
}
