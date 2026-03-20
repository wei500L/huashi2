package com.huashi.eftransfer.app.modules.training.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record SaveTrainingProgressRequest(
        @NotNull(message = "progressSnapshot must not be null")
        Map<String, Object> progressSnapshot
) {
}
