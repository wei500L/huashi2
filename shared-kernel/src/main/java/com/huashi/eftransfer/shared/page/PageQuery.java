package com.huashi.eftransfer.shared.page;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PageQuery(
        @Min(value = 1, message = "pageNo must be greater than 0")
        int pageNo,
        @Min(value = 1, message = "pageSize must be greater than 0")
        @Max(value = 200, message = "pageSize must be less than or equal to 200")
        int pageSize
) {

    public long offset() {
        return (long) (Math.max(pageNo, 1) - 1) * Math.max(pageSize, 1);
    }
}
