package com.huashi.eftransfer.app.modules.assessment.vo;

import java.util.List;

public record AssessmentAiAnalysisVO(
        String performanceOverview,
        List<String> strengths,
        List<String> risks,
        String contextInterpretation,
        String reactionTimeInterpretation,
        List<String> recommendations,
        Double confidence,
        String qualityNotice
) {
}
