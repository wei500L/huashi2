package com.huashi.eftransfer.app.modules.notification.ws;

import com.huashi.eftransfer.app.common.security.JwtPrincipal;
import com.huashi.eftransfer.app.common.security.JwtTokenProvider;
import com.huashi.eftransfer.app.common.security.store.AuthTokenStore;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.WebSocketHandler;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationAuthHandshakeInterceptorTest {

    @Test
    void shouldAuthenticateBearerTokenDuringHandshake() {
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        AuthTokenStore authTokenStore = mock(AuthTokenStore.class);
        JwtPrincipal principal = principal("token-ok");
        when(jwtTokenProvider.parseAccessToken("access-token")).thenReturn(principal);
        when(authTokenStore.isAccessTokenBlacklisted("token-ok")).thenReturn(false);

        NotificationAuthHandshakeInterceptor interceptor =
                new NotificationAuthHandshakeInterceptor(jwtTokenProvider, authTokenStore);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer access-token");
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        Map<String, Object> attributes = new HashMap<>();

        boolean allowed = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                new ServletServerHttpResponse(servletResponse),
                mock(WebSocketHandler.class),
                attributes
        );

        assertThat(allowed).isTrue();
        assertThat(attributes).containsEntry(NotificationWebSocketHandler.USER_ID_ATTRIBUTE, 42L);
        assertThat(attributes).containsEntry(NotificationWebSocketHandler.TOKEN_ID_ATTRIBUTE, "token-ok");
    }

    @Test
    void shouldRejectQueryAccessToken() {
        NotificationAuthHandshakeInterceptor interceptor =
                new NotificationAuthHandshakeInterceptor(mock(JwtTokenProvider.class), mock(AuthTokenStore.class));
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setParameter("access_token", "query-token");
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        boolean allowed = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                new ServletServerHttpResponse(servletResponse),
                mock(WebSocketHandler.class),
                new HashMap<>()
        );

        assertThat(allowed).isFalse();
        assertThat(servletResponse.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void shouldAllowUnauthenticatedHandshakeWithoutToken() {
        NotificationAuthHandshakeInterceptor interceptor =
                new NotificationAuthHandshakeInterceptor(mock(JwtTokenProvider.class), mock(AuthTokenStore.class));
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        Map<String, Object> attributes = new HashMap<>();

        boolean allowed = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                new ServletServerHttpResponse(servletResponse),
                mock(WebSocketHandler.class),
                attributes
        );

        assertThat(allowed).isTrue();
        assertThat(attributes).isEmpty();
    }

    @Test
    void shouldRejectInvalidBearerToken() {
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        when(jwtTokenProvider.parseAccessToken("bad-token")).thenThrow(new JwtException("invalid"));
        NotificationAuthHandshakeInterceptor interceptor =
                new NotificationAuthHandshakeInterceptor(jwtTokenProvider, mock(AuthTokenStore.class));
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer bad-token");
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        boolean allowed = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                new ServletServerHttpResponse(servletResponse),
                mock(WebSocketHandler.class),
                new HashMap<>()
        );

        assertThat(allowed).isFalse();
        assertThat(servletResponse.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    private JwtPrincipal principal(String tokenId) {
        return new JwtPrincipal(
                42L,
                "student.li",
                "Student Li",
                Set.of("STUDENT"),
                tokenId,
                Instant.now().plusSeconds(300),
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );
    }
}
