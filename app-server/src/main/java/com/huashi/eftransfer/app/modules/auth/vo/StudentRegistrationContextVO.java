package com.huashi.eftransfer.app.modules.auth.vo;

import java.time.OffsetDateTime;

public record StudentRegistrationContextVO(
        String className,
        String gradeName,
        String registrationToken,
        OffsetDateTime registrationTokenExpiresAt
) {
}
