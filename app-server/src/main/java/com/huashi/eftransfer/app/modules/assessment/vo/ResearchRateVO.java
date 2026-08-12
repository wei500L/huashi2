package com.huashi.eftransfer.app.modules.assessment.vo;

public record ResearchRateVO(
        long numerator,
        long denominator,
        Double value
) {
    public static ResearchRateVO of(long numerator, long denominator) {
        Double value = denominator <= 0 ? null : numerator / (double) denominator;
        return new ResearchRateVO(numerator, denominator, value);
    }
}
