package com.huashi.eftransfer.app.modules.assessment.vo;

import java.util.List;

public record PublicAssessmentQuestionVO(
        Long questionId,
        Integer questionOrder,
        String questionType,
        String sectionCode,
        String sectionTitle,
        String sharedMaterial,
        boolean formalSection,
        String stemText,
        String promptText,
        List<AssessmentOptionVO> options,
        boolean required,
        boolean justificationRequired,
        List<String> responses,
        String justificationText,
        String itemCode,
        String displayCondition,
        String spellingHintFirstLetter,
        boolean spellingHintShown,
        int spellingWrongAttemptCount,
        List<ResearchAttachmentVO> attachments
) {
    public PublicAssessmentQuestionVO(
            Long questionId, Integer questionOrder, String questionType,
            String sectionCode, String sectionTitle, String sharedMaterial,
            boolean formalSection,
            String stemText, String promptText, List<AssessmentOptionVO> options,
            boolean required, boolean justificationRequired, List<String> responses,
            String justificationText, String itemCode, String displayCondition
    ) {
        this(questionId, questionOrder, questionType, sectionCode, sectionTitle, sharedMaterial, formalSection,
                stemText, promptText, options, required, justificationRequired, responses,
                justificationText, itemCode, displayCondition, null, false, 0, List.of());
    }

    public PublicAssessmentQuestionVO(
            Long questionId, Integer questionOrder, String questionType,
            String sectionCode, String sectionTitle, String sharedMaterial,
            boolean formalSection,
            String stemText, String promptText, List<AssessmentOptionVO> options,
            boolean required, boolean justificationRequired, List<String> responses,
            String justificationText, String itemCode, String displayCondition,
            String spellingHintFirstLetter, boolean spellingHintShown, int spellingWrongAttemptCount
    ) {
        this(questionId, questionOrder, questionType, sectionCode, sectionTitle, sharedMaterial, formalSection,
                stemText, promptText, options, required, justificationRequired, responses,
                justificationText, itemCode, displayCondition, spellingHintFirstLetter, spellingHintShown,
                spellingWrongAttemptCount, List.of());
    }
}
