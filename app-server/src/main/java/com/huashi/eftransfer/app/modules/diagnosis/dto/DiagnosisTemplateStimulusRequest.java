package com.huashi.eftransfer.app.modules.diagnosis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DiagnosisTemplateStimulusRequest(
        @NotBlank(message = "instruction must not be blank")
        @Size(max = 255, message = "instruction must be less than or equal to 255 characters")
        String instruction,
        @Size(max = 1000, message = "contextSentence must be less than or equal to 1000 characters")
        String contextSentence,
        @Size(max = 255, message = "promptText must be less than or equal to 255 characters")
        String promptText
) {
}
