package com.huashi.eftransfer.app.modules.opsconfig.service;

import com.huashi.eftransfer.app.modules.opsconfig.support.AiOpsConfigChangeSet;
import com.huashi.eftransfer.app.modules.opsconfig.support.AiOpsConfigDiffEntry;
import com.huashi.eftransfer.app.modules.opsconfig.support.AiOpsSecretChangeEntry;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigPayload;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class AiOpsConfigChangeSummaryService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public AiOpsConfigChangeSummaryService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AiOpsConfigChangeSet summarize(AiOpsConfigPayload before, AiOpsConfigPayload after, AiOpsConfigPayload sanitizedBefore, AiOpsConfigPayload sanitizedAfter) {
        return new AiOpsConfigChangeSet(
                collectConfigDiffs(toMap(sanitizedBefore), toMap(sanitizedAfter), "config"),
                collectSecretChanges(before, after)
        );
    }

    private List<AiOpsConfigDiffEntry> collectConfigDiffs(Object base, Object next, String path) {
        if (base instanceof Map<?, ?> baseMap && next instanceof Map<?, ?> nextMap) {
            Set<String> keys = new LinkedHashSet<>();
            baseMap.keySet().forEach(key -> keys.add(String.valueOf(key)));
            nextMap.keySet().forEach(key -> keys.add(String.valueOf(key)));
            return keys.stream()
                    .sorted()
                    .flatMap(key -> collectConfigDiffs(baseMap.get(key), nextMap.get(key), path + "." + key).stream())
                    .toList();
        }
        String before = formatDiffValue(base);
        String after = formatDiffValue(next);
        return Objects.equals(before, after) ? List.of() : List.of(new AiOpsConfigDiffEntry(path, before, after));
    }

    private List<AiOpsSecretChangeEntry> collectSecretChanges(AiOpsConfigPayload before, AiOpsConfigPayload after) {
        List<AiOpsSecretChangeEntry> changes = new ArrayList<>();
        Map<String, Object> beforeMap = toMap(before);
        Map<String, Object> afterMap = toMap(after);
        Map<String, Object> beforeProviders = nestedMap(beforeMap, "provider", "providers");
        Map<String, Object> afterProviders = nestedMap(afterMap, "provider", "providers");
        Set<String> providerNames = new LinkedHashSet<>();
        providerNames.addAll(beforeProviders.keySet());
        providerNames.addAll(afterProviders.keySet());
        providerNames.forEach(providerName -> {
            addSecretChange(changes,
                    secretValue(nestedMap(beforeProviders, providerName, "chat"), "apiKey"),
                    secretValue(nestedMap(afterProviders, providerName, "chat"), "apiKey"),
                    "secrets.providers." + providerName + ".chatApiKey",
                    "密钥");
            addSecretChange(changes,
                    secretValue(nestedMap(beforeProviders, providerName, "embedding"), "apiKey"),
                    secretValue(nestedMap(afterProviders, providerName, "embedding"), "apiKey"),
                    "secrets.providers." + providerName + ".embeddingApiKey",
                    "密钥");
            addSecretChange(changes,
                    secretValue(nestedMap(beforeProviders, providerName, "rerank"), "apiKey"),
                    secretValue(nestedMap(afterProviders, providerName, "rerank"), "apiKey"),
                    "secrets.providers." + providerName + ".rerankApiKey",
                    "密钥");
        });
        addSecretChange(
                changes,
                secretValue(nestedMap(beforeMap, "rag", "appServer"), "internalToken"),
                secretValue(nestedMap(afterMap, "rag", "appServer"), "internalToken"),
                "secrets.appServerInternalToken",
                "内部令牌"
        );
        return changes;
    }

    private void addSecretChange(List<AiOpsSecretChangeEntry> changes, String before, String after, String field, String noun) {
        if (Objects.equals(trimToNull(before), trimToNull(after))) {
            return;
        }
        boolean beforePresent = StringUtils.hasText(before);
        boolean afterPresent = StringUtils.hasText(after);
        String action;
        if (!beforePresent && afterPresent) {
            action = "写入新" + noun;
        } else if (beforePresent && !afterPresent) {
            action = "清空现有" + noun;
        } else {
            action = "覆盖现有" + noun;
        }
        changes.add(new AiOpsSecretChangeEntry(field, action));
    }

    private String formatDiffValue(Object value) {
        if (value == null || "".equals(value)) {
            return "--";
        }
        if (value instanceof Boolean bool) {
            return bool ? "true" : "false";
        }
        if (value instanceof List<?> list) {
            return list.isEmpty() ? "--" : list.stream().map(String::valueOf).reduce((left, right) -> left + ", " + right).orElse("--");
        }
        if (value instanceof Map<?, ?> map) {
            try {
                return objectMapper.writeValueAsString(map);
            } catch (Exception exception) {
                return String.valueOf(map);
            }
        }
        return String.valueOf(value);
    }

    private Map<String, Object> toMap(AiOpsConfigPayload payload) {
        if (payload == null) {
            return Map.of();
        }
        return objectMapper.convertValue(payload, MAP_TYPE);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> nestedMap(Map<String, Object> root, String... path) {
        if (root == null) {
            return Map.of();
        }
        Object current = root;
        for (String key : path) {
            if (!(current instanceof Map<?, ?> map)) {
                return Map.of();
            }
            current = map.get(key);
        }
        return current instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private String secretValue(Map<String, Object> root, String key) {
        Object value = root.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
