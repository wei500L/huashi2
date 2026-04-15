package com.huashi.eftransfer.app.modules.analytics.vo;

import java.time.LocalDateTime;
import java.util.List;

public record TeacherClassDetailVO(
        Long classId,
        String classCode,
        String className,
        String gradeName,
        Long teacherUserId,
        boolean active,
        long studentCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<TeacherClassStudentVO> students
) {
}
