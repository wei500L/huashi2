package com.huashi.eftransfer.app.modules.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.common.security.store.AuthTokenStore;
import com.huashi.eftransfer.app.modules.user.dto.AdminUserAccessUpdateRequest;
import com.huashi.eftransfer.app.modules.user.dto.AdminUserBatchCreateItemRequest;
import com.huashi.eftransfer.app.modules.user.dto.AdminUserBatchRequest;
import com.huashi.eftransfer.app.modules.user.dto.AdminUserCreateRequest;
import com.huashi.eftransfer.app.modules.user.dto.AdminUserPageQuery;
import com.huashi.eftransfer.app.modules.user.entity.StudentProfileEntity;
import com.huashi.eftransfer.app.modules.user.entity.TeacherProfileEntity;
import com.huashi.eftransfer.app.modules.user.entity.UserEntity;
import com.huashi.eftransfer.app.modules.user.entity.UserRoleEntity;
import com.huashi.eftransfer.app.modules.user.mapper.StudentProfileMapper;
import com.huashi.eftransfer.app.modules.user.mapper.TeacherProfileMapper;
import com.huashi.eftransfer.app.modules.user.mapper.UserMapper;
import com.huashi.eftransfer.app.modules.user.mapper.UserRoleMapper;
import com.huashi.eftransfer.app.modules.user.vo.AdminUserBatchResultVO;
import com.huashi.eftransfer.app.modules.user.vo.AdminUserProvisionResultVO;
import com.huashi.eftransfer.app.modules.user.vo.UserSummaryVO;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.enums.UserRole;
import com.huashi.eftransfer.shared.exception.BusinessException;
import com.huashi.eftransfer.shared.page.PageQuery;
import com.huashi.eftransfer.shared.page.PageResult;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserAdminService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final TeacherProfileMapper teacherProfileMapper;
    private final PasswordEncoder passwordEncoder;
    private final AccountActionService accountActionService;
    private final AuthTokenStore authTokenStore;

    public UserAdminService(
            UserMapper userMapper,
            UserRoleMapper userRoleMapper,
            StudentProfileMapper studentProfileMapper,
            TeacherProfileMapper teacherProfileMapper,
            PasswordEncoder passwordEncoder,
            AccountActionService accountActionService,
            AuthTokenStore authTokenStore
    ) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.studentProfileMapper = studentProfileMapper;
        this.teacherProfileMapper = teacherProfileMapper;
        this.passwordEncoder = passwordEncoder;
        this.accountActionService = accountActionService;
        this.authTokenStore = authTokenStore;
    }

    @Transactional
    public AdminUserProvisionResultVO createUser(AdminUserCreateRequest request) {
        String username = request.username().trim();
        String email = request.email().trim();
        ensureLoginIdentifierAvailable(username, "username");
        ensureLoginIdentifierAvailable(email, "email");
        String credentialMode = normalizeCredentialMode(request.credentialMode());

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setEmail(email);
        user.setDisplayName(request.displayName().trim());
        user.setPasswordHash(passwordEncoder.encode(resolveInitialPassword(request, credentialMode)));
        user.setEnabled(request.enabled());
        userMapper.insert(user);

        replaceRoles(user.getId(), normalizeRoles(request.roles()));
        UserSummaryVO summary = toSummary(requireUser(user.getId()));
        return new AdminUserProvisionResultVO(
                summary,
                "INVITE_LINK".equals(credentialMode) ? accountActionService.createInviteLink(user.getId()) : null
        );
    }

    @Transactional
    public AdminUserBatchResultVO batchUsers(AdminUserBatchRequest request) {
        String operation = normalizeBatchOperation(request.operation());
        if ("IMPORT_CREATE".equals(operation)) {
            return batchCreateUsers(request);
        }
        if ("BULK_ACCESS_UPDATE".equals(operation)) {
            return batchUpdateAccess(request);
        }
        throw new BusinessException(ResultCode.VALIDATION_ERROR, "Unsupported batch operation: " + request.operation(), 400);
    }

    @Transactional
    public UserSummaryVO updateUserAccess(Long userId, AdminUserAccessUpdateRequest request) {
        UserEntity user = requireUser(userId);
        user.setEnabled(request.enabled());
        userMapper.updateById(user);

        replaceRoles(userId, normalizeRoles(request.roles()));
        return toSummary(requireUser(userId));
    }

    public PageResult<UserSummaryVO> pageUsers(AdminUserPageQuery query) {
        PageQuery pageQuery = query.toPageQuery();
        List<UserEntity> users = userMapper.selectList(Wrappers.<UserEntity>lambdaQuery()
                .orderByAsc(UserEntity::getId));
        Map<Long, Set<String>> roleCodes = loadRoleCodes(users.stream().map(UserEntity::getId).toList());
        Set<Long> studentProfileUserIds = studentProfileMapper.selectList(Wrappers.<StudentProfileEntity>lambdaQuery()).stream()
                .map(StudentProfileEntity::getUserId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> teacherProfileUserIds = teacherProfileMapper.selectList(Wrappers.<TeacherProfileEntity>lambdaQuery()).stream()
                .map(TeacherProfileEntity::getUserId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<UserSummaryVO> filtered = users.stream()
                .filter(user -> matchesKeyword(user, query.keyword()))
                .filter(user -> query.enabled() == null || Objects.equals(Boolean.TRUE.equals(user.getEnabled()), query.enabled()))
                .map(user -> toSummary(user, roleCodes, studentProfileUserIds, teacherProfileUserIds))
                .filter(user -> matchesRole(user, query.role()))
                .filter(user -> matchesInvitationStatus(user, query.invitationStatus()))
                .filter(user -> matchesProfileLinkStatus(user, query.profileLinkStatus()))
                .sorted(Comparator.comparing(UserSummaryVO::id))
                .toList();

        int fromIndex = (int) Math.min(filtered.size(), pageQuery.offset());
        int toIndex = Math.min(filtered.size(), fromIndex + pageQuery.pageSize());
        return new PageResult<>(filtered.size(), pageQuery.pageNo(), pageQuery.pageSize(), filtered.subList(fromIndex, toIndex));
    }

    private AdminUserBatchResultVO batchCreateUsers(AdminUserBatchRequest request) {
        List<AdminUserBatchCreateItemRequest> items = request.createItems();
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "createItems must not be empty when operation is IMPORT_CREATE", 400);
        }
        validateCreateBatchIdentifiers(items);
        List<AdminUserProvisionResultVO> createdUsers = new ArrayList<>(items.size());
        for (AdminUserBatchCreateItemRequest item : items) {
            createdUsers.add(createUser(toCreateRequest(item)));
        }
        return new AdminUserBatchResultVO("IMPORT_CREATE", createdUsers.size(), createdUsers.size(), createdUsers, List.of());
    }

    private AdminUserBatchResultVO batchUpdateAccess(AdminUserBatchRequest request) {
        List<Long> userIds = normalizeBatchUserIds(request.userIds());
        if (request.enabled() == null) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "enabled must not be null when operation is BULK_ACCESS_UPDATE", 400);
        }
        Set<String> roles = request.roles();
        if (roles == null || roles.isEmpty()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "roles must not be empty when operation is BULK_ACCESS_UPDATE", 400);
        }
        AdminUserAccessUpdateRequest accessUpdateRequest = new AdminUserAccessUpdateRequest(request.enabled(), roles);
        List<UserSummaryVO> updatedUsers = new ArrayList<>(userIds.size());
        for (Long userId : userIds) {
            updatedUsers.add(updateUserAccess(userId, accessUpdateRequest));
        }
        return new AdminUserBatchResultVO("BULK_ACCESS_UPDATE", userIds.size(), updatedUsers.size(), List.of(), updatedUsers);
    }

    private AdminUserCreateRequest toCreateRequest(AdminUserBatchCreateItemRequest item) {
        return new AdminUserCreateRequest(
                item.username(),
                item.email(),
                item.displayName(),
                item.initialPassword(),
                item.credentialMode(),
                item.enabled(),
                item.roles()
        );
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

    private void validateCreateBatchIdentifiers(List<AdminUserBatchCreateItemRequest> items) {
        Map<String, String> identifierSources = new LinkedHashMap<>();
        for (int index = 0; index < items.size(); index++) {
            AdminUserBatchCreateItemRequest item = items.get(index);
            String rowLabel = batchRowLabel(item.rowNumber(), index);
            registerBatchIdentifier(item.username(), "username", rowLabel, identifierSources);
            registerBatchIdentifier(item.email(), "email", rowLabel, identifierSources);
            ensureLoginIdentifierAvailable(item.username().trim(), "username");
            ensureLoginIdentifierAvailable(item.email().trim(), "email");
        }
    }

    private void registerBatchIdentifier(
            String value,
            String identifierType,
            String rowLabel,
            Map<String, String> identifierSources
    ) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        String currentSource = rowLabel + " " + identifierType;
        String previousSource = identifierSources.putIfAbsent(normalized, currentSource);
        if (previousSource != null) {
            throw new BusinessException(
                    ResultCode.VALIDATION_ERROR,
                    "Duplicate login identifier in batch: " + value + " (" + currentSource + " conflicts with " + previousSource + ")",
                    400
            );
        }
    }

    private List<Long> normalizeBatchUserIds(List<Long> userIds) {
        List<Long> normalized = (userIds == null ? List.<Long>of() : userIds).stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "userIds must not be empty when operation is BULK_ACCESS_UPDATE", 400);
        }
        return normalized;
    }

    private String normalizeBatchOperation(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "operation must not be blank", 400);
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("IMPORT_CREATE", "BULK_ACCESS_UPDATE").contains(normalized)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "Unsupported batch operation: " + value, 400);
        }
        return normalized;
    }

    private String batchRowLabel(Integer rowNumber, int index) {
        return "row " + (rowNumber == null ? index + 1 : rowNumber);
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

    private String resolveInitialPassword(AdminUserCreateRequest request, String credentialMode) {
        if ("INVITE_LINK".equals(credentialMode)) {
            return "Invite@" + java.util.UUID.randomUUID();
        }
        if (request.initialPassword() == null || request.initialPassword().isBlank()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "initialPassword must not be blank when credentialMode is MANUAL_PASSWORD", 400);
        }
        return request.initialPassword();
    }

    private String normalizeCredentialMode(String value) {
        if (value == null || value.isBlank()) {
            return "INVITE_LINK";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("INVITE_LINK", "MANUAL_PASSWORD").contains(normalized)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "Unsupported credentialMode: " + value, 400);
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

    private Map<Long, Set<String>> loadRoleCodes(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Set<String>> roleCodes = new LinkedHashMap<>();
        for (UserRoleEntity userRole : userRoleMapper.selectList(Wrappers.<UserRoleEntity>lambdaQuery()
                .in(UserRoleEntity::getUserId, userIds))) {
            roleCodes.computeIfAbsent(userRole.getUserId(), ignored -> new LinkedHashSet<>()).add(userRole.getRoleCode());
        }
        return roleCodes;
    }

    private UserSummaryVO toSummary(UserEntity user) {
        Map<Long, Set<String>> roleCodeMap = loadRoleCodes(List.of(user.getId()));
        Set<Long> studentProfileUserIds = studentProfileMapper.selectCount(Wrappers.<StudentProfileEntity>lambdaQuery()
                .eq(StudentProfileEntity::getUserId, user.getId())) > 0 ? Set.of(user.getId()) : Set.of();
        Set<Long> teacherProfileUserIds = teacherProfileMapper.selectCount(Wrappers.<TeacherProfileEntity>lambdaQuery()
                .eq(TeacherProfileEntity::getUserId, user.getId())) > 0 ? Set.of(user.getId()) : Set.of();
        return toSummary(user, roleCodeMap, studentProfileUserIds, teacherProfileUserIds);
    }

    private UserSummaryVO toSummary(
            UserEntity user,
            Map<Long, Set<String>> roleCodeMap,
            Set<Long> studentProfileUserIds,
            Set<Long> teacherProfileUserIds
    ) {
        Set<String> roles = new LinkedHashSet<>(roleCodeMap.getOrDefault(user.getId(), Set.of()));
        boolean studentProfileLinked = studentProfileUserIds.contains(user.getId());
        boolean teacherProfileLinked = teacherProfileUserIds.contains(user.getId());
        return new UserSummaryVO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                Boolean.TRUE.equals(user.getEnabled()),
                roles,
                user.getLastLoginAt(),
                studentProfileLinked,
                teacherProfileLinked,
                profileLinkStatus(studentProfileLinked, teacherProfileLinked),
                accountActionService.resolveInvitationStatus(user.getId()),
                authTokenStore.findActiveRefreshTokenHash(user.getId()).isPresent()
        );
    }

    private boolean matchesKeyword(UserEntity user, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String normalized = keyword.trim().toLowerCase(Locale.ROOT);
        return user.getUsername().toLowerCase(Locale.ROOT).contains(normalized)
                || user.getEmail().toLowerCase(Locale.ROOT).contains(normalized)
                || user.getDisplayName().toLowerCase(Locale.ROOT).contains(normalized);
    }

    private boolean matchesRole(UserSummaryVO user, String role) {
        if (role == null || role.isBlank()) {
            return true;
        }
        return user.roles().contains(role.trim().toUpperCase(Locale.ROOT));
    }

    private boolean matchesInvitationStatus(UserSummaryVO user, String invitationStatus) {
        if (invitationStatus == null || invitationStatus.isBlank()) {
            return true;
        }
        return invitationStatus.trim().equalsIgnoreCase(user.invitationStatus());
    }

    private boolean matchesProfileLinkStatus(UserSummaryVO user, String profileLinkStatus) {
        if (profileLinkStatus == null || profileLinkStatus.isBlank()) {
            return true;
        }
        return profileLinkStatus.trim().equalsIgnoreCase(user.profileLinkStatus());
    }

    private String profileLinkStatus(boolean studentProfileLinked, boolean teacherProfileLinked) {
        if (studentProfileLinked && teacherProfileLinked) {
            return "BOTH";
        }
        if (studentProfileLinked) {
            return "STUDENT_ONLY";
        }
        if (teacherProfileLinked) {
            return "TEACHER_ONLY";
        }
        return "UNLINKED";
    }
}
