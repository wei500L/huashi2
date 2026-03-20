package com.huashi.eftransfer.shared.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RagRetrieveRequest(
        @NotBlank(message = "query must not be blank")
        String query,
        @Size(max = 16, message = "sourceTypes size must be less than or equal to 16")
        List<@NotBlank(message = "sourceTypes item must not be blank") String> sourceTypes,
        @Size(max = 128, message = "sourceIds size must be less than or equal to 128")
        List<@NotBlank(message = "sourceIds item must not be blank") String> sourceIds
) {
}
