package com.huashi.eftransfer.app.modules.lexicon.support;

import com.huashi.eftransfer.shared.enums.RiskLevel;

import java.math.BigDecimal;

public final class LexicalRiskSupport {

    private static final BigDecimal MEDIUM_THRESHOLD = new BigDecimal("0.25");
    private static final BigDecimal HIGH_THRESHOLD = new BigDecimal("0.50");
    private static final BigDecimal CRITICAL_THRESHOLD = new BigDecimal("0.75");

    private LexicalRiskSupport() {
    }

    public static RiskLevel resolve(BigDecimal risk) {
        BigDecimal normalized = risk == null ? BigDecimal.ZERO : risk;
        if (normalized.compareTo(CRITICAL_THRESHOLD) >= 0) {
            return RiskLevel.CRITICAL;
        }
        if (normalized.compareTo(HIGH_THRESHOLD) >= 0) {
            return RiskLevel.HIGH;
        }
        if (normalized.compareTo(MEDIUM_THRESHOLD) >= 0) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }
}
