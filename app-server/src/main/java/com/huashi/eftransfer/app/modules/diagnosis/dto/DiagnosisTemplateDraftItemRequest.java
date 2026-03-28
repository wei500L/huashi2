package com.huashi.eftransfer.app.modules.diagnosis.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DiagnosisTemplateDraftItemRequest(
        @Size(max = 64, message = "draftItemId must be less than or equal to 64 characters")
        String draftItemId,
        Long lexicalPairId,
        String taskType,
        @Size(max = 64, message = "blockCode must be less than or equal to 64 characters")
        String blockCode,
        Integer sortOrder,
        String contextSupportLevel,
        Boolean expectedSemanticMatch,
        @Valid DiagnosisTemplateStimulusRequest stimulus,
        List<@Valid DiagnosisTemplateOptionRequest> options,
        String correctAnswerKey,
        @Valid DiagnosisTemplateScoringProfileRequest scoringProfile
) {
}
