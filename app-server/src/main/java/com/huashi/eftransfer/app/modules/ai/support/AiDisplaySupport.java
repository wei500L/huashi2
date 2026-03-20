package com.huashi.eftransfer.app.modules.ai.support;

import com.huashi.eftransfer.shared.enums.TrainingMode;

import java.util.Locale;
import java.util.Map;

public final class AiDisplaySupport {

    private static final Map<String, String> ERROR_LABELS = Map.of(
            "FALSE_FRIEND_CONFUSION", "假朋友混淆",
            "CONTEXT_IGNORED", "忽略语境",
            "OVER_TRANSFER", "过度迁移",
            "UNDER_TRANSFER", "迁移不足",
            "ORTHOGRAPHIC_INTERFERENCE", "形近干扰",
            "SEMANTIC_MISFIRE", "语义误判"
    );

    private AiDisplaySupport() {
    }

    public static String modeLabel(String mode) {
        TrainingMode trainingMode = TrainingMode.fromCode(mode);
        return switch (trainingMode) {
            case FALSE_FRIEND_DISCRIM -> "纠偏：同形异义词辨析";
            case CONTEXT_FIX -> "修复：语境纠偏";
            case SPEED_CHALLENGE -> "提速：快速识别";
            case COGNATE_BOOST -> "强化：正迁移促进";
        };
    }

    public static String errorLabel(String errorType) {
        if (errorType == null || errorType.isBlank()) {
            return "未标注错误";
        }
        return ERROR_LABELS.getOrDefault(errorType.toUpperCase(Locale.ROOT), errorType);
    }

    public static String riskLevelFromScore(double riskScore) {
        if (riskScore >= 0.75d) {
            return "CRITICAL";
        }
        if (riskScore >= 0.55d) {
            return "HIGH";
        }
        if (riskScore >= 0.30d) {
            return "MEDIUM";
        }
        return "LOW";
    }

    public static String priorityFromScore(double riskScore) {
        if (riskScore >= 0.75d) {
            return "HIGH";
        }
        if (riskScore >= 0.45d) {
            return "MEDIUM";
        }
        return "LOW";
    }
}
