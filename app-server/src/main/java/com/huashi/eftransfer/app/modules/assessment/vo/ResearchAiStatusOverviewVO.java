package com.huashi.eftransfer.app.modules.assessment.vo;

public record ResearchAiStatusOverviewVO(
        long pending,
        long processing,
        long completed,
        long fallback,
        long failed
) {
}
