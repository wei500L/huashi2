package com.huashi.eftransfer.app.modules.assessment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ParticipationCodeBatchCreateRequest(
        @Min(1) @Max(5000) int count
) {
}
