package com.huashi.eftransfer.app.modules.auth.service;

import com.huashi.eftransfer.app.common.config.JwtProperties;
import com.huashi.eftransfer.app.common.security.AccessToken;
import com.huashi.eftransfer.app.common.security.JwtPrincipal;
import com.huashi.eftransfer.app.common.security.JwtTokenProvider;
import com.huashi.eftransfer.app.common.security.store.AuthTokenStore;
import com.huashi.eftransfer.app.common.security.store.RefreshTokenSession;
import com.huashi.eftransfer.app.common.util.TokenGenerator;
import com.huashi.eftransfer.app.modules.analytics.entity.TeachingClassEntity;
import com.huashi.eftransfer.app.modules.analytics.entity.TeachingClassStudentEntity;
import com.huashi.eftransfer.app.modules.analytics.mapper.TeachingClassStudentMapper;
import com.huashi.eftransfer.app.modules.analytics.service.TeachingClassService;
import com.huashi.eftransfer.app.modules.auth.dto.LoginRequest;
import com.huashi.eftransfer.app.modules.auth.dto.RegisterStudentRequest;
import com.huashi.eftransfer.app.modules.auth.dto.RefreshTokenRequest;
import com.huashi.eftransfer.app.modules.auth.support.AuthClientContext;
import com.huashi.eftransfer.app.modules.auth.vo.LoginResponse;
import com.huashi.eftransfer.app.modules.auth.vo.StudentRegistrationContextVO;
import com.huashi.eftransfer.app.modules.user.entity.StudentProfileEntity;
import com.huashi.eftransfer.app.modules.user.entity.UserEntity;
import com.huashi.eftransfer.app.modules.user.entity.UserRoleEntity;
import com.huashi.eftransfer.app.modules.user.mapper.StudentProfileMapper;
import com.huashi.eftransfer.app.modules.user.mapper.UserMapper;
import com.huashi.eftransfer.app.modules.user.mapper.UserRoleMapper;
import com.huashi.eftransfer.app.modules.user.service.UserQueryService;
import com.huashi.eftransfer.app.modules.user.vo.CurrentUserVO;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.enums.UserRole;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final DateTimeFormatter STUDENT_NO_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String DEFAULT_STUDENT_COURSE_STAGE = "FOUNDATION";
    private static final int DEFAULT_STUDENT_COMPOSITE_SCORE = 0;

    private final UserQueryService userQueryService;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final TeachingClassService teachingClassService;
    private final TeachingClassStudentMapper teachingClassStudentMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final AuthTokenStore authTokenStore;
    private final AuthLockoutService authLockoutService;

    public AuthService(
            UserQueryService userQueryService,
            UserMapper userMapper,
            UserRoleMapper userRoleMapper,
            StudentProfileMapper studentProfileMapper,
            TeachingClassService teachingClassService,
            TeachingClassStudentMapper teachingClassStudentMapper,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            JwtProperties jwtProperties,
            AuthTokenStore authTokenStore,
            AuthLockoutService authLockoutService
    ) {
        this.userQueryService = userQueryService;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.studentProfileMapper = studentProfileMapper;
        this.teachingClassService = teachingClassService;
        this.teachingClassStudentMapper = teachingClassStudentMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
        this.authTokenStore = authTokenStore;
        this.authLockoutService = authLockoutService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request, AuthClientContext clientContext) {
        String loginId = request.usernameOrEmail();
        UserEntity user = userQueryService.findByUsernameOrEmail(loginId)
                .orElseGet(() -> {
                    authLockoutService.ensureNotLocked(null, loginId);
                    authLockoutService.recordFailure(null, loginId);
                    throw invalidCredentials();
                });

        authLockoutService.ensureNotLocked(user, loginId);

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED, "User account is disabled", 403);
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            authLockoutService.recordFailure(user, loginId);
            throw invalidCredentials();
        }

        Set<String> roles = userQueryService.getRoleCodes(user.getId());
        requireAssignedRoles(roles);
        authLockoutService.clearFailures(user, loginId);

        revokeExistingSession(user.getId());
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        LoginResponse response = issueTokens(user, roles, clientContext);
        log.info("event=auth_login_success userId={} username={} roles={}", user.getId(), user.getUsername(), roles);
        return response;
    }

    @Transactional
    public LoginResponse refresh(RefreshTokenRequest request, AuthClientContext clientContext) {
        String refreshTokenHash = TokenGenerator.sha256(request.refreshToken());
        RefreshTokenSession session = authTokenStore.findRefreshSession(refreshTokenHash)
                .orElseThrow(() -> new BusinessException(ResultCode.TOKEN_INVALID, "Refresh token is invalid", 401));

        if (session.expiresAt().isBefore(Instant.now())) {
            authTokenStore.revokeRefreshSession(refreshTokenHash);
            throw new BusinessException(ResultCode.TOKEN_EXPIRED, "Refresh token has expired", 401);
        }

        String activeTokenHash = authTokenStore.findActiveRefreshTokenHash(session.userId())
                .orElseThrow(() -> new BusinessException(ResultCode.TOKEN_INVALID, "Refresh token is invalid", 401));
        if (!refreshTokenHash.equals(activeTokenHash)) {
            authTokenStore.revokeRefreshSession(refreshTokenHash);
            throw new BusinessException(ResultCode.TOKEN_INVALID, "Refresh token has already been rotated", 401);
        }
        validateRefreshClientBinding(session, refreshTokenHash, clientContext);

        UserEntity user = userQueryService.findEnabledById(session.userId())
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "User session is no longer valid", 401));

        blacklistAccessToken(session.accessTokenId(), session.accessTokenExpiresAt());
        authTokenStore.revokeRefreshSession(refreshTokenHash);
        LoginResponse response = issueTokens(user, userQueryService.getRoleCodes(user.getId()), clientContext);
        log.info("event=auth_refresh_success userId={} username={}", user.getId(), user.getUsername());
        return response;
    }

    @Transactional
    public LoginResponse registerStudent(RegisterStudentRequest request, AuthClientContext clientContext) {
        String username = normalizeValue(request.username());
        String email = normalizeEmail(request.email());
        ensureLoginIdentifierAvailable(username, "username");
        ensureLoginIdentifierAvailable(email, "email");

        TeachingClassEntity teachingClass = requireActiveClass(request.classCode());
        LocalDateTime now = LocalDateTime.now();

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setEmail(email);
        user.setDisplayName(normalizeValue(request.displayName()));
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEnabled(Boolean.TRUE);
        user.setLastLoginAt(now);
        userMapper.insert(user);

        UserRoleEntity role = new UserRoleEntity();
        role.setUserId(user.getId());
        role.setRoleCode(UserRole.STUDENT.name());
        userRoleMapper.insert(role);

        StudentProfileEntity studentProfile = new StudentProfileEntity();
        studentProfile.setUserId(user.getId());
        studentProfile.setStudentNo(generateStudentNo());
        studentProfile.setGradeName(teachingClass.getGradeName());
        studentProfile.setEnglishLevel(normalizeLevel(request.englishLevel()));
        studentProfile.setFrenchLevel(normalizeLevel(request.frenchLevel()));
        studentProfile.setCourseStage(normalizeCourseStage(request.courseStage()));
        studentProfile.setCompositeScore(DEFAULT_STUDENT_COMPOSITE_SCORE);
        studentProfileMapper.insert(studentProfile);

        TeachingClassStudentEntity relation = new TeachingClassStudentEntity();
        relation.setTeachingClassId(teachingClass.getId());
        relation.setStudentUserId(user.getId());
        relation.setJoinedAt(now);
        relation.setLeftAt(null);
        relation.setActive(Boolean.TRUE);
        teachingClassStudentMapper.insert(relation);

        LoginResponse response = issueTokens(user, Set.of(UserRole.STUDENT.name()), clientContext);
        log.info(
                "event=student_self_register_success userId={} username={} classId={} classCode={}",
                user.getId(),
                user.getUsername(),
                teachingClass.getId(),
                teachingClass.getClassCode()
        );
        return response;
    }

    public StudentRegistrationContextVO getRegistrationContext(String rawClassCode) {
        TeachingClassEntity teachingClass = requireActiveClass(rawClassCode);
        return new StudentRegistrationContextVO(
                teachingClass.getClassCode(),
                teachingClass.getClassName(),
                teachingClass.getGradeName()
        );
    }

    public void logout(JwtPrincipal principal) {
        authTokenStore.revokeAllUserSessions(principal.userId());
        Duration ttl = Duration.between(Instant.now(), principal.expiresAt());
        authTokenStore.blacklistAccessToken(principal.tokenId(), ttl);
        log.info("event=auth_logout_success userId={} username={}", principal.userId(), principal.username());
    }

    public CurrentUserVO currentUser(JwtPrincipal principal) {
        return userQueryService.getCurrentUser(principal.userId());
    }

    private LoginResponse issueTokens(UserEntity user, Set<String> roles, AuthClientContext clientContext) {
        requireAssignedRoles(roles);
        AccessToken accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                roles
        );

        String refreshToken = TokenGenerator.generateOpaqueToken();
        String refreshTokenHash = TokenGenerator.sha256(refreshToken);
        Instant now = Instant.now();
        Instant refreshExpiresAt = now.plus(jwtProperties.getRefreshTokenTtl());

        authTokenStore.saveRefreshSession(
                new RefreshTokenSession(
                        refreshTokenHash,
                        user.getId(),
                        user.getUsername(),
                        user.getDisplayName(),
                        roles,
                        accessToken.tokenId(),
                        accessToken.expiresAt(),
                        now,
                        refreshExpiresAt,
                        clientContext.userAgentFingerprint(),
                        clientContext.ipAddress()
                ),
                jwtProperties.getRefreshTokenTtl()
        );

        CurrentUserVO currentUserVO = userQueryService.getCurrentUser(user.getId());
        return new LoginResponse(
                accessToken.token(),
                OffsetDateTime.ofInstant(accessToken.expiresAt(), ZoneOffset.UTC),
                refreshToken,
                OffsetDateTime.ofInstant(refreshExpiresAt, ZoneOffset.UTC),
                currentUserVO
        );
    }

    private BusinessException invalidCredentials() {
        return new BusinessException(ResultCode.INVALID_CREDENTIALS, "Invalid username/email or password", 401);
    }

    private void ensureLoginIdentifierAvailable(String value, String fieldName) {
        if (userQueryService.findByUsernameOrEmail(value).isPresent()) {
            throw new BusinessException(ResultCode.CONFLICT, fieldName + " already exists", 409);
        }
    }

    private void requireAssignedRoles(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "User account has no assigned roles", 403);
        }
    }

    private void revokeExistingSession(Long userId) {
        authTokenStore.findActiveRefreshTokenHash(userId)
                .flatMap(authTokenStore::findRefreshSession)
                .ifPresent(session -> blacklistAccessToken(session.accessTokenId(), session.accessTokenExpiresAt()));
        authTokenStore.revokeAllUserSessions(userId);
    }

    private void blacklistAccessToken(String tokenId, Instant expiresAt) {
        authTokenStore.blacklistAccessToken(tokenId, Duration.between(Instant.now(), expiresAt));
    }

    private void validateRefreshClientBinding(
            RefreshTokenSession session,
            String refreshTokenHash,
            AuthClientContext clientContext
    ) {
        if (!clientContext.matchesUserAgent(session.userAgentFingerprint())) {
            authTokenStore.revokeRefreshSession(refreshTokenHash);
            log.warn(
                    "event=auth_refresh_fingerprint_mismatch userId={} username={} issuedIp={} requestIp={} issuedUserAgent={} requestUserAgent={}",
                    session.userId(),
                    session.username(),
                    session.issuedIpAddress(),
                    clientContext.ipAddress(),
                    session.userAgentFingerprint(),
                    clientContext.userAgentFingerprint()
            );
            throw new BusinessException(ResultCode.TOKEN_INVALID, "Refresh token is invalid", 401);
        }

        if (StringUtils.hasText(session.userAgentFingerprint()) && clientContext.isIpChanged(session.issuedIpAddress())) {
            log.info(
                    "event=auth_refresh_ip_changed userId={} username={} issuedIp={} requestIp={}",
                    session.userId(),
                    session.username(),
                    session.issuedIpAddress(),
                    clientContext.ipAddress()
            );
        }
    }

    private TeachingClassEntity requireActiveClass(String classCode) {
        return teachingClassService.findActiveByClassCode(classCode)
                .orElseThrow(() -> new BusinessException(ResultCode.VALIDATION_ERROR, "Class invite code is invalid", 400));
    }

    private String normalizeValue(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeEmail(String value) {
        String normalized = normalizeValue(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeLevel(String value) {
        String normalized = normalizeValue(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeCourseStage(String value) {
        String normalizedValue = normalizeValue(value);
        if (normalizedValue == null || normalizedValue.isBlank()) {
            return DEFAULT_STUDENT_COURSE_STAGE;
        }
        String normalized = normalizedValue.toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? DEFAULT_STUDENT_COURSE_STAGE : normalized;
    }

    private String generateStudentNo() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = "S%s%04d".formatted(
                    LocalDateTime.now().format(STUDENT_NO_TIMESTAMP),
                    ThreadLocalRandom.current().nextInt(0, 10_000)
            );
            Long count = studentProfileMapper.selectCount(
                    com.baomidou.mybatisplus.core.toolkit.Wrappers.<StudentProfileEntity>lambdaQuery()
                            .eq(StudentProfileEntity::getStudentNo, candidate)
            );
            if (count == null || count == 0) {
                return candidate;
            }
        }
        throw new BusinessException(ResultCode.INTERNAL_ERROR, "Failed to generate student number", 500);
    }
}
