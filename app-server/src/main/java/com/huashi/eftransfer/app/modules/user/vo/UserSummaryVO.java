package com.huashi.eftransfer.app.modules.user.vo;

import java.util.Set;

public record UserSummaryVO(
        Long id,
        String username,
        String email,
        String displayName,
        boolean enabled,
        Set<String> roles
) {
}
