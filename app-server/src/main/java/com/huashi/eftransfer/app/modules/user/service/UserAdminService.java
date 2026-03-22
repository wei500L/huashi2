package com.huashi.eftransfer.app.modules.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.modules.user.dto.AdminUserAccessUpdateRequest;
import com.huashi.eftransfer.app.modules.user.dto.AdminUserCreateRequest;
import com.huashi.eftransfer.app.modules.user.entity.UserEntity;
import com.huashi.eftransfer.app.modules.user.entity.UserRoleEntity;
import com.huashi.eftransfer.app.modules.user.mapper.UserMapper;
import com.huashi.eftransfer.app.modules.user.mapper.UserRoleMapper;
import com.huashi.eftransfer.app.modules.user.vo.UserSummaryVO;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.enums.UserRole;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserAdminService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public UserAdminService(
            UserMapper userMapper,
            UserRoleMapper userRoleMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserSummaryVO createUser(AdminUserCreateRequest request) {
        String username = request.username().trim();
        String email = request.email().trim();
        ensureLoginIdentifierAvailable(username, "username");
        ensureLoginIdentifierAvailable(email, "email");

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setEmail(email);
        user.setDisplayName(request.displayName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.initialPassword()));
        user.setEnabled(request.enabled());
        userMapper.insert(user);

        replaceRoles(user.getId(), normalizeRoles(request.roles()));
        return toSummary(requireUser(user.getId()));
    }

    @Transactional
    public UserSummaryVO updateUserAccess(Long userId, AdminUserAccessUpdateRequest request) {
        UserEntity user = requireUser(userId);
        user.setEnabled(request.enabled());
        userMapper.updateById(user);

        replaceRoles(userId, normalizeRoles(request.roles()));
        return toSummary(requireUser(userId));
    }

    private void replaceRoles(Long userId, Set<UserRole> roles) {
        userRoleMapper.hardDeleteByUserId(userId);
        for (UserRole role : roles) {
            UserRoleEntity userRole = new UserRoleEntity();
            userRole.setUserId(userId);
            userRole.setRoleCode(role.name());
            userRoleMapper.insert(userRole);
        }
    }

    private UserEntity requireUser(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "User was not found", 404);
        }
        return user;
    }

    private void ensureLoginIdentifierAvailable(String value, String fieldName) {
        UserEntity existing = userMapper.selectByUsernameOrEmail(value);
        if (existing != null) {
            throw new BusinessException(ResultCode.CONFLICT, fieldName + " already exists", 409);
        }
    }

    private Set<UserRole> normalizeRoles(Set<String> roles) {
        Set<UserRole> normalized = (roles == null ? Set.<String>of() : roles).stream()
                .map(this::parseRole)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalized.isEmpty()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "roles must not be empty", 400);
        }
        return normalized;
    }

    private UserRole parseRole(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "role must not be blank", 400);
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        try {
            return UserRole.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "Unsupported userRole: " + value, 400);
        }
    }

    private UserSummaryVO toSummary(UserEntity user) {
        Set<String> roles = userRoleMapper.selectList(Wrappers.<UserRoleEntity>lambdaQuery()
                        .eq(UserRoleEntity::getUserId, user.getId()))
                .stream()
                .map(UserRoleEntity::getRoleCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new UserSummaryVO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                Boolean.TRUE.equals(user.getEnabled()),
                roles
        );
    }
}
