package com.huashi.eftransfer.app.modules.notification.ws;

import com.huashi.eftransfer.app.common.security.JwtPrincipal;
import com.huashi.eftransfer.app.common.security.JwtTokenProvider;
import com.huashi.eftransfer.app.common.security.store.AuthTokenStore;
import com.huashi.eftransfer.app.modules.notification.vo.NotificationItemVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationWebSocketHandlerTest {

    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    private final AuthTokenStore authTokenStore = mock(AuthTokenStore.class);
    private final NotificationWebSocketHandler handler = new NotificationWebSocketHandler(
            new ObjectMapper(),
            authTokenStore,
            jwtTokenProvider,
            Duration.ofSeconds(30)
    );

    @AfterEach
    void shutdown() {
        handler.shutdownAuthTimeouts();
    }

    @Test
    void shouldPushNotificationToAuthorizedSession() throws Exception {
        WebSocketSession session = mockSession(
                "session-valid",
                42L,
                "token-valid",
                Instant.now().plusSeconds(300),
                true
        );
        when(authTokenStore.isAccessTokenBlacklisted("token-valid")).thenReturn(false);

        handler.afterConnectionEstablished(session);
        handler.pushNotificationCreated(42L, notification(), 3);

        verify(session).sendMessage(any(TextMessage.class));
        verify(session, never()).close(CloseStatus.POLICY_VIOLATION);
    }

    @Test
    void shouldCloseRevokedSessionBeforePushingNotification() throws Exception {
        WebSocketSession session = mockSession(
                "session-revoked",
                42L,
                "token-revoked",
                Instant.now().plusSeconds(300),
                true
        );
        when(authTokenStore.isAccessTokenBlacklisted("token-revoked")).thenReturn(true);

        handler.afterConnectionEstablished(session);
        handler.pushNotificationCreated(42L, notification(), 1);

        verify(session).close(CloseStatus.POLICY_VIOLATION);
        verify(session, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void shouldNotPushBeforeFirstMessageAuth() throws Exception {
        WebSocketSession session = mockUnauthenticatedSession("session-pending", true);

        handler.afterConnectionEstablished(session);
        handler.pushNotificationCreated(42L, notification(), 1);

        verify(session, never()).sendMessage(any(TextMessage.class));
        verify(session, never()).close(CloseStatus.POLICY_VIOLATION);
    }

    @Test
    void shouldRegisterAfterFirstAuthMessage() throws Exception {
        WebSocketSession session = mockUnauthenticatedSession("session-auth", true);
        JwtPrincipal principal = new JwtPrincipal(
                42L,
                "student.li",
                "Student Li",
                Set.of("STUDENT"),
                "token-valid",
                Instant.now().plusSeconds(300),
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );
        when(jwtTokenProvider.parseAccessToken("access-token")).thenReturn(principal);
        when(authTokenStore.isAccessTokenBlacklisted("token-valid")).thenReturn(false);

        handler.afterConnectionEstablished(session);
        handler.handleTextMessage(session, new TextMessage("{\"type\":\"AUTH\",\"accessToken\":\"access-token\"}"));
        handler.pushNotificationCreated(42L, notification(), 2);

        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    void shouldRejectInvalidFirstAuthMessage() throws Exception {
        WebSocketSession session = mockUnauthenticatedSession("session-bad-auth", true);

        handler.afterConnectionEstablished(session);
        handler.handleTextMessage(session, new TextMessage("{\"type\":\"AUTH\"}"));

        verify(session).close(CloseStatus.POLICY_VIOLATION);
        verify(session, never()).sendMessage(any(TextMessage.class));
    }

    private WebSocketSession mockSession(
            String sessionId,
            Long userId,
            String tokenId,
            Instant expiresAt,
            boolean open
    ) {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(NotificationWebSocketHandler.USER_ID_ATTRIBUTE, userId);
        attributes.put(NotificationWebSocketHandler.TOKEN_ID_ATTRIBUTE, tokenId);
        attributes.put(NotificationWebSocketHandler.TOKEN_EXPIRES_AT_ATTRIBUTE, expiresAt);
        when(session.getId()).thenReturn(sessionId);
        when(session.getAttributes()).thenReturn(attributes);
        when(session.isOpen()).thenReturn(open);
        return session;
    }

    private WebSocketSession mockUnauthenticatedSession(String sessionId, boolean open) {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        when(session.getId()).thenReturn(sessionId);
        when(session.getAttributes()).thenReturn(attributes);
        when(session.isOpen()).thenReturn(open);
        return session;
    }

    private NotificationItemVO notification() {
        return new NotificationItemVO(
                99L,
                "DIAGNOSIS",
                "INFO",
                "New diagnosis available",
                "A new diagnosis has been assigned.",
                "/diagnosis/99",
                "Open",
                "UNREAD",
                "{}",
                LocalDateTime.now(),
                null
        );
    }
}
