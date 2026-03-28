package com.huashi.eftransfer.app.modules.user.vo;

import java.time.LocalDateTime;

public record AccountActionLinkVO(
        String purpose,
        String linkUrl,
        LocalDateTime expiresAt,
        String status
) {
}
