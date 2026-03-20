package com.huashi.eftransfer.app.modules.user.vo;

import java.util.Set;

public record CurrentUserVO(
        Long id,
        String username,
        String email,
        String displayName,
        String primaryRole,
        Set<String> roles,
        Set<String> capabilities,
        StudentProfileVO studentProfile,
        TeacherProfileVO teacherProfile
) {
}
