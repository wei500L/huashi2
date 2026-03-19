package com.huashi.eftransfer.app.modules.diagnosis.support;

public record DiagnosisDistributionItem(
        String code,
        String label,
        long count,
        double ratio
) {
}
