package com.huashi.eftransfer.app.modules.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.common.security.JwtPrincipal;
import com.huashi.eftransfer.app.common.security.store.AuthTokenStore;
import com.huashi.eftransfer.app.common.security.store.RefreshTokenSession;
import com.huashi.eftransfer.app.common.util.TokenGenerator;
import com.huashi.eftransfer.app.modules.user.dto.CompleteAccountActionRequest;
import com.huashi.eftransfer.app.modules.user.entity.AccountActionTokenEntity;
import com.huashi.eftransfer.app.modules.user.entity.UserEntity;
import com.huashi.eftransfer.app.modules.user.mapper.AccountActionTokenMapper;
import com.huashi.eftransfer.app.modules.user.mapper.UserMapper;
import com.huashi.eftransfer.app.modules.user.support.AccountActionPurpose;
import com.huashi.eftransfer.app.modules.user.support.AccountActionStatus;
import com.huashi.eftransfer.app.modules.user.vo.AccountActionLinkVO;
import com.huashi.eftransfer.app.modules.user.vo.AccountActionPreviewVO;
import com.huashi.eftransfer.app.modules.user.vo.SessionOverviewVO;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class AccountActionService {

    private static final Duration INVITE_TTL = Duration.ofHours(72);
    private static final Duration RESET_TTL = Duration.ofMinutes(30);

    private final AccountActionTokenMapper accountActionTokenMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenStore authTokenStore;

    public AccountActionService(
            AccountActionTokenMapper accountActionTokenMapper,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            AuthTokenStore authTokenStore
    ) {
        this.accountActionTokenMapper = accountActionTokenMapper;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.authTokenStore = authTokenStore;
    }

    @Transactional
    public AccountActionLinkVO createInviteLink(Long userId) {
        return createLink(userId, AccountActionPurpose.INVITE_ACTIVATION);
    }

    @Transactional
    public AccountActionLinkVO createPasswordResetLink(Long userId) {
        return createLink(userId, AccountActionPurpose.PASSWORD_RESET);
    }

    public AccountActionPreviewVO preview(String rawToken) {
        AccountActionTokenEntity token = requireUsableToken(rawToken);
        UserEntity user = requireUser(token.getUserId());
        return new AccountActionPreviewVO(
                token.getPurpose(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                Boolean.TRUE.equals(user.getEnabled()),
                token.getExpiresAt()
        );
    }

    @Transactional
    public void complete(String rawToken, CompleteAccountActionRequest request) {
        AccountActionTokenEntity token = requireUsableToken(rawToken);
        UserEntity user = requireUser(token.getUserId());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        if (AccountActionPurpose.INVITE_ACTIVATION.name().equals(token.getPurpose())) {
            user.setEnabled(true);
        }
        userMapper.updateById(user);

        token.setStatus(AccountActionStatus.CONSUMED.name());
        token.setConsumedAt(LocalDateTime.now());
        accountActionTokenMapper.updateById(token);
        revokeUserSession(user.getId());
    }

    public SessionOverviewVO getSessionOverview(JwtPrincipal principal) {
        UserEntity user = requireUser(principal.userId());
        Optional<RefreshTokenSession> refreshSession = authTokenStore.findActiveRefreshTokenHash(principal.userId())
                .flatMap(authTokenStore::findRefreshSession);
        return new SessionOverviewVO(
                user.getLastLoginAt(),
                refreshSession.map(RefreshTokenSession::issuedAt).orElse(null),
                refreshSession.map(RefreshTokenSession::expiresAt).orElse(null),
                principal.expiresAt(),
                refreshSession.map(RefreshTokenSession::userAgentFingerprint).orElse(null),
                refreshSession.map(RefreshTokenSession::issuedIpAddress).orElse(null),
                refreshSession.isPresent()
        );
    }

    public String resolveInvitationStatus(Long userId) {
        return latestToken(userId, AccountActionPurpose.INVITE_ACTIVATION)
                .map(this::resolveStatus)
                .orElse("NONE");
    }

    public String resolveLatestResetStatus(Long userId) {
        return latestToken(userId, AccountActionPurpose.PASSWORD_RESET)
                .map(this::resolveStatus)
                .orElse("NONE");
    }

    private AccountActionLinkVO createLink(Long userId, AccountActionPurpose purpose) {
        requireUser(userId);
        invalidatePendingTokens(userId, purpose);

        String rawToken = TokenGenerator.generateOpaqueToken();
        LocalDateTime expiresAt = LocalDateTime.now().plus(resolveTtl(purpose));
        AccountActionTokenEntity entity = new AccountActionTokenEntity();
        entity.setUserId(userId);
        entity.setPurpose(purpose.name());
        entity.setTokenHash(TokenGenerator.sha256(rawToken));
        entity.setStatus(AccountActionStatus.PENDING.name());
        entity.setExpiresAt(expiresAt);
        accountActionTokenMapper.insert(entity);

        return new AccountActionLinkVO(
                purpose.name(),
                "/account-action/" + rawToken,
                expiresAt,
                entity.getStatus()
        );
    }

    private void invalidatePendingTokens(Long userId, AccountActionPurpose purpose) {
        List<AccountActionTokenEntity> tokens = accountActionTokenMapper.selectList(Wrappers.<AccountActionTokenEntity>lambdaQuery()
                .eq(AccountActionTokenEntity::getUserId, userId)
                .eq(AccountActionTokenEntity::getPurpose, purpose.name())
                .eq(AccountActionTokenEntity::getStatus, AccountActionStatus.PENDING.name()));
        LocalDateTime now = LocalDateTime.now();
        for (AccountActionTokenEntity token : tokens) {
            token.setStatus(AccountActionStatus.INVALIDATED.name());
            token.setInvalidatedAt(now);
            accountActionTokenMapper.updateById(token);
        }
    }

    private Optional<AccountActionTokenEntity> latestToken(Long userId, AccountActionPurpose purpose) {
        return accountActionTokenMapper.selectList(Wrappers.<AccountActionTokenEntity>lambdaQuery()
                        .eq(AccountActionTokenEntity::getUserId, userId)
                        .eq(AccountActionTokenEntity::getPurpose, purpose.name()))
                .stream()
                .max(Comparator.comparing(AccountActionTokenEntity::getCreatedAt));
    }

    private String resolveStatus(AccountActionTokenEntity token) {
        if (token.getStatus() == null) {
            return "NONE";
        }
        if (AccountActionStatus.PENDING.name().equals(token.getStatus())
                && token.getExpiresAt() != null
                && token.getExpiresAt().isBefore(LocalDateTime.now())) {
            return AccountActionStatus.EXPIRED.name();
        }
        return token.getStatus();
    }

    private AccountActionTokenEntity requireUsableToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "Account action token must not be blank", 400);
        }
        String tokenHash = TokenGenerator.sha256(rawToken);
        AccountActionTokenEntity token = accountActionTokenMapper.selectOne(Wrappers.<AccountActionTokenEntity>lambdaQuery()
                .eq(AccountActionTokenEntity::getTokenHash, tokenHash)
                .last("LIMIT 1"));
        if (token == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Account action token was not found", 404);
        }
        if (!AccountActionStatus.PENDING.name().equals(token.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "Account action token is no longer valid", 409);
        }
        if (token.getExpiresAt() != null && token.getExpiresAt().isBefore(LocalDateTime.now())) {
            token.setStatus(AccountActionStatus.EXPIRED.name());
            accountActionTokenMapper.updateById(token);
            throw new BusinessException(ResultCode.CONFLICT, "Account action token has expired", 409);
        }
        return token;
    }

    private Duration resolveTtl(AccountActionPurpose purpose) {
        return switch (purpose) {
            case INVITE_ACTIVATION -> INVITE_TTL;
            case PASSWORD_RESET -> RESET_TTL;
        };
    }

    private void revokeUserSession(Long userId) {
        authTokenStore.findActiveRefreshTokenHash(userId)
                .flatMap(authTokenStore::findRefreshSession)
                .ifPresent(this::blacklistAccessToken);
        authTokenStore.revokeAllUserSessions(userId);
    }

    private void blacklistAccessToken(RefreshTokenSession session) {
        if (session.accessTokenId() == null || session.accessTokenId().isBlank() || session.accessTokenExpiresAt() == null) {
            return;
        }
        authTokenStore.blacklistAccessToken(
                session.accessTokenId(),
                Duration.between(Instant.now(), session.accessTokenExpiresAt())
        );
    }

    private UserEntity requireUser(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "User was not found", 404);
        }
        return user;
    }
}
