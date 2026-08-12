package com.huashi.eftransfer.app.modules.practice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PracticeSessionPageQuery(
        @NotNull(message = "pageNo must not be null")
        @Min(1)
        Long pageNo,

        @NotNull(message = "pageSize must not be null")
        @Min(1)
        @Max(50)
        Long pageSize
) {
}
