package com.huashi.eftransfer.app.modules.auth.support;

import org.springframework.http.ResponseCookie;

import java.time.Duration;

public final class AuthRefreshCookie {

    public static final String NAME = "EF_REFRESH";
    public static final String PATH = "/api/auth";

    private AuthRefreshCookie() {
    }

    public static ResponseCookie issue(String refreshToken, Duration maxAge, boolean secure) {
        long seconds = Math.max(0, maxAge == null ? 0 : maxAge.getSeconds());
        return ResponseCookie.from(NAME, refreshToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(PATH)
                .maxAge(seconds)
                .build();
    }

    public static ResponseCookie clear(boolean secure) {
        return ResponseCookie.from(NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(PATH)
                .maxAge(0)
                .build();
    }
}
