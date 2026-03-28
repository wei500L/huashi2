package com.huashi.eftransfer.app.modules.analytics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.common.util.SecurityUtils;
import com.huashi.eftransfer.app.modules.analytics.dto.TeacherInterventionPageQuery;
import com.huashi.eftransfer.app.modules.analytics.dto.TeacherInterventionUpdateRequest;
import com.huashi.eftransfer.app.modules.analytics.entity.InterventionRecordEntity;
import com.huashi.eftransfer.app.modules.analytics.entity.TeachingClassEntity;
import com.huashi.eftransfer.app.modules.analytics.mapper.InterventionRecordMapper;
import com.huashi.eftransfer.app.modules.analytics.mapper.TeachingClassMapper;
import com.huashi.eftransfer.app.modules.analytics.support.AnalyticsJsonCodec;
import com.huashi.eftransfer.app.modules.analytics.vo.TeacherInterventionSummaryVO;
import com.huashi.eftransfer.app.modules.user.entity.UserEntity;
import com.huashi.eftransfer.app.modules.user.mapper.UserMapper;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import com.huashi.eftransfer.shared.page.PageQuery;
import com.huashi.eftransfer.shared.page.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class TeacherInterventionService {

    private final InterventionRecordMapper interventionRecordMapper;
    private final TeachingClassService teachingClassService;
    private final TeachingClassMapper teachingClassMapper;
    private final UserMapper userMapper;
    private final AnalyticsJsonCodec analyticsJsonCodec;

    public TeacherInterventionService(
            InterventionRecordMapper interventionRecordMapper,
            TeachingClassService teachingClassService,
            TeachingClassMapper teachingClassMapper,
            UserMapper userMapper,
            AnalyticsJsonCodec analyticsJsonCodec
    ) {
        this.interventionRecordMapper = interventionRecordMapper;
        this.teachingClassService = teachingClassService;
        this.teachingClassMapper = teachingClassMapper;
        this.userMapper = userMapper;
        this.analyticsJsonCodec = analyticsJsonCodec;
    }

    public PageResult<TeacherInterventionSummaryVO> pageQuery(TeacherInterventionPageQuery query) {
        PageQuery pageQuery = query.toPageQuery();
        List<Long> accessibleClassIds = resolveAccessibleClassIds(query.classId());
        if (accessibleClassIds.isEmpty()) {
            return new PageResult<>(0, pageQuery.pageNo(), pageQuery.pageSize(), List.of());
        }

        LambdaQueryWrapper<InterventionRecordEntity> wrapper = Wrappers.<InterventionRecordEntity>lambdaQuery()
                .in(InterventionRecordEntity::getTeachingClassId, accessibleClassIds)
                .orderByDesc(InterventionRecordEntity::getPlannedAt)
                .orderByDesc(InterventionRecordEntity::getId);

        if (query.status() != null && !query.status().isBlank()) {
            wrapper.eq(InterventionRecordEntity::getStatus, query.status().trim().toUpperCase());
        }
        if (query.priority() != null && !query.priority().isBlank()) {
            wrapper.eq(InterventionRecordEntity::getPriority, query.priority().trim().toUpperCase());
        }
        if (query.studentUserId() != null) {
            wrapper.eq(InterventionRecordEntity::getStudentUserId, query.studentUserId());
        }

        long total = interventionRecordMapper.selectCount(wrapper);
        List<InterventionRecordEntity> records = interventionRecordMapper.selectList(wrapper
                .last("LIMIT " + pageQuery.pageSize() + " OFFSET " + pageQuery.offset()));

        Map<Long, TeachingClassEntity> classMap = loadClassMap(records.stream()
                .map(InterventionRecordEntity::getTeachingClassId)
                .toList());
        Map<Long, UserEntity> userMap = loadUserMap(records.stream()
                .map(InterventionRecordEntity::getStudentUserId)
                .toList());

        List<TeacherInterventionSummaryVO> items = records.stream()
                .map(record -> toSummary(record, classMap.get(record.getTeachingClassId()), userMap.get(record.getStudentUserId())))
                .toList();
        return new PageResult<>(total, pageQuery.pageNo(), pageQuery.pageSize(), items);
    }

    @Transactional
    public TeacherInterventionSummaryVO update(Long interventionId, TeacherInterventionUpdateRequest request) {
        InterventionRecordEntity entity = interventionRecordMapper.selectById(interventionId);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Intervention record was not found", 404);
        }
        requireAccessible(entity);

        if (request.priority() != null) {
            entity.setPriority(request.priority().trim().toUpperCase());
        }
        if (request.status() != null) {
            entity.setStatus(request.status().trim().toUpperCase());
        }
        entity.setPlannedAt(request.plannedAt());
        entity.setNote(normalizeNullableText(request.teacherNote()));

        if ("COMPLETED".equalsIgnoreCase(entity.getStatus())) {
            if (entity.getCompletedAt() == null) {
                entity.setCompletedAt(LocalDateTime.now());
            }
        } else {
            entity.setCompletedAt(null);
        }

        interventionRecordMapper.updateById(entity);
        TeachingClassEntity teachingClass = entity.getTeachingClassId() == null
                ? null
                : teachingClassMapper.selectById(entity.getTeachingClassId());
        UserEntity student = entity.getStudentUserId() == null
                ? null
                : userMapper.selectById(entity.getStudentUserId());
        return toSummary(entity, teachingClass, student);
    }

    @Transactional
    public void markCompleted(Long interventionId) {
        InterventionRecordEntity entity = interventionRecordMapper.selectById(interventionId);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Intervention record was not found", 404);
        }
        requireAccessible(entity);
        if ("COMPLETED".equalsIgnoreCase(entity.getStatus())) {
            if (entity.getCompletedAt() == null) {
                entity.setCompletedAt(LocalDateTime.now());
                interventionRecordMapper.updateById(entity);
            }
            return;
        }
        entity.setStatus("COMPLETED");
        entity.setCompletedAt(LocalDateTime.now());
        interventionRecordMapper.updateById(entity);
    }

    private void requireAccessible(InterventionRecordEntity entity) {
        if (entity.getTeachingClassId() != null) {
            teachingClassService.requireAccessibleClass(entity.getTeachingClassId());
            return;
        }
        if (!isAdmin() && !Objects.equals(entity.getTeacherUserId(), currentUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "You do not have access to this intervention record", 403);
        }
    }

    private List<Long> resolveAccessibleClassIds(Long requestedClassId) {
        if (requestedClassId != null) {
            return List.of(teachingClassService.requireAccessibleClass(requestedClassId).getId());
        }
        return teachingClassService.listAccessibleClasses().stream()
                .map(TeachingClassEntity::getId)
                .toList();
    }

    private Map<Long, TeachingClassEntity> loadClassMap(Collection<Long> classIds) {
        LinkedHashSet<Long> deduplicated = classIds.stream()
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (deduplicated.isEmpty()) {
            return Map.of();
        }
        return teachingClassMapper.selectBatchIds(deduplicated).stream()
                .collect(java.util.stream.Collectors.toMap(
                        TeachingClassEntity::getId,
                        item -> item,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private Map<Long, UserEntity> loadUserMap(Collection<Long> userIds) {
        LinkedHashSet<Long> deduplicated = userIds.stream()
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (deduplicated.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(deduplicated).stream()
                .collect(java.util.stream.Collectors.toMap(
                        UserEntity::getId,
                        item -> item,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private TeacherInterventionSummaryVO toSummary(
            InterventionRecordEntity entity,
            TeachingClassEntity teachingClass,
            UserEntity student
    ) {
        Map<?, ?> snapshot = analyticsJsonCodec.read(entity.getTriggerSnapshotJson(), Map.class);
        String patternDetected = resolvePatternDetected(snapshot, entity);
        String suggestedAction = resolveSuggestedAction(snapshot, entity);
        return new TeacherInterventionSummaryVO(
                entity.getId(),
                entity.getStudentUserId(),
                student == null ? null : student.getDisplayName(),
                entity.getTeachingClassId(),
                teachingClass == null ? null : teachingClass.getClassName(),
                entity.getPriority(),
                entity.getStatus(),
                entity.getPlannedAt(),
                entity.getCompletedAt(),
                entity.getNote(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                patternDetected,
                suggestedAction
        );
    }

    private String resolvePatternDetected(Map<?, ?> snapshot, InterventionRecordEntity entity) {
        if (snapshot != null) {
            Object focusPairs = snapshot.get("focusLexicalPairs");
            if (focusPairs instanceof List<?> list && !list.isEmpty() && list.getFirst() instanceof Map<?, ?> pair) {
                Object englishWord = pair.get("englishWord");
                Object frenchWord = pair.get("frenchWord");
                Object dominantErrorType = pair.get("dominantErrorType");
                String pairLabel = joinWords(englishWord, frenchWord);
                if (pairLabel != null && dominantErrorType != null) {
                    return pairLabel + " · " + dominantErrorType;
                }
                if (pairLabel != null) {
                    return pairLabel;
                }
            }
            Object recommendationPath = snapshot.get("recommendationPath");
            if (recommendationPath instanceof List<?> list && !list.isEmpty() && list.getFirst() instanceof Map<?, ?> pathItem) {
                Object title = pathItem.get("title");
                if (title instanceof String text && !text.isBlank()) {
                    return text;
                }
            }
        }
        return entity.getInterventionType();
    }

    private String resolveSuggestedAction(Map<?, ?> snapshot, InterventionRecordEntity entity) {
        if (entity.getNote() != null && !entity.getNote().isBlank()) {
            return entity.getNote();
        }
        if (snapshot != null) {
            Object recommendationPath = snapshot.get("recommendationPath");
            if (recommendationPath instanceof List<?> list && !list.isEmpty() && list.getFirst() instanceof Map<?, ?> pathItem) {
                Object reason = pathItem.get("reason");
                if (reason instanceof String text && !text.isBlank()) {
                    return text;
                }
            }
        }
        return "Follow up with targeted teacher guidance.";
    }

    private String joinWords(Object englishWord, Object frenchWord) {
        String left = englishWord instanceof String text && !text.isBlank() ? text : null;
        String right = frenchWord instanceof String text && !text.isBlank() ? text : null;
        if (left == null && right == null) {
            return null;
        }
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left + " / " + right;
    }

    private Long currentUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "Authentication required", 401));
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isAdmin() {
        return SecurityUtils.getCurrentPrincipal()
                .map(principal -> principal.roles().contains("ADMIN"))
                .orElse(false);
    }
}
