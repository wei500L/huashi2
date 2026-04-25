package com.huashi.eftransfer.app.modules.analytics.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.common.util.TextMojibakeNormalizer;
import com.huashi.eftransfer.app.common.util.SecurityUtils;
import com.huashi.eftransfer.app.modules.analytics.entity.TeachingClassEntity;
import com.huashi.eftransfer.app.modules.analytics.entity.TeachingClassStudentEntity;
import com.huashi.eftransfer.app.modules.analytics.mapper.TeachingClassMapper;
import com.huashi.eftransfer.app.modules.analytics.mapper.TeachingClassStudentMapper;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
public class TeachingClassService {

    private final TeachingClassMapper teachingClassMapper;
    private final TeachingClassStudentMapper teachingClassStudentMapper;

    public TeachingClassService(
            TeachingClassMapper teachingClassMapper,
            TeachingClassStudentMapper teachingClassStudentMapper
    ) {
        this.teachingClassMapper = teachingClassMapper;
        this.teachingClassStudentMapper = teachingClassStudentMapper;
    }

    public List<TeachingClassEntity> listAccessibleClasses() {
        if (isAdmin()) {
            return teachingClassMapper.selectList(Wrappers.<TeachingClassEntity>lambdaQuery()
                    .eq(TeachingClassEntity::getActive, Boolean.TRUE)
                    .orderByAsc(TeachingClassEntity::getId)).stream()
                    .map(this::normalizeDisplayFields)
                    .toList();
        }
        return teachingClassMapper.selectList(Wrappers.<TeachingClassEntity>lambdaQuery()
                .eq(TeachingClassEntity::getTeacherUserId, currentUserId())
                .eq(TeachingClassEntity::getActive, Boolean.TRUE)
                .orderByAsc(TeachingClassEntity::getId)).stream()
                .map(this::normalizeDisplayFields)
                .toList();
    }

    public TeachingClassEntity requireAccessibleClass(Long classId) {
        TeachingClassEntity teachingClass = teachingClassMapper.selectById(classId);
        if (teachingClass == null || !Boolean.TRUE.equals(teachingClass.getActive())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Teaching class was not found", 404);
        }
        if (!isAdmin() && !Objects.equals(teachingClass.getTeacherUserId(), currentUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "You do not have access to this teaching class", 403);
        }
        return normalizeDisplayFields(teachingClass);
    }

    public Optional<TeachingClassEntity> findActiveByClassCode(String classCode) {
        if (!StringUtils.hasText(classCode)) {
            return Optional.empty();
        }
        String normalized = classCode.trim().toLowerCase(Locale.ROOT);
        TeachingClassEntity teachingClass = teachingClassMapper.selectOne(Wrappers.<TeachingClassEntity>lambdaQuery()
                .eq(TeachingClassEntity::getActive, Boolean.TRUE)
                .apply("LOWER(class_code) = {0}", normalized)
                .last("LIMIT 1"));
        return Optional.ofNullable(normalizeDisplayFields(teachingClass));
    }

    public void requireStudentInClass(Long classId, Long studentUserId) {
        TeachingClassEntity teachingClass = teachingClassMapper.selectById(classId);
        if (teachingClass == null || !Boolean.TRUE.equals(teachingClass.getActive())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Teaching class was not found", 404);
        }
        Long count = teachingClassStudentMapper.selectCount(Wrappers.<TeachingClassStudentEntity>lambdaQuery()
                .eq(TeachingClassStudentEntity::getTeachingClassId, classId)
                .eq(TeachingClassStudentEntity::getStudentUserId, studentUserId)
                .eq(TeachingClassStudentEntity::getActive, Boolean.TRUE)
                .and(wrapper -> wrapper.isNull(TeachingClassStudentEntity::getLeftAt)
                        .or()
                        .gt(TeachingClassStudentEntity::getLeftAt, LocalDateTime.now())));
        if (count == null || count == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Student is not in the teaching class", 404);
        }
    }

    public List<Long> listActiveStudentIds(Long classId) {
        return listActiveStudentIds(classId, LocalDateTime.now());
    }

    public List<Long> listActiveStudentIds(Long classId, LocalDateTime asOf) {
        return teachingClassStudentMapper.selectList(Wrappers.<TeachingClassStudentEntity>lambdaQuery()
                        .eq(TeachingClassStudentEntity::getTeachingClassId, classId)
                        .eq(TeachingClassStudentEntity::getActive, Boolean.TRUE)
                        .le(TeachingClassStudentEntity::getJoinedAt, asOf)
                        .and(wrapper -> wrapper.isNull(TeachingClassStudentEntity::getLeftAt)
                                .or()
                                .gt(TeachingClassStudentEntity::getLeftAt, asOf))
                        .orderByAsc(TeachingClassStudentEntity::getId))
                .stream()
                .map(TeachingClassStudentEntity::getStudentUserId)
                .distinct()
                .toList();
    }

    public List<Long> listActiveClassIdsByStudent(Long studentUserId, LocalDateTime asOf) {
        List<Long> classIds = teachingClassStudentMapper.selectList(Wrappers.<TeachingClassStudentEntity>lambdaQuery()
                        .eq(TeachingClassStudentEntity::getStudentUserId, studentUserId)
                        .eq(TeachingClassStudentEntity::getActive, Boolean.TRUE)
                        .le(TeachingClassStudentEntity::getJoinedAt, asOf)
                        .and(wrapper -> wrapper.isNull(TeachingClassStudentEntity::getLeftAt)
                                .or()
                                .gt(TeachingClassStudentEntity::getLeftAt, asOf))
                        .orderByAsc(TeachingClassStudentEntity::getTeachingClassId)
                        .orderByAsc(TeachingClassStudentEntity::getId))
                .stream()
                .map(TeachingClassStudentEntity::getTeachingClassId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
        if (classIds.isEmpty()) {
            return List.of();
        }
        List<TeachingClassEntity> teachingClasses = teachingClassMapper.selectBatchIds(classIds);
        return List.copyOf(teachingClasses.stream()
                .filter(Objects::nonNull)
                .filter(teachingClass -> Boolean.TRUE.equals(teachingClass.getActive()))
                .map(TeachingClassEntity::getId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)));
    }

    public long countActiveStudents(Long classId) {
        return listActiveStudentIds(classId).size();
    }

    private Long currentUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "Authentication required", 401));
    }

    private boolean isAdmin() {
        return SecurityUtils.getCurrentPrincipal()
                .map(principal -> principal.roles().contains("ADMIN"))
                .orElse(false);
    }

    private TeachingClassEntity normalizeDisplayFields(TeachingClassEntity teachingClass) {
        if (teachingClass == null) {
            return null;
        }
        teachingClass.setClassName(TextMojibakeNormalizer.normalize(teachingClass.getClassName()));
        teachingClass.setGradeName(TextMojibakeNormalizer.normalize(teachingClass.getGradeName()));
        return teachingClass;
    }
}
