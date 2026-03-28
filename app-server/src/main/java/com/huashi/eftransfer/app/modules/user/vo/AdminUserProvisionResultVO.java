package com.huashi.eftransfer.app.modules.user.vo;

public record AdminUserProvisionResultVO(
        UserSummaryVO user,
        AccountActionLinkVO accountAction
) {
}
