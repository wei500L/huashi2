package com.huashi.eftransfer.app.modules.assessment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PublicReleaseUpdateRequest(
        @NotNull Boolean qrEntryEnabled,
        @Min(1) @Max(20) Integer maxAttempts
) {
}
