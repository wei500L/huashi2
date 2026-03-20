package com.huashi.eftransfer.shared.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

public record StructuredChatRequest(
        @NotEmpty(message = "messages must not be empty")
        List<@Valid ChatMessage> messages,
        String model,
        @DecimalMin(value = "0.0", message = "temperature must be >= 0")
        @DecimalMax(value = "2.0", message = "temperature must be <= 2")
        Double temperature,
        @NotBlank(message = "schemaName must not be blank")
        String schemaName,
        Boolean strict,
        @NotEmpty(message = "schema must not be empty")
        Map<String, Object> schema
) {
}
