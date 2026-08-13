package com.huashi.eftransfer.app.modules.auth.vo;

import java.time.Duration;

public record IssuedAuthSession(
        LoginResponse response,
        String refreshToken,
        Duration refreshTtl
) {
}
