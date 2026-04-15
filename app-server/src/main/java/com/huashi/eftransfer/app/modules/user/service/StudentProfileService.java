package com.huashi.eftransfer.app.modules.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.common.util.SecurityUtils;
import com.huashi.eftransfer.app.modules.user.dto.UpdateStudentLearningGoalRequest;
import com.huashi.eftransfer.app.modules.user.entity.StudentProfileEntity;
import com.huashi.eftransfer.app.modules.user.mapper.StudentProfileMapper;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class StudentProfileService {

    private final StudentProfileMapper studentProfileMapper;

    public StudentProfileService(StudentProfileMapper studentProfileMapper) {
        this.studentProfileMapper = studentProfileMapper;
    }

    public void updateCurrentStudentLearningGoals(UpdateStudentLearningGoalRequest request) {
        StudentProfileEntity studentProfile = requireCurrentStudentProfile();
        boolean goalsChanged = !Objects.equals(studentProfile.getDailyTrainingTarget(), request.dailyTrainingTarget())
                || !Objects.equals(studentProfile.getWeeklyAccuracyTarget(), request.weeklyAccuracyTarget());
        if (!goalsChanged) {
            return;
        }
        studentProfile.setDailyTrainingTarget(request.dailyTrainingTarget());
        studentProfile.setWeeklyAccuracyTarget(request.weeklyAccuracyTarget());
        studentProfile.setLearningGoalsUpdatedAt(LocalDateTime.now());
        studentProfileMapper.updateById(studentProfile);
    }

    private StudentProfileEntity requireCurrentStudentProfile() {
        Long studentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "Authentication required", 401));
        StudentProfileEntity studentProfile = studentProfileMapper.selectOne(Wrappers.<StudentProfileEntity>lambdaQuery()
                .eq(StudentProfileEntity::getUserId, studentUserId)
                .last("LIMIT 1"));
        if (studentProfile == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Student profile not found", 404);
        }
        return studentProfile;
    }
}
