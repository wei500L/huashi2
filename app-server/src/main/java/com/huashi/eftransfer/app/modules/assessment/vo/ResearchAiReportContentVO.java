package com.huashi.eftransfer.app.modules.assessment.vo;

import java.util.List;

public record ResearchAiReportContentVO(
        String executiveSummary,
        List<String> observedPatterns,
        List<String> dimensionFindings,
        List<String> difficultQuestions,
        List<String> distractorFindings,
        List<String> reactionTimeFindings,
        List<String> dataQualityLimitations,
        List<String> researchCautions,
        List<String> recommendedNextAnalyses,
        Double confidence
) {
}
