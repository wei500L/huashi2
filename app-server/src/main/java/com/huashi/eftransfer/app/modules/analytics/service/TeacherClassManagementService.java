package com.huashi.eftransfer.app.modules.analytics.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.common.util.SecurityUtils;
import com.huashi.eftransfer.app.modules.analytics.dto.TeacherClassStudentBatchRequest;
import com.huashi.eftransfer.app.modules.analytics.dto.TeacherClassUpsertRequest;
import com.huashi.eftransfer.app.modules.analytics.entity.TeachingClassEntity;
import com.huashi.eftransfer.app.modules.analytics.entity.TeachingClassStudentEntity;
import com.huashi.eftransfer.app.modules.analytics.mapper.TeachingClassMapper;
import com.huashi.eftransfer.app.modules.analytics.mapper.TeachingClassStudentMapper;
import com.huashi.eftransfer.app.modules.analytics.vo.TeacherClassDetailVO;
import com.huashi.eftransfer.app.modules.analytics.vo.TeacherClassStudentCandidateVO;
import com.huashi.eftransfer.app.modules.analytics.vo.TeacherClassStudentVO;
import com.huashi.eftransfer.app.modules.analytics.vo.TeachingClassSummaryVO;
import com.huashi.eftransfer.app.modules.user.entity.StudentProfileEntity;
import com.huashi.eftransfer.app.modules.user.entity.UserEntity;
import com.huashi.eftransfer.app.modules.user.entity.UserRoleEntity;
import com.huashi.eftransfer.app.modules.user.mapper.StudentProfileMapper;
import com.huashi.eftransfer.app.modules.user.mapper.UserMapper;
import com.huashi.eftransfer.app.modules.user.mapper.UserRoleMapper;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.enums.UserRole;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TeacherClassManagementService {

    private final TeachingClassMapper teachingClassMapper;
    private final TeachingClassStudentMapper teachingClassStudentMapper;
    private final TeachingClassService teachingClassService;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final StudentProfileMapper studentProfileMapper;

    public TeacherClassManagementService(
            TeachingClassMapper teachingClassMapper,
            TeachingClassStudentMapper teachingClassStudentMapper,
            TeachingClassService teachingClassService,
            UserMapper userMapper,
            UserRoleMapper userRoleMapper,
            StudentProfileMapper studentProfileMapper
    ) {
        this.teachingClassMapper = teachingClassMapper;
        this.teachingClassStudentMapper = teachingClassStudentMapper;
        this.teachingClassService = teachingClassService;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.studentProfileMapper = studentProfileMapper;
    }

    public List<TeachingClassSummaryVO> listClasses() {
        return teachingClassService.listAccessibleClasses().stream()
                .map(teachingClass -> new TeachingClassSummaryVO(
                        teachingClass.getId(),
                        teachingClass.getClassCode(),
                        teachingClass.getClassName(),
                        teachingClass.getGradeName(),
                        teachingClassService.countActiveStudents(teachingClass.getId())
                ))
                .toList();
    }

    public TeacherClassDetailVO getClassDetail(Long classId) {
        return buildClassDetail(teachingClassService.requireAccessibleClass(classId));
    }

    @Transactional
    public TeacherClassDetailVO createClass(TeacherClassUpsertRequest request) {
        String normalizedCode = normalize(request.classCode());
        ensureClassCodeUnique(normalizedCode, null);

        TeachingClassEntity entity = new TeachingClassEntity();
        entity.setClassCode(normalizedCode);
        entity.setClassName(normalize(request.className()));
        entity.setGradeName(normalize(request.gradeName()));
        entity.setTeacherUserId(currentUserId());
        entity.setActive(Boolean.TRUE);
        teachingClassMapper.insert(entity);
        return buildClassDetail(entity);
    }

    @Transactional
    public TeacherClassDetailVO updateClass(Long classId, TeacherClassUpsertRequest request) {
        TeachingClassEntity entity = teachingClassService.requireAccessibleClass(classId);
        String normalizedCode = normalize(request.classCode());
        ensureClassCodeUnique(normalizedCode, entity.getId());

        entity.setClassCode(normalizedCode);
        entity.setClassName(normalize(request.className()));
        entity.setGradeName(normalize(request.gradeName()));
        teachingClassMapper.updateById(entity);
        return buildClassDetail(entity);
    }

    @Transactional
    public void archiveClass(Long classId) {
        TeachingClassEntity entity = teachingClassService.requireAccessibleClass(classId);
        entity.setActive(Boolean.FALSE);
        teachingClassMapper.updateById(entity);
    }

    @Transactional
    public TeacherClassDetailVO addStudents(Long classId, TeacherClassStudentBatchRequest request) {
        TeachingClassEntity teachingClass = teachingClassService.requireAccessibleClass(classId);
        Set<Long> studentIds = normalizeStudentIds(request.studentUserIds());
        Map<Long, UserEntity> assignableUsers = loadAssignableStudents(studentIds);
        LocalDateTime now = LocalDateTime.now();

        Map<Long, TeachingClassStudentEntity> existingMap = teachingClassStudentMapper.selectList(Wrappers.<TeachingClassStudentEntity>lambdaQuery()
                        .eq(TeachingClassStudentEntity::getTeachingClassId, classId)
                        .in(TeachingClassStudentEntity::getStudentUserId, studentIds))
                .stream()
                .collect(Collectors.toMap(TeachingClassStudentEntity::getStudentUserId, Function.identity()));

        for (Long studentId : studentIds) {
            if (!assignableUsers.containsKey(studentId)) {
                continue;
            }
            TeachingClassStudentEntity existing = existingMap.get(studentId);
            if (existing == null) {
                TeachingClassStudentEntity relation = new TeachingClassStudentEntity();
                relation.setTeachingClassId(classId);
                relation.setStudentUserId(studentId);
                relation.setJoinedAt(now);
                relation.setLeftAt(null);
                relation.setActive(Boolean.TRUE);
                teachingClassStudentMapper.insert(relation);
                continue;
            }
            if (Boolean.TRUE.equals(existing.getActive())
                    && (existing.getLeftAt() == null || existing.getLeftAt().isAfter(now))) {
                continue;
            }
            existing.setJoinedAt(now);
            existing.setLeftAt(null);
            existing.setActive(Boolean.TRUE);
            teachingClassStudentMapper.updateById(existing);
        }

        return buildClassDetail(teachingClass);
    }

    @Transactional
    public TeacherClassDetailVO removeStudents(Long classId, TeacherClassStudentBatchRequest request) {
        TeachingClassEntity teachingClass = teachingClassService.requireAccessibleClass(classId);
        Set<Long> studentIds = normalizeStudentIds(request.studentUserIds());
        if (studentIds.isEmpty()) {
            return buildClassDetail(teachingClass);
        }

        LocalDateTime now = LocalDateTime.now();
        List<TeachingClassStudentEntity> relations = teachingClassStudentMapper.selectList(Wrappers.<TeachingClassStudentEntity>lambdaQuery()
                .eq(TeachingClassStudentEntity::getTeachingClassId, classId)
                .in(TeachingClassStudentEntity::getStudentUserId, studentIds)
                .eq(TeachingClassStudentEntity::getActive, Boolean.TRUE));
        for (TeachingClassStudentEntity relation : relations) {
            relation.setActive(Boolean.FALSE);
            relation.setLeftAt(now);
            teachingClassStudentMapper.updateById(relation);
        }
        return buildClassDetail(teachingClass);
    }

    public List<TeacherClassStudentCandidateVO> listStudentCandidates(Long classId, String keyword) {
        teachingClassService.requireAccessibleClass(classId);
        String normalizedKeyword = normalizeKeyword(keyword);

        List<UserRoleEntity> studentRoles = userRoleMapper.selectList(Wrappers.<UserRoleEntity>lambdaQuery()
                .eq(UserRoleEntity::getRoleCode, UserRole.STUDENT.name()));
        if (studentRoles.isEmpty()) {
            return List.of();
        }

        Set<Long> studentIds = studentRoles.stream()
                .map(UserRoleEntity::getUserId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<UserEntity> studentUsers = userMapper.selectBatchIds(studentIds).stream()
                .filter(Objects::nonNull)
                .filter(user -> Boolean.TRUE.equals(user.getEnabled()))
                .toList();
        if (studentUsers.isEmpty()) {
            return List.of();
        }

        Map<Long, StudentProfileEntity> profileMap = studentProfileMapper.selectList(Wrappers.<StudentProfileEntity>lambdaQuery()
                        .in(StudentProfileEntity::getUserId, studentUsers.stream().map(UserEntity::getId).toList()))
                .stream()
                .collect(Collectors.toMap(StudentProfileEntity::getUserId, Function.identity()));

        Set<Long> assignedIds = new LinkedHashSet<>(teachingClassService.listActiveStudentIds(classId));
        Map<Long, Long> activeClassCountMap = loadActiveClassCountMap(studentUsers.stream()
                .map(UserEntity::getId)
                .toList());

        return studentUsers.stream()
                .map(user -> toCandidate(user, profileMap.get(user.getId()), assignedIds, activeClassCountMap))
                .filter(candidate -> matchesKeyword(candidate, normalizedKeyword))
                .sorted(Comparator.comparing(TeacherClassStudentCandidateVO::assigned).reversed()
                        .thenComparing(TeacherClassStudentCandidateVO::gradeName, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(TeacherClassStudentCandidateVO::studentName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .limit(100)
                .toList();
    }

    private TeacherClassDetailVO buildClassDetail(TeachingClassEntity teachingClass) {
        List<TeachingClassStudentEntity> relations = teachingClassStudentMapper.selectList(Wrappers.<TeachingClassStudentEntity>lambdaQuery()
                .eq(TeachingClassStudentEntity::getTeachingClassId, teachingClass.getId())
                .eq(TeachingClassStudentEntity::getActive, Boolean.TRUE)
                .and(wrapper -> wrapper.isNull(TeachingClassStudentEntity::getLeftAt)
                        .or()
                        .gt(TeachingClassStudentEntity::getLeftAt, LocalDateTime.now()))
                .orderByAsc(TeachingClassStudentEntity::getJoinedAt)
                .orderByAsc(TeachingClassStudentEntity::getId));
        Map<Long, UserEntity> userMap = loadUserMap(relations.stream()
                .map(TeachingClassStudentEntity::getStudentUserId)
                .toList());
        Map<Long, StudentProfileEntity> profileMap = loadStudentProfileMap(userMap.keySet());

        List<TeacherClassStudentVO> students = relations.stream()
                .map(relation -> {
                    UserEntity user = userMap.get(relation.getStudentUserId());
                    StudentProfileEntity profile = profileMap.get(relation.getStudentUserId());
                    return new TeacherClassStudentVO(
                            relation.getStudentUserId(),
                            user == null ? "未知学生" : user.getDisplayName(),
                            user == null ? null : user.getUsername(),
                            profile == null ? null : profile.getStudentNo(),
                            profile == null ? null : profile.getGradeName(),
                            relation.getJoinedAt()
                    );
                })
                .sorted(Comparator.comparing(TeacherClassStudentVO::gradeName, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(TeacherClassStudentVO::studentName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        return new TeacherClassDetailVO(
                teachingClass.getId(),
                teachingClass.getClassCode(),
                teachingClass.getClassName(),
                teachingClass.getGradeName(),
                teachingClass.getTeacherUserId(),
                Boolean.TRUE.equals(teachingClass.getActive()),
                students.size(),
                teachingClass.getCreatedAt(),
                teachingClass.getUpdatedAt(),
                students
        );
    }

    private Map<Long, UserEntity> loadAssignableStudents(Collection<Long> studentIds) {
        Map<Long, UserEntity> userMap = loadUserMap(studentIds);
        if (userMap.size() != studentIds.size()) {
            throw new BusinessException(ResultCode.NOT_FOUND, "One or more students were not found", 404);
        }

        Set<Long> studentRoleUserIds = userRoleMapper.selectList(Wrappers.<UserRoleEntity>lambdaQuery()
                        .in(UserRoleEntity::getUserId, studentIds)
                        .eq(UserRoleEntity::getRoleCode, UserRole.STUDENT.name()))
                .stream()
                .map(UserRoleEntity::getUserId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (studentRoleUserIds.size() != studentIds.size()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "Selected users must all be students", 400);
        }

        Map<Long, StudentProfileEntity> profileMap = loadStudentProfileMap(studentIds);
        if (profileMap.size() != studentIds.size()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "Selected students must have linked student profiles", 400);
        }
        return userMap;
    }

    private Map<Long, UserEntity> loadUserMap(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .filter(Objects::nonNull)
                .filter(user -> Boolean.TRUE.equals(user.getEnabled()))
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
    }

    private Map<Long, StudentProfileEntity> loadStudentProfileMap(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return studentProfileMapper.selectList(Wrappers.<StudentProfileEntity>lambdaQuery()
                        .in(StudentProfileEntity::getUserId, userIds))
                .stream()
                .collect(Collectors.toMap(StudentProfileEntity::getUserId, Function.identity()));
    }

    private Map<Long, Long> loadActiveClassCountMap(Collection<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return Map.of();
        }
        LocalDateTime now = LocalDateTime.now();
        Map<Long, Long> counts = new LinkedHashMap<>();
        for (TeachingClassStudentEntity relation : teachingClassStudentMapper.selectList(Wrappers.<TeachingClassStudentEntity>lambdaQuery()
                .in(TeachingClassStudentEntity::getStudentUserId, studentIds)
                .eq(TeachingClassStudentEntity::getActive, Boolean.TRUE)
                .and(wrapper -> wrapper.isNull(TeachingClassStudentEntity::getLeftAt)
                        .or()
                        .gt(TeachingClassStudentEntity::getLeftAt, now)))) {
            counts.merge(relation.getStudentUserId(), 1L, Long::sum);
        }
        return counts;
    }

    private TeacherClassStudentCandidateVO toCandidate(
            UserEntity user,
            StudentProfileEntity profile,
            Set<Long> assignedIds,
            Map<Long, Long> activeClassCountMap
    ) {
        return new TeacherClassStudentCandidateVO(
                user.getId(),
                user.getDisplayName(),
                user.getUsername(),
                profile == null ? null : profile.getStudentNo(),
                profile == null ? null : profile.getGradeName(),
                assignedIds.contains(user.getId()),
                activeClassCountMap.getOrDefault(user.getId(), 0L)
        );
    }

    private boolean matchesKeyword(TeacherClassStudentCandidateVO candidate, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        return containsIgnoreCase(candidate.studentName(), keyword)
                || containsIgnoreCase(candidate.username(), keyword)
                || containsIgnoreCase(candidate.studentNo(), keyword)
                || containsIgnoreCase(candidate.gradeName(), keyword);
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return keyword.trim().toLowerCase(Locale.ROOT);
    }

    private Set<Long> normalizeStudentIds(List<Long> studentIds) {
        return studentIds == null
                ? Set.of()
                : studentIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void ensureClassCodeUnique(String classCode, Long excludeClassId) {
        Long count = teachingClassMapper.selectCount(Wrappers.<TeachingClassEntity>lambdaQuery()
                .eq(TeachingClassEntity::getClassCode, classCode)
                .ne(excludeClassId != null, TeachingClassEntity::getId, excludeClassId));
        if (count != null && count > 0) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "Class code already exists", 400);
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private Long currentUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "Authentication required", 401));
    }
}
