package com.huashi.eftransfer.app.modules.diagnosis.vo;

import com.huashi.eftransfer.app.common.session.SessionCompletionHookStatus;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisChartPayload;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisDistributionItem;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisHighRiskLexicalPair;
import com.huashi.eftransfer.shared.enums.DiagnosisSessionStatus;

import java.time.LocalDateTime;
import java.util.List;

public record DiagnosisResultDetailVO(
        Long summaryId,
        Long sessionId,
        DiagnosisSessionStatus status,
        Long templateId,
        String templateName,
        Long ownerUserId,
        Integer totalItems,
        Integer answeredItems,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        SessionCompletionHookStatus completionHooksStatus,
        DiagnosisSummaryMetricsVO metrics,
        List<DiagnosisDistributionItem> errorTypeDistribution,
        List<DiagnosisHighRiskLexicalPair> highRiskLexicalPairs,
        DiagnosisChartPayload chartPayload,
        List<DiagnosisItemResultDetailVO> items
) {
}
