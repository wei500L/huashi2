package com.huashi.eftransfer.app.modules.user.vo;

import java.time.LocalDateTime;

public record AccountActionPreviewVO(
        String purpose,
        String username,
        String email,
        String displayName,
        boolean enabled,
        LocalDateTime expiresAt
) {
}
