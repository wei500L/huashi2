package com.huashi.eftransfer.app.modules.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.modules.user.entity.StudentProfileEntity;
import com.huashi.eftransfer.app.modules.user.entity.TeacherProfileEntity;
import com.huashi.eftransfer.app.modules.user.entity.UserEntity;
import com.huashi.eftransfer.app.modules.user.entity.UserRoleEntity;
import com.huashi.eftransfer.app.modules.user.mapper.StudentProfileMapper;
import com.huashi.eftransfer.app.modules.user.mapper.TeacherProfileMapper;
import com.huashi.eftransfer.app.modules.user.mapper.UserMapper;
import com.huashi.eftransfer.app.modules.user.mapper.UserRoleMapper;
import com.huashi.eftransfer.app.modules.user.vo.CurrentUserVO;
import com.huashi.eftransfer.app.modules.user.vo.StudentProfileVO;
import com.huashi.eftransfer.app.modules.user.vo.TeacherProfileVO;
import com.huashi.eftransfer.app.modules.user.vo.UserSummaryVO;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.enums.UserCapability;
import com.huashi.eftransfer.shared.enums.UserRole;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserQueryService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final TeacherProfileMapper teacherProfileMapper;

    public UserQueryService(
            UserMapper userMapper,
            UserRoleMapper userRoleMapper,
            StudentProfileMapper studentProfileMapper,
            TeacherProfileMapper teacherProfileMapper
    ) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.studentProfileMapper = studentProfileMapper;
        this.teacherProfileMapper = teacherProfileMapper;
    }

    public Optional<UserEntity> findByUsernameOrEmail(String loginId) {
        return Optional.ofNullable(userMapper.selectByUsernameOrEmail(loginId));
    }

    public Optional<UserEntity> findByUsername(String username) {
        return Optional.ofNullable(userMapper.selectByUsername(username));
    }

    public Optional<UserEntity> findByEmail(String email) {
        return Optional.ofNullable(userMapper.selectByEmail(email));
    }

    public Optional<UserEntity> findEnabledById(Long userId) {
        UserEntity user = userMapper.selectOne(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getId, userId)
                .eq(UserEntity::getEnabled, Boolean.TRUE));
        return Optional.ofNullable(user);
    }

    public Set<String> getRoleCodes(Long userId) {
        return userRoleMapper.selectList(Wrappers.<UserRoleEntity>lambdaQuery()
                        .eq(UserRoleEntity::getUserId, userId))
                .stream()
                .map(UserRoleEntity::getRoleCode)
                .collect(Collectors.toSet());
    }

    public CurrentUserVO getCurrentUser(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "User is not available", 401);
        }

        Set<UserRole> roles = typedRoles(getRoleCodes(userId));
        requireAssignedRoles(roles);
        StudentProfileEntity studentProfile = studentProfileMapper.selectOne(Wrappers.<StudentProfileEntity>lambdaQuery()
                .eq(StudentProfileEntity::getUserId, userId));
        TeacherProfileEntity teacherProfile = teacherProfileMapper.selectOne(Wrappers.<TeacherProfileEntity>lambdaQuery()
                .eq(TeacherProfileEntity::getUserId, userId));

        StudentProfileVO studentProfileVO = studentProfile == null ? null : new StudentProfileVO(
                studentProfile.getStudentNo(),
                studentProfile.getGradeName(),
                studentProfile.getFrenchLevel(),
                studentProfile.getCourseStage(),
                studentProfile.getCompositeScore(),
                studentProfile.getDailyTrainingTarget(),
                studentProfile.getWeeklyAccuracyTarget()
        );
        TeacherProfileVO teacherProfileVO = teacherProfile == null ? null : new TeacherProfileVO(
                teacherProfile.getEmployeeNo(),
                teacherProfile.getDepartment(),
                teacherProfile.getTitle()
        );

        return new CurrentUserVO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                DisplayNameNormalizer.normalize(user.getDisplayName()),
                user.getLastLoginAt() == null ? null : user.getLastLoginAt().atOffset(ZoneOffset.UTC),
                primaryRole(roles),
                roles,
                capabilities(roles),
                studentProfileVO,
                teacherProfileVO
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<UserSummaryVO> listAllUsers() {
        return userMapper.selectList(Wrappers.<UserEntity>lambdaQuery().orderByAsc(UserEntity::getId))
                .stream()
                .map(user -> new UserSummaryVO(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        DisplayNameNormalizer.normalize(user.getDisplayName()),
                        Boolean.TRUE.equals(user.getEnabled()),
                        getRoleCodes(user.getId()),
                        user.getLastLoginAt(),
                        studentProfileMapper.selectCount(Wrappers.<StudentProfileEntity>lambdaQuery().eq(StudentProfileEntity::getUserId, user.getId())) > 0,
                        teacherProfileMapper.selectCount(Wrappers.<TeacherProfileEntity>lambdaQuery().eq(TeacherProfileEntity::getUserId, user.getId())) > 0,
                        "UNLINKED",
                        "NONE",
                        false
                ))
                .toList();
    }

    private UserRole primaryRole(Set<UserRole> roles) {
        return roles.stream()
                .sorted(Comparator.comparingInt(this::rolePriority))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.FORBIDDEN, "User account has no assigned roles", 403));
    }

    private int rolePriority(UserRole role) {
        return switch (role) {
            case ADMIN -> 0;
            case TEACHER -> 1;
            case STUDENT -> 2;
        };
    }

    private Set<UserRole> typedRoles(Set<String> roleCodes) {
        return roleCodes.stream()
                .map(UserRole::valueOf)
                .sorted(Comparator.comparingInt(this::rolePriority))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<UserCapability> capabilities(Set<UserRole> roles) {
        Set<UserCapability> capabilities = new LinkedHashSet<>();
        if (roles.contains(UserRole.STUDENT)) {
            capabilities.add(UserCapability.STUDENT_WORKSPACE);
        }
        if (roles.contains(UserRole.TEACHER)) {
            capabilities.add(UserCapability.TEACHING_WORKSPACE);
        }
        if (roles.contains(UserRole.ADMIN)) {
            capabilities.add(UserCapability.ADMIN_CONSOLE);
            capabilities.add(UserCapability.TEACHING_WORKSPACE);
            capabilities.add(UserCapability.STUDENT_WORKSPACE);
        }
        return capabilities;
    }

    private void requireAssignedRoles(Set<UserRole> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "User account has no assigned roles", 403);
        }
    }
}
