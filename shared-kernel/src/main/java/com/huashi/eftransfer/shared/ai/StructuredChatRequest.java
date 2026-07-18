package com.huashi.eftransfer.shared.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record StructuredChatRequest(
        @NotEmpty(message = "messages must not be empty")
        @Size(max = 64, message = "messages size must be less than or equal to 64")
        List<@Valid ChatMessage> messages,
        @Size(max = 128, message = "model is too long")
        String model,
        @DecimalMin(value = "0.0", message = "temperature must be >= 0")
        @DecimalMax(value = "2.0", message = "temperature must be <= 2")
        Double temperature,
        @NotBlank(message = "schemaName must not be blank")
        @Size(max = 128, message = "schemaName is too long")
        String schemaName,
        Boolean strict,
        @NotEmpty(message = "schema must not be empty")
        @Size(max = 256, message = "schema has too many top-level fields")
        Map<String, Object> schema
) {
}
