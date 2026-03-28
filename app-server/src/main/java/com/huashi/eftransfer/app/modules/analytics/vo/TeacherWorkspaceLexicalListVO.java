package com.huashi.eftransfer.app.modules.analytics.vo;

import java.time.LocalDateTime;

public record TeacherWorkspaceLexicalListVO(
        Long id,
        String listName,
        long itemCount,
        LocalDateTime updatedAt
) {
}
