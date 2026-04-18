package com.huashi.eftransfer.app.common.security;

import com.huashi.eftransfer.app.common.config.JwtProperties;
import com.huashi.eftransfer.app.common.trace.TraceIdSupport;
import com.huashi.eftransfer.app.common.security.store.AuthTokenStore;
import com.huashi.eftransfer.app.common.security.store.RegistrationContextSession;
import com.huashi.eftransfer.app.common.security.store.RefreshTokenSession;
import org.slf4j.MDC;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    private final JwtTokenProvider jwtTokenProvider = createJwtTokenProvider();
    private final AuthTokenStore authTokenStore = new NoopAuthTokenStore();
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, authTokenStore);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void shouldSkipInternalRequests() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/knowledge/export");

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void shouldContinueWhenAuthenticationAlreadyExists() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("existing-user", "n/a")
        );
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/student/profile");
        request.addHeader("Authorization", "Bearer should-not-be-read");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo("existing-user");
    }

    @Test
    void shouldBindAuthenticatedUserIdIntoMdc() throws Exception {
        AccessToken accessToken = jwtTokenProvider.generateAccessToken(42L, "teacher.zhang", "Teacher Zhang", java.util.Set.of("TEACHER"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/teacher/dashboard");
        request.addHeader("Authorization", "Bearer " + accessToken.token());
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(MDC.get(TraceIdSupport.USER_ID_MDC_KEY)).isEqualTo("42");
    }

    private static JwtTokenProvider createJwtTokenProvider() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("x7Pq2Lk9Vd4Nc8Rs1Tf6Yh3Jm5Bw0QeZ");
        properties.setIssuer("ef-transfer-platform-test");
        JwtTokenProvider provider = new JwtTokenProvider(properties);
        provider.init();
        return provider;
    }

    private static final class NoopAuthTokenStore implements AuthTokenStore {

        @Override
        public Optional<RegistrationContextSession> findRegistrationContextSession(String tokenHash) {
            return Optional.empty();
        }

        @Override
        public boolean acquireRegistrationContextLock(String tokenHash, Duration ttl) {
            return true;
        }

        @Override
        public void releaseRegistrationContextLock(String tokenHash) {
        }

        @Override
        public void saveRegistrationContextSession(RegistrationContextSession session, Duration ttl) {
        }

        @Override
        public void revokeRegistrationContextSession(String tokenHash) {
        }

        @Override
        public Optional<RefreshTokenSession> findRefreshSession(String refreshTokenHash) {
            return Optional.empty();
        }

        @Override
        public void saveRefreshSession(RefreshTokenSession session, Duration ttl) {
        }

        @Override
        public void revokeRefreshSession(String refreshTokenHash) {
        }

        @Override
        public Optional<String> findActiveRefreshTokenHash(Long userId) {
            return Optional.empty();
        }

        @Override
        public void revokeAllUserSessions(Long userId) {
        }

        @Override
        public void blacklistAccessToken(String tokenId, Duration ttl) {
        }

        @Override
        public boolean isAccessTokenBlacklisted(String tokenId) {
            return false;
        }
    }
}
