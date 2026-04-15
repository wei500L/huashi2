package com.huashi.eftransfer.app.modules.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.common.util.SecurityUtils;
import com.huashi.eftransfer.app.modules.analytics.entity.LearningProfileSnapshotEntity;
import com.huashi.eftransfer.app.modules.analytics.mapper.LearningProfileSnapshotMapper;
import com.huashi.eftransfer.app.modules.analytics.support.AnalyticsConstants;
import com.huashi.eftransfer.app.modules.analytics.support.AnalyticsJsonCodec;
import com.huashi.eftransfer.app.modules.analytics.support.StudentAnalyticsSnapshotPayload;
import com.huashi.eftransfer.app.modules.user.dto.UpdateStudentLearningGoalRequest;
import com.huashi.eftransfer.app.modules.user.dto.UpdateStudentProfileRequest;
import com.huashi.eftransfer.app.modules.user.entity.StudentProfileEntity;
import com.huashi.eftransfer.app.modules.user.entity.UserEntity;
import com.huashi.eftransfer.app.modules.user.mapper.StudentProfileMapper;
import com.huashi.eftransfer.app.modules.user.mapper.UserMapper;
import com.huashi.eftransfer.app.modules.user.vo.StudentProfileVO;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.enums.TrainingMode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class StudentProfileService {

    private static final int DEFAULT_STUDENT_COMPOSITE_SCORE = 0;

    private final StudentProfileMapper studentProfileMapper;
    private final LearningProfileSnapshotMapper learningProfileSnapshotMapper;
    private final AnalyticsJsonCodec analyticsJsonCodec;
    private final UserMapper userMapper;

    public StudentProfileService(
            StudentProfileMapper studentProfileMapper,
            LearningProfileSnapshotMapper learningProfileSnapshotMapper,
            AnalyticsJsonCodec analyticsJsonCodec,
            UserMapper userMapper
    ) {
        this.studentProfileMapper = studentProfileMapper;
        this.learningProfileSnapshotMapper = learningProfileSnapshotMapper;
        this.analyticsJsonCodec = analyticsJsonCodec;
        this.userMapper = userMapper;
    }

    @Transactional
    public StudentProfileVO updateCurrentStudentProfile(UpdateStudentProfileRequest request) {
        Long studentUserId = requireCurrentStudentUserId();
        StudentProfileEntity studentProfile = findStudentProfile(studentUserId);
        boolean newProfile = studentProfile == null;
        if (newProfile) {
            studentProfile = new StudentProfileEntity();
            studentProfile.setUserId(studentUserId);
            studentProfile.setCompositeScore(DEFAULT_STUDENT_COMPOSITE_SCORE);
        }
        studentProfile.setGradeName(normalizeValue(request.gradeName()));
        studentProfile.setEnglishLevel(normalizeLevel(request.englishLevel()));
        studentProfile.setFrenchLevel(normalizeLevel(request.frenchLevel()));
        studentProfile.setCourseStage(normalizeCourseStage(request.courseStage()));
        if (newProfile) {
            studentProfile = insertStudentProfileWithRetry(studentProfile);
        } else {
            studentProfileMapper.updateById(studentProfile);
        }
        refreshStudentSnapshotProfileFields(studentProfile);
        return toVO(studentProfile);
    }

    @Transactional
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
        StudentProfileEntity studentProfile = findStudentProfile(requireCurrentStudentUserId());
        if (studentProfile == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Student profile not found", 404);
        }
        return studentProfile;
    }

    private StudentProfileEntity findStudentProfile(Long studentUserId) {
        return studentProfileMapper.selectOne(Wrappers.<StudentProfileEntity>lambdaQuery()
                .eq(StudentProfileEntity::getUserId, studentUserId)
                .last("LIMIT 1"));
    }

    private Long requireCurrentStudentUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "Authentication required", 401));
    }

    private void refreshStudentSnapshotProfileFields(StudentProfileEntity studentProfile) {
        LearningProfileSnapshotEntity snapshot = learningProfileSnapshotMapper.selectOne(
                Wrappers.<LearningProfileSnapshotEntity>lambdaQuery()
                        .eq(LearningProfileSnapshotEntity::getScope, AnalyticsConstants.PROFILE_SCOPE_STUDENT)
                        .eq(LearningProfileSnapshotEntity::getStudentUserId, studentProfile.getUserId())
                        .last("LIMIT 1")
        );
        if (snapshot == null) {
            return;
        }

        StudentAnalyticsSnapshotPayload payload = analyticsJsonCodec.read(
                snapshot.getSnapshotJson(),
                StudentAnalyticsSnapshotPayload.class
        );
        UserEntity user = userMapper.selectById(studentProfile.getUserId());
        StudentAnalyticsSnapshotPayload refreshedPayload = new StudentAnalyticsSnapshotPayload(
                payload == null ? user == null ? null : user.getDisplayName() : payload.studentName(),
                studentProfile.getGradeName(),
                studentProfile.getEnglishLevel(),
                studentProfile.getFrenchLevel(),
                payload == null ? null : payload.lastDiagnosisSummaryId(),
                payload == null ? null : payload.lastTrainingSessionId(),
                payload == null ? defaultSnapshotRiskLevel(snapshot.getPrimaryRiskLevel()) : payload.primaryRiskLevel(),
                payload == null ? defaultSnapshotTrainingMode(snapshot.getRecommendedTrainingMode()) : payload.recommendedTrainingMode(),
                payload == null ? safeInt(snapshot.getPendingReviewCount()) : payload.pendingReviewCount(),
                payload == null ? safeInt(snapshot.getHighRiskPairCount()) : payload.highRiskPairCount(),
                payload == null ? safeDecimal(snapshot.getRecentAccuracy()) : payload.recentAccuracy(),
                payload == null ? safeDecimal(snapshot.getRecentNegativeTransferRisk()) : payload.recentNegativeTransferRisk(),
                payload == null ? safeLong(snapshot.getRecentAvgReactionTimeMs()) : payload.recentAvgReactionTimeMs(),
                payload == null ? snapshot.getLastActiveAt() : payload.lastActiveAt(),
                payload == null ? List.of() : payload.topRiskPairs(),
                payload == null ? List.of() : payload.errorDistribution(),
                payload == null ? List.of() : payload.focusTags()
        );
        snapshot.setSnapshotJson(analyticsJsonCodec.write(refreshedPayload));
        learningProfileSnapshotMapper.updateById(snapshot);
    }

    private StudentProfileEntity insertStudentProfileWithRetry(StudentProfileEntity studentProfile) {
        for (int attempt = 0; attempt < 5; attempt++) {
            studentProfile.setStudentNo(generateStudentNo());
            try {
                studentProfileMapper.insert(studentProfile);
                return studentProfile;
            } catch (DataIntegrityViolationException exception) {
                if (studentNoExists(studentProfile.getStudentNo())) {
                    continue;
                }
                StudentProfileEntity existingProfile = findStudentProfile(studentProfile.getUserId());
                if (existingProfile != null) {
                    existingProfile.setGradeName(studentProfile.getGradeName());
                    existingProfile.setEnglishLevel(studentProfile.getEnglishLevel());
                    existingProfile.setFrenchLevel(studentProfile.getFrenchLevel());
                    existingProfile.setCourseStage(studentProfile.getCourseStage());
                    studentProfileMapper.updateById(existingProfile);
                    return existingProfile;
                }
                throw exception;
            }
        }
        throw new BusinessException(ResultCode.INTERNAL_ERROR, "Failed to generate student number", 500);
    }

    private boolean studentNoExists(String studentNo) {
        Long count = studentProfileMapper.selectCount(Wrappers.<StudentProfileEntity>lambdaQuery()
                .eq(StudentProfileEntity::getStudentNo, studentNo));
        return count != null && count > 0;
    }

    private String generateStudentNo() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = "S%s%04d".formatted(
                    LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")),
                    ThreadLocalRandom.current().nextInt(0, 10_000)
            );
            if (!studentNoExists(candidate)) {
                return candidate;
            }
        }
        throw new BusinessException(ResultCode.INTERNAL_ERROR, "Failed to generate student number", 500);
    }

    private StudentProfileVO toVO(StudentProfileEntity studentProfile) {
        return new StudentProfileVO(
                studentProfile.getStudentNo(),
                studentProfile.getGradeName(),
                studentProfile.getEnglishLevel(),
                studentProfile.getFrenchLevel(),
                studentProfile.getCourseStage(),
                studentProfile.getCompositeScore(),
                studentProfile.getDailyTrainingTarget(),
                studentProfile.getWeeklyAccuracyTarget()
        );
    }

    private String normalizeValue(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeLevel(String value) {
        String normalized = normalizeValue(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeCourseStage(String value) {
        String normalized = normalizeValue(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String defaultSnapshotRiskLevel(String value) {
        return value == null || value.isBlank() ? "LOW" : value;
    }

    private String defaultSnapshotTrainingMode(String value) {
        return value == null || value.isBlank() ? TrainingMode.COGNATE_BOOST.name() : value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private double safeDecimal(java.math.BigDecimal value) {
        return value == null ? 0d : value.doubleValue();
    }
}
