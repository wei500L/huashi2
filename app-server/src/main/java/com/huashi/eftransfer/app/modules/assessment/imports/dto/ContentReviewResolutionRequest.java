package com.huashi.eftransfer.app.modules.assessment.imports.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record ContentReviewResolutionRequest(
        String decision,
        @NotBlank String resolutionNote,
        List<Long> issueIds
) {
}
