package com.huashi.eftransfer.app.modules.auth.vo;

import com.huashi.eftransfer.app.modules.user.vo.CurrentUserVO;

import java.time.OffsetDateTime;

public record LoginResponse(
        String accessToken,
        OffsetDateTime accessTokenExpiresAt,
        OffsetDateTime refreshTokenExpiresAt,
        CurrentUserVO userInfo
) {
}
