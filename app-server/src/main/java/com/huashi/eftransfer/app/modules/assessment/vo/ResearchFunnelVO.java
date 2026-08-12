package com.huashi.eftransfer.app.modules.assessment.vo;

public record ResearchFunnelVO(
        long codeGenerated,
        long codeVerified,
        long participantCreated,
        long attemptStarted,
        long inProgress,
        long submitted,
        long expired
) {
}
