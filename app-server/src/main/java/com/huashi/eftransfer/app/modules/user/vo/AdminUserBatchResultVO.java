package com.huashi.eftransfer.app.modules.user.vo;

import java.util.List;

public record AdminUserBatchResultVO(
        String operation,
        int totalCount,
        int successCount,
        List<AdminUserProvisionResultVO> createdUsers,
        List<UserSummaryVO> updatedUsers
) {
}
