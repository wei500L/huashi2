package com.huashi.eftransfer.app.modules.assessment.vo;

import java.util.List;

public record PublicAssessmentQuestionPresentationVO(
        List<Emphasis> emphasis
) {
    public PublicAssessmentQuestionPresentationVO {
        emphasis = emphasis == null ? List.of() : List.copyOf(emphasis);
    }

    public record Emphasis(
            String text,
            boolean bold,
            boolean underline,
            Integer occurrence
    ) {
    }
}
