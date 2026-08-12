package com.huashi.eftransfer.app.modules.assessment.vo;

public record ResearchRatesVO(
        ResearchRateVO completionRate,
        ResearchRateVO codeRedemptionRate,
        ResearchRateVO submissionRate
) {
}
