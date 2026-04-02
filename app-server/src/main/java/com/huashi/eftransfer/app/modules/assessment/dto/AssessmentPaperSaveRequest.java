package com.huashi.eftransfer.app.modules.assessment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AssessmentPaperSaveRequest(
        @NotBlank(message = "title must not be blank")
        @Size(max = 255, message = "title must be at most 255 characters")
        String title,
        @Size(max = 1000, message = "description must be at most 1000 characters")
        String description,
        @NotNull(message = "durationMinutes must not be null")
        @Min(value = 1, message = "durationMinutes must be greater than 0")
        Integer durationMinutes,
        @Valid
        @NotEmpty(message = "questions must not be empty")
        List<AssessmentQuestionRequest> questions
) {
}
