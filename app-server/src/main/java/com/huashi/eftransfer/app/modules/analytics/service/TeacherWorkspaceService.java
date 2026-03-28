package com.huashi.eftransfer.app.modules.analytics.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.common.util.SecurityUtils;
import com.huashi.eftransfer.app.modules.analytics.entity.InterventionRecordEntity;
import com.huashi.eftransfer.app.modules.analytics.entity.LearningProfileSnapshotEntity;
import com.huashi.eftransfer.app.modules.analytics.entity.TeachingClassEntity;
import com.huashi.eftransfer.app.modules.analytics.mapper.InterventionRecordMapper;
import com.huashi.eftransfer.app.modules.analytics.mapper.LearningProfileSnapshotMapper;
import com.huashi.eftransfer.app.modules.analytics.support.AnalyticsConstants;
import com.huashi.eftransfer.app.modules.analytics.vo.TeacherWorkspaceClassActivityVO;
import com.huashi.eftransfer.app.modules.analytics.vo.TeacherWorkspaceDraftTemplateVO;
import com.huashi.eftransfer.app.modules.analytics.vo.TeacherWorkspaceInterventionVO;
import com.huashi.eftransfer.app.modules.analytics.vo.TeacherWorkspaceLexicalListVO;
import com.huashi.eftransfer.app.modules.analytics.vo.TeacherWorkspaceOverviewVO;
import com.huashi.eftransfer.app.modules.analytics.vo.TeacherWorkspaceSummaryVO;
import com.huashi.eftransfer.app.modules.diagnosis.entity.DiagnosisTemplateDraftEntity;
import com.huashi.eftransfer.app.modules.diagnosis.mapper.DiagnosisTemplateDraftMapper;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalListEntity;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalListItemEntity;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalPairEntity;
import com.huashi.eftransfer.app.modules.lexicon.imports.entity.LexicalImportBatchEntity;
import com.huashi.eftransfer.app.modules.lexicon.imports.mapper.LexicalImportBatchMapper;
import com.huashi.eftransfer.app.modules.lexicon.imports.support.LexicalImportBatchStatus;
import com.huashi.eftransfer.app.modules.lexicon.mapper.LexicalListItemMapper;
import com.huashi.eftransfer.app.modules.lexicon.mapper.LexicalListMapper;
import com.huashi.eftransfer.app.modules.lexicon.mapper.LexicalPairMapper;
import com.huashi.eftransfer.app.modules.user.entity.TeacherProfileEntity;
import com.huashi.eftransfer.app.modules.user.entity.UserEntity;
import com.huashi.eftransfer.app.modules.user.mapper.TeacherProfileMapper;
import com.huashi.eftransfer.app.modules.user.mapper.UserMapper;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TeacherWorkspaceService {

    private static final BigDecimal HIGH_RISK_THRESHOLD = BigDecimal.valueOf(0.6d);

    private final TeachingClassService teachingClassService;
    private final LearningProfileSnapshotMapper learningProfileSnapshotMapper;
    private final DiagnosisTemplateDraftMapper diagnosisTemplateDraftMapper;
    private final InterventionRecordMapper interventionRecordMapper;
    private final LexicalPairMapper lexicalPairMapper;
    private final LexicalListMapper lexicalListMapper;
    private final LexicalListItemMapper lexicalListItemMapper;
    private final LexicalImportBatchMapper lexicalImportBatchMapper;
    private final UserMapper userMapper;
    private final TeacherProfileMapper teacherProfileMapper;

    public TeacherWorkspaceService(
            TeachingClassService teachingClassService,
            LearningProfileSnapshotMapper learningProfileSnapshotMapper,
            DiagnosisTemplateDraftMapper diagnosisTemplateDraftMapper,
            InterventionRecordMapper interventionRecordMapper,
            LexicalPairMapper lexicalPairMapper,
            LexicalListMapper lexicalListMapper,
            LexicalListItemMapper lexicalListItemMapper,
            LexicalImportBatchMapper lexicalImportBatchMapper,
            UserMapper userMapper,
            TeacherProfileMapper teacherProfileMapper
    ) {
        this.teachingClassService = teachingClassService;
        this.learningProfileSnapshotMapper = learningProfileSnapshotMapper;
        this.diagnosisTemplateDraftMapper = diagnosisTemplateDraftMapper;
        this.interventionRecordMapper = interventionRecordMapper;
        this.lexicalPairMapper = lexicalPairMapper;
        this.lexicalListMapper = lexicalListMapper;
        this.lexicalListItemMapper = lexicalListItemMapper;
        this.lexicalImportBatchMapper = lexicalImportBatchMapper;
        this.userMapper = userMapper;
        this.teacherProfileMapper = teacherProfileMapper;
    }

    public TeacherWorkspaceOverviewVO getOverview() {
        Long userId = currentUserId();
        UserEntity user = requireUser(userId);
        TeacherProfileEntity teacherProfile = teacherProfileMapper.selectOne(Wrappers.<TeacherProfileEntity>lambdaQuery()
                .eq(TeacherProfileEntity::getUserId, userId));

        List<TeachingClassEntity> classes = teachingClassService.listAccessibleClasses();
        Map<Long, List<Long>> classStudentIds = new LinkedHashMap<>();
        LinkedHashSet<Long> uniqueStudentIds = new LinkedHashSet<>();
        for (TeachingClassEntity teachingClass : classes) {
            List<Long> studentIds = teachingClassService.listActiveStudentIds(teachingClass.getId());
            classStudentIds.put(teachingClass.getId(), studentIds);
            uniqueStudentIds.addAll(studentIds);
        }

        Map<Long, LearningProfileSnapshotEntity> snapshotMap = loadStudentSnapshotMap(uniqueStudentIds);
        List<TeacherWorkspaceClassActivityVO> recentClasses = classes.stream()
                .map(teachingClass -> toClassActivity(teachingClass, classStudentIds.getOrDefault(teachingClass.getId(), List.of()), snapshotMap))
                .sorted(Comparator.comparing(
                        TeacherWorkspaceClassActivityVO::lastActiveAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ).thenComparing(TeacherWorkspaceClassActivityVO::className, Comparator.nullsLast(String::compareToIgnoreCase)))
                .limit(4)
                .toList();

        List<DiagnosisTemplateDraftEntity> drafts = diagnosisTemplateDraftMapper.selectList(Wrappers.<DiagnosisTemplateDraftEntity>lambdaQuery()
                .eq(DiagnosisTemplateDraftEntity::getOwnerUserId, userId)
                .orderByDesc(DiagnosisTemplateDraftEntity::getUpdatedAt)
                .orderByDesc(DiagnosisTemplateDraftEntity::getId));
        List<TeacherWorkspaceDraftTemplateVO> draftTemplates = drafts.stream()
                .limit(4)
                .map(draft -> new TeacherWorkspaceDraftTemplateVO(
                        draft.getId(),
                        draft.getTemplateName(),
                        draft.getSyncState(),
                        draft.getUpdatedAt()
                ))
                .toList();

        Set<Long> accessibleClassIds = classes.stream()
                .map(TeachingClassEntity::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<InterventionRecordEntity> pendingRecords = accessibleClassIds.isEmpty()
                ? interventionRecordMapper.selectList(Wrappers.<InterventionRecordEntity>lambdaQuery()
                        .eq(InterventionRecordEntity::getTeacherUserId, userId)
                        .ne(InterventionRecordEntity::getStatus, "COMPLETED")
                        .orderByAsc(InterventionRecordEntity::getPlannedAt)
                        .orderByDesc(InterventionRecordEntity::getId)
                        .last("LIMIT 4"))
                : interventionRecordMapper.selectList(Wrappers.<InterventionRecordEntity>lambdaQuery()
                        .in(InterventionRecordEntity::getTeachingClassId, accessibleClassIds)
                        .ne(InterventionRecordEntity::getStatus, "COMPLETED")
                        .orderByAsc(InterventionRecordEntity::getPlannedAt)
                        .orderByDesc(InterventionRecordEntity::getId)
                        .last("LIMIT 4"));
        Map<Long, String> studentNameMap = loadUserDisplayNameMap(pendingRecords.stream()
                .map(InterventionRecordEntity::getStudentUserId)
                .toList());
        List<TeacherWorkspaceInterventionVO> pendingInterventions = pendingRecords.stream()
                .map(record -> new TeacherWorkspaceInterventionVO(
                        record.getId(),
                        record.getTeachingClassId(),
                        record.getStudentUserId(),
                        studentNameMap.get(record.getStudentUserId()),
                        record.getPriority(),
                        record.getStatus(),
                        record.getPlannedAt()
                ))
                .toList();

        List<LexicalListEntity> lists = lexicalListMapper.selectList(Wrappers.<LexicalListEntity>lambdaQuery()
                .eq(LexicalListEntity::getOwnerUserId, userId)
                .orderByDesc(LexicalListEntity::getUpdatedAt)
                .orderByDesc(LexicalListEntity::getId)
                .last("LIMIT 4"));
        Map<Long, Long> lexicalListItemCountMap = loadLexicalListItemCountMap(lists.stream()
                .map(LexicalListEntity::getId)
                .toList());
        List<TeacherWorkspaceLexicalListVO> recentLexicalLists = lists.stream()
                .map(list -> new TeacherWorkspaceLexicalListVO(
                        list.getId(),
                        list.getListName(),
                        lexicalListItemCountMap.getOrDefault(list.getId(), 0L),
                        list.getUpdatedAt()
                ))
                .toList();

        long pendingInterventionCount = accessibleClassIds.isEmpty()
                ? interventionRecordMapper.selectCount(Wrappers.<InterventionRecordEntity>lambdaQuery()
                        .eq(InterventionRecordEntity::getTeacherUserId, userId)
                        .ne(InterventionRecordEntity::getStatus, "COMPLETED"))
                : interventionRecordMapper.selectCount(Wrappers.<InterventionRecordEntity>lambdaQuery()
                        .in(InterventionRecordEntity::getTeachingClassId, accessibleClassIds)
                        .ne(InterventionRecordEntity::getStatus, "COMPLETED"));

        long draftTemplateCount = diagnosisTemplateDraftMapper.selectCount(Wrappers.<DiagnosisTemplateDraftEntity>lambdaQuery()
                .eq(DiagnosisTemplateDraftEntity::getOwnerUserId, userId));
        long lexicalPairCount = lexicalPairMapper.selectCount(Wrappers.<LexicalPairEntity>lambdaQuery());
        long lexicalListCount = lexicalListMapper.selectCount(Wrappers.<LexicalListEntity>lambdaQuery()
                .eq(LexicalListEntity::getOwnerUserId, userId));
        long pendingImportBatchCount = lexicalImportBatchMapper.selectCount(Wrappers.<LexicalImportBatchEntity>lambdaQuery()
                .eq(LexicalImportBatchEntity::getOwnerUserId, userId)
                .in(LexicalImportBatchEntity::getStatus, List.of(
                        LexicalImportBatchStatus.PARSING.name(),
                        LexicalImportBatchStatus.DRAFT.name(),
                        LexicalImportBatchStatus.IMPORTING.name()
                )));

        TeacherWorkspaceSummaryVO summary = new TeacherWorkspaceSummaryVO(
                classes.size(),
                uniqueStudentIds.size(),
                draftTemplateCount,
                pendingInterventionCount,
                lexicalPairCount,
                lexicalListCount,
                pendingImportBatchCount
        );

        return new TeacherWorkspaceOverviewVO(
                resolveTeacherName(user),
                resolveOrganizationLabel(teacherProfile),
                summary,
                recentClasses,
                draftTemplates,
                pendingInterventions,
                recentLexicalLists
        );
    }

    private TeacherWorkspaceClassActivityVO toClassActivity(
            TeachingClassEntity teachingClass,
            List<Long> studentIds,
            Map<Long, LearningProfileSnapshotEntity> snapshotMap
    ) {
        long highRiskStudentCount = studentIds.stream()
                .map(snapshotMap::get)
                .filter(Objects::nonNull)
                .filter(snapshot -> snapshot.getRecentNegativeTransferRisk() != null
                        && snapshot.getRecentNegativeTransferRisk().compareTo(HIGH_RISK_THRESHOLD) >= 0)
                .count();
        LocalDateTime lastActiveAt = studentIds.stream()
                .map(snapshotMap::get)
                .filter(Objects::nonNull)
                .map(LearningProfileSnapshotEntity::getLastActiveAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        return new TeacherWorkspaceClassActivityVO(
                teachingClass.getId(),
                teachingClass.getClassCode(),
                teachingClass.getClassName(),
                studentIds.size(),
                highRiskStudentCount,
                lastActiveAt
        );
    }

    private Map<Long, LearningProfileSnapshotEntity> loadStudentSnapshotMap(Collection<Long> studentIds) {
        LinkedHashSet<Long> deduplicated = studentIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (deduplicated.isEmpty()) {
            return Map.of();
        }
        return learningProfileSnapshotMapper.selectList(Wrappers.<LearningProfileSnapshotEntity>lambdaQuery()
                        .eq(LearningProfileSnapshotEntity::getScope, AnalyticsConstants.PROFILE_SCOPE_STUDENT)
                        .in(LearningProfileSnapshotEntity::getStudentUserId, deduplicated))
                .stream()
                .collect(Collectors.toMap(
                        LearningProfileSnapshotEntity::getStudentUserId,
                        snapshot -> snapshot,
                        (left, right) -> {
                            LocalDateTime leftUpdatedAt = left.getUpdatedAt();
                            LocalDateTime rightUpdatedAt = right.getUpdatedAt();
                            if (leftUpdatedAt == null) {
                                return right;
                            }
                            if (rightUpdatedAt == null) {
                                return left;
                            }
                            return rightUpdatedAt.isAfter(leftUpdatedAt) ? right : left;
                        },
                        LinkedHashMap::new
                ));
    }

    private Map<Long, String> loadUserDisplayNameMap(Collection<Long> userIds) {
        LinkedHashSet<Long> deduplicated = userIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (deduplicated.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(deduplicated).stream()
                .collect(Collectors.toMap(
                        UserEntity::getId,
                        this::resolveTeacherName,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private Map<Long, Long> loadLexicalListItemCountMap(Collection<Long> lexicalListIds) {
        LinkedHashSet<Long> deduplicated = lexicalListIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (deduplicated.isEmpty()) {
            return Map.of();
        }
        return lexicalListItemMapper.selectList(Wrappers.<LexicalListItemEntity>lambdaQuery()
                        .in(LexicalListItemEntity::getLexicalListId, deduplicated))
                .stream()
                .collect(Collectors.groupingBy(
                        LexicalListItemEntity::getLexicalListId,
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
    }

    private UserEntity requireUser(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "User is not available", 401);
        }
        return user;
    }

    private String resolveTeacherName(UserEntity user) {
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
            return user.getDisplayName();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return "Teacher";
    }

    private String resolveOrganizationLabel(TeacherProfileEntity teacherProfile) {
        if (teacherProfile == null) {
            return null;
        }
        String department = trimToNull(teacherProfile.getDepartment());
        String title = trimToNull(teacherProfile.getTitle());
        if (department == null && title == null) {
            return null;
        }
        if (department == null) {
            return title;
        }
        if (title == null) {
            return department;
        }
        return department + " · " + title;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Long currentUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "Authentication required", 401));
    }
}
