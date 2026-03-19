package com.huashi.eftransfer.app.modules.diagnosis.support;

public record DiagnosisOptionPayload(
        String key,
        String label,
        Boolean semanticMatch,
        Boolean ignoreContextTrap
) {
}
