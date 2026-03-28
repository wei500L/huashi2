package com.huashi.eftransfer.app.modules.analytics.vo;

import java.time.LocalDateTime;

public record TeacherWorkspaceDraftTemplateVO(
        Long draftId,
        String templateName,
        String syncState,
        LocalDateTime updatedAt
) {
}
