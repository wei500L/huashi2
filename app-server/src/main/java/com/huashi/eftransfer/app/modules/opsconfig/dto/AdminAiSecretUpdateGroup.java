package com.huashi.eftransfer.app.modules.opsconfig.dto;

import java.util.Map;

public record AdminAiSecretUpdateGroup(
        Map<String, AdminAiProviderSecretUpdateGroup> providers,
        AdminAiSecretValueUpdate appServerInternalToken
) {
}
