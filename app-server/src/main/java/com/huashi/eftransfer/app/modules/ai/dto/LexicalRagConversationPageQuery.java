package com.huashi.eftransfer.app.modules.ai.dto;

import com.huashi.eftransfer.shared.page.PageQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record LexicalRagConversationPageQuery(
        @Min(value = 1, message = "pageNo must be greater than 0")
        Integer pageNo,
        @Min(value = 1, message = "pageSize must be greater than 0")
        @Max(value = 100, message = "pageSize must be less than or equal to 100")
        Integer pageSize
) {

    public PageQuery toPageQuery() {
        return new PageQuery(pageNo == null ? 1 : pageNo, pageSize == null ? 20 : pageSize);
    }
}
