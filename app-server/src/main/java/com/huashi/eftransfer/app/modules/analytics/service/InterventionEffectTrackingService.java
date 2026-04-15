package com.huashi.eftransfer.app.modules.analytics.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.modules.analytics.entity.InterventionRecordEntity;
import com.huashi.eftransfer.app.modules.analytics.entity.LearningProfileSnapshotEntity;
import com.huashi.eftransfer.app.modules.analytics.mapper.LearningProfileSnapshotMapper;
import com.huashi.eftransfer.app.modules.analytics.support.AnalyticsConstants;
import com.huashi.eftransfer.app.modules.analytics.vo.TeacherInterventionEffectDiffVO;
import com.huashi.eftransfer.app.modules.analytics.vo.TeacherInterventionEffectSnapshotVO;
import com.huashi.eftransfer.app.modules.analytics.vo.TeacherInterventionEffectVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class InterventionEffectTrackingService {

    private final LearningProfileSnapshotMapper learningProfileSnapshotMapper;

    public InterventionEffectTrackingService(LearningProfileSnapshotMapper learningProfileSnapshotMapper) {
        this.learningProfileSnapshotMapper = learningProfileSnapshotMapper;
    }

    public Long ensureBaselineSnapshot(InterventionRecordEntity intervention) {
        if (intervention == null || intervention.getBaselineSnapshotId() != null) {
            return intervention == null ? null : intervention.getBaselineSnapshotId();
        }
        Long snapshotId = upsertInterventionSnapshot(intervention, 'B');
        intervention.setBaselineSnapshotId(snapshotId);
        return snapshotId;
    }

    public Long captureCompletionSnapshot(InterventionRecordEntity intervention) {
        if (intervention == null) {
            return null;
        }
        Long snapshotId = upsertInterventionSnapshot(intervention, 'C');
        intervention.setCompletionSnapshotId(snapshotId);
        return snapshotId;
    }

    public Map<Long, LearningProfileSnapshotEntity> loadSnapshotMap(Collection<Long> snapshotIds) {
        LinkedHashSet<Long> deduplicated = snapshotIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (deduplicated.isEmpty()) {
            return Map.of();
        }
        return learningProfileSnapshotMapper.selectBatchIds(deduplicated).stream()
                .collect(Collectors.toMap(
                        LearningProfileSnapshotEntity::getId,
                        snapshot -> snapshot,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    public TeacherInterventionEffectVO buildEffectTracking(
            InterventionRecordEntity intervention,
            LearningProfileSnapshotEntity baselineSnapshot,
            LearningProfileSnapshotEntity completionSnapshot
    ) {
        TeacherInterventionEffectSnapshotVO baseline = toSnapshotVO(baselineSnapshot);
        TeacherInterventionEffectSnapshotVO completion = toSnapshotVO(completionSnapshot);
        return new TeacherInterventionEffectVO(
                intervention == null ? null : intervention.getBaselineSnapshotId(),
                intervention == null ? null : intervention.getCompletionSnapshotId(),
                baseline,
                completion,
                buildMetricDiff(baseline, completion)
        );
    }

    public TeacherInterventionEffectVO buildEffectTracking(InterventionRecordEntity intervention) {
        Map<Long, LearningProfileSnapshotEntity> snapshotMap = loadSnapshotMap(java.util.Arrays.asList(
                intervention == null ? null : intervention.getBaselineSnapshotId(),
                intervention == null ? null : intervention.getCompletionSnapshotId()
        ));
        return buildEffectTracking(
                intervention,
                snapshotMap.get(intervention == null ? null : intervention.getBaselineSnapshotId()),
                snapshotMap.get(intervention == null ? null : intervention.getCompletionSnapshotId())
        );
    }

    static String buildInterventionSnapshotScope(Long interventionId, char stage) {
        if (interventionId == null) {
            throw new IllegalArgumentException("interventionId is required");
        }
        return "I" + Long.toUnsignedString(interventionId, 36).toUpperCase() + stage;
    }

    static TeacherInterventionEffectDiffVO buildMetricDiff(
            TeacherInterventionEffectSnapshotVO baseline,
            TeacherInterventionEffectSnapshotVO completion
    ) {
        if (baseline == null || completion == null) {
            return null;
        }
        return new TeacherInterventionEffectDiffVO(
                diffDouble(completion.recentAccuracy(), baseline.recentAccuracy()),
                diffDouble(completion.recentNegativeTransferRisk(), baseline.recentNegativeTransferRisk()),
                diffLong(completion.recentAvgReactionTimeMs(), baseline.recentAvgReactionTimeMs()),
                diffInt(completion.pendingReviewCount(), baseline.pendingReviewCount()),
                diffInt(completion.highRiskPairCount(), baseline.highRiskPairCount())
        );
    }

    private Long upsertInterventionSnapshot(InterventionRecordEntity intervention, char stage) {
        if (intervention.getId() == null || intervention.getStudentUserId() == null) {
            return null;
        }
        LearningProfileSnapshotEntity currentSnapshot = learningProfileSnapshotMapper.selectOne(
                Wrappers.<LearningProfileSnapshotEntity>lambdaQuery()
                        .eq(LearningProfileSnapshotEntity::getScope, AnalyticsConstants.PROFILE_SCOPE_STUDENT)
                        .eq(LearningProfileSnapshotEntity::getStudentUserId, intervention.getStudentUserId())
                        .last("LIMIT 1")
        );
        if (currentSnapshot == null) {
            return null;
        }

        String scope = buildInterventionSnapshotScope(intervention.getId(), stage);
        LearningProfileSnapshotEntity archivedSnapshot = learningProfileSnapshotMapper.selectOne(
                Wrappers.<LearningProfileSnapshotEntity>lambdaQuery()
                        .eq(LearningProfileSnapshotEntity::getScope, scope)
                        .eq(LearningProfileSnapshotEntity::getStudentUserId, intervention.getStudentUserId())
                        .last("LIMIT 1")
        );
        if (archivedSnapshot == null) {
            archivedSnapshot = new LearningProfileSnapshotEntity();
            archivedSnapshot.setScope(scope);
            archivedSnapshot.setStudentUserId(intervention.getStudentUserId());
        }

        copySnapshot(currentSnapshot, archivedSnapshot, intervention);
        if (archivedSnapshot.getId() == null) {
            learningProfileSnapshotMapper.insert(archivedSnapshot);
        } else {
            learningProfileSnapshotMapper.updateById(archivedSnapshot);
        }
        return archivedSnapshot.getId();
    }

    private void copySnapshot(
            LearningProfileSnapshotEntity source,
            LearningProfileSnapshotEntity target,
            InterventionRecordEntity intervention
    ) {
        target.setTeachingClassId(intervention.getTeachingClassId());
        target.setTeacherUserId(intervention.getTeacherUserId() == null
                ? source.getTeacherUserId()
                : intervention.getTeacherUserId());
        target.setLastDiagnosisSummaryId(source.getLastDiagnosisSummaryId());
        target.setLastTrainingSessionId(source.getLastTrainingSessionId());
        target.setPrimaryRiskLevel(source.getPrimaryRiskLevel());
        target.setRecommendedTrainingMode(source.getRecommendedTrainingMode());
        target.setPendingReviewCount(source.getPendingReviewCount());
        target.setHighRiskPairCount(source.getHighRiskPairCount());
        target.setRecentAccuracy(source.getRecentAccuracy());
        target.setRecentNegativeTransferRisk(source.getRecentNegativeTransferRisk());
        target.setRecentAvgReactionTimeMs(source.getRecentAvgReactionTimeMs());
        target.setLastActiveAt(source.getLastActiveAt());
        target.setSnapshotJson(source.getSnapshotJson());
        target.setSnapshotAt(source.getSnapshotAt());
        target.setVersion(source.getVersion());
    }

    private TeacherInterventionEffectSnapshotVO toSnapshotVO(LearningProfileSnapshotEntity snapshot) {
        if (snapshot == null) {
            return null;
        }
        return new TeacherInterventionEffectSnapshotVO(
                snapshot.getId(),
                snapshot.getSnapshotAt(),
                snapshot.getPrimaryRiskLevel(),
                snapshot.getRecommendedTrainingMode(),
                snapshot.getPendingReviewCount(),
                snapshot.getHighRiskPairCount(),
                toDouble(snapshot.getRecentAccuracy()),
                toDouble(snapshot.getRecentNegativeTransferRisk()),
                snapshot.getRecentAvgReactionTimeMs()
        );
    }

    private static Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    private static Double diffDouble(Double left, Double right) {
        if (left == null || right == null) {
            return null;
        }
        return left - right;
    }

    private static Long diffLong(Long left, Long right) {
        if (left == null || right == null) {
            return null;
        }
        return left - right;
    }

    private static Integer diffInt(Integer left, Integer right) {
        if (left == null || right == null) {
            return null;
        }
        return left - right;
    }
}
