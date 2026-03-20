package com.huashi.eftransfer.shared.ai;

import jakarta.validation.constraints.Size;

import java.util.List;

public record RagReindexRequest(
        String mode,
        @Size(max = 16, message = "sourceTypes size must be less than or equal to 16")
        List<String> sourceTypes,
        @Size(max = 128, message = "sourceIds size must be less than or equal to 128")
        List<String> sourceIds,
        Boolean forceReembed
) {
}
