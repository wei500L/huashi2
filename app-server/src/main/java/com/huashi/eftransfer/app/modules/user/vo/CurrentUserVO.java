package com.huashi.eftransfer.app.modules.user.vo;

import com.huashi.eftransfer.shared.enums.UserCapability;
import com.huashi.eftransfer.shared.enums.UserRole;

import java.time.OffsetDateTime;
import java.util.Set;

public record CurrentUserVO(
        Long id,
        String username,
        String email,
        String displayName,
        OffsetDateTime lastLoginAt,
        UserRole primaryRole,
        Set<UserRole> roles,
        Set<UserCapability> capabilities,
        StudentProfileVO studentProfile,
        TeacherProfileVO teacherProfile
) {
}
