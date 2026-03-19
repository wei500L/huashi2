package com.huashi.eftransfer.shared.page;

import java.util.List;

public record PageResult<T>(
        long total,
        long pageNo,
        long pageSize,
        List<T> records
) {
}
