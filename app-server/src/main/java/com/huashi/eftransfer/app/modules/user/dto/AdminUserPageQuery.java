package com.huashi.eftransfer.app.modules.user.dto;

import com.huashi.eftransfer.shared.page.PageQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AdminUserPageQuery(
        @Min(value = 1, message = "pageNo must be greater than 0")
        Integer pageNo,
        @Min(value = 1, message = "pageSize must be greater than 0")
        @Max(value = 200, message = "pageSize must be less than or equal to 200")
        Integer pageSize,
        String keyword,
        String role,
        Boolean enabled,
        String invitationStatus,
        String profileLinkStatus
) {

    public PageQuery toPageQuery() {
        return new PageQuery(pageNo == null ? 1 : pageNo, pageSize == null ? 20 : pageSize);
    }
}
