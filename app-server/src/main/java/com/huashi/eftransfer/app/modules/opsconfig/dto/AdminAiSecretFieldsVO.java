package com.huashi.eftransfer.app.modules.opsconfig.dto;

import java.util.Map;

public record AdminAiSecretFieldsVO(
        Map<String, AdminAiProviderSecretFieldsVO> providers,
        AdminAiSecretFieldVO appServerInternalToken
) {
}
