package com.huashi.eftransfer.app.modules.notification.ws;

import com.huashi.eftransfer.app.common.security.JwtPrincipal;
import com.huashi.eftransfer.app.common.security.JwtTokenProvider;
import com.huashi.eftransfer.app.common.security.store.AuthTokenStore;
import com.huashi.eftransfer.app.modules.notification.vo.NotificationItemVO;
import io.jsonwebtoken.JwtException;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    public static final String USER_ID_ATTRIBUTE = "notification.userId";
    public static final String TOKEN_ID_ATTRIBUTE = "notification.tokenId";
    public static final String TOKEN_EXPIRES_AT_ATTRIBUTE = "notification.tokenExpiresAt";
    public static final String AUTH_MESSAGE_TYPE = "AUTH";
    static final String AUTH_TIMEOUT_ATTRIBUTE = "notification.authTimeout";
    private static final Logger log = LoggerFactory.getLogger(NotificationWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final AuthTokenStore authTokenStore;
    private final JwtTokenProvider jwtTokenProvider;
    private final Duration authTimeout;
    private final ScheduledExecutorService authTimeouts;
    private final Map<Long, Set<WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();

    @Autowired
    public NotificationWebSocketHandler(
            ObjectMapper objectMapper,
            AuthTokenStore authTokenStore,
            JwtTokenProvider jwtTokenProvider
    ) {
        this(objectMapper, authTokenStore, jwtTokenProvider, Duration.ofSeconds(5));
    }

    NotificationWebSocketHandler(
            ObjectMapper objectMapper,
            AuthTokenStore authTokenStore,
            JwtTokenProvider jwtTokenProvider,
            Duration authTimeout
    ) {
        this.objectMapper = objectMapper;
        this.authTokenStore = authTokenStore;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authTimeout = authTimeout == null ? Duration.ofSeconds(5) : authTimeout;
        this.authTimeouts = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "notification-ws-auth-timeout");
            thread.setDaemon(true);
            return thread;
        });
    }

    @PreDestroy
    void shutdownAuthTimeouts() {
        authTimeouts.shutdownNow();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (isAuthenticated(session)) {
            register(session);
            return;
        }
        ScheduledFuture<?> timeout = authTimeouts.schedule(() -> {
            if (!isAuthenticated(session) && session.isOpen()) {
                closeQuietly(session, CloseStatus.POLICY_VIOLATION);
            }
        }, Math.max(authTimeout.toMillis(), 1L), TimeUnit.MILLISECONDS);
        session.getAttributes().put(AUTH_TIMEOUT_ATTRIBUTE, timeout);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (isAuthenticated(session)) {
            return;
        }
        if (!authenticateFromMessage(session, message.getPayload())) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        cancelAuthTimeout(session);
        register(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        cancelAuthTimeout(session);
        unregister(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        cancelAuthTimeout(session);
        unregister(session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    public void pushNotificationCreated(Long userId, NotificationItemVO notification, long unreadCount) {
        Set<WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        sessions.removeIf(session -> shouldEvictSession(session, userId));
        if (sessions.isEmpty()) {
            sessionsByUser.remove(userId);
            return;
        }
        TextMessage message;
        try {
            message = new TextMessage(objectMapper.writeValueAsString(
                    new NotificationSocketMessage("NOTIFICATION_CREATED", notification, unreadCount, LocalDateTime.now())
            ));
        } catch (Exception exception) {
            log.warn("event=notification_ws_serialize_failed userId={} notificationId={} reason={}",
                    userId, notification.id(), exception.getMessage());
            return;
        }

        sessions.removeIf(session -> !session.isOpen());
        for (WebSocketSession session : sessions) {
            try {
                synchronized (session) {
                    session.sendMessage(message);
                }
            } catch (IOException exception) {
                log.warn("event=notification_ws_send_failed userId={} sessionId={} notificationId={} reason={}",
                        userId, session.getId(), notification.id(), exception.getMessage());
                unregister(session);
            }
        }
    }

    private boolean authenticateFromMessage(WebSocketSession session, String payload) {
        JsonNode node;
        try {
            node = objectMapper.readTree(payload);
        } catch (Exception exception) {
            return false;
        }
        JsonNode typeNode = node.get("type");
        JsonNode tokenNode = node.get("accessToken");
        if (typeNode == null || !AUTH_MESSAGE_TYPE.equals(typeNode.textValue())) {
            return false;
        }
        String accessToken = tokenNode == null ? null : tokenNode.textValue();
        if (!StringUtils.hasText(accessToken)) {
            return false;
        }
        try {
            JwtPrincipal principal = jwtTokenProvider.parseAccessToken(accessToken);
            if (authTokenStore.isAccessTokenBlacklisted(principal.tokenId())) {
                return false;
            }
            session.getAttributes().put(USER_ID_ATTRIBUTE, principal.userId());
            session.getAttributes().put(TOKEN_ID_ATTRIBUTE, principal.tokenId());
            session.getAttributes().put(TOKEN_EXPIRES_AT_ATTRIBUTE, principal.expiresAt());
            return true;
        } catch (JwtException exception) {
            return false;
        }
    }

    private void register(WebSocketSession session) throws IOException {
        Long userId = userId(session);
        if (userId == null || tokenId(session) == null || tokenExpiresAt(session) == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        sessionsByUser.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }

    private boolean isAuthenticated(WebSocketSession session) {
        return userId(session) != null && tokenId(session) != null && tokenExpiresAt(session) != null;
    }

    private boolean shouldEvictSession(WebSocketSession session, Long expectedUserId) {
        if (!session.isOpen()) {
            return true;
        }
        Long actualUserId = userId(session);
        String tokenId = tokenId(session);
        Instant expiresAt = tokenExpiresAt(session);
        if (!expectedUserId.equals(actualUserId) || tokenId == null || expiresAt == null) {
            closeQuietly(session, CloseStatus.POLICY_VIOLATION);
            return true;
        }
        if (!expiresAt.isAfter(Instant.now()) || authTokenStore.isAccessTokenBlacklisted(tokenId)) {
            log.info("event=notification_ws_session_revoked userId={} sessionId={}", expectedUserId, session.getId());
            closeQuietly(session, CloseStatus.POLICY_VIOLATION);
            return true;
        }
        return false;
    }

    private void unregister(WebSocketSession session) {
        Long userId = userId(session);
        if (userId == null) {
            return;
        }
        Set<WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            sessionsByUser.remove(userId);
        }
    }

    private void cancelAuthTimeout(WebSocketSession session) {
        Object timeout = session.getAttributes().remove(AUTH_TIMEOUT_ATTRIBUTE);
        if (timeout instanceof ScheduledFuture<?> future) {
            future.cancel(false);
        }
    }

    private Long userId(WebSocketSession session) {
        Object value = session.getAttributes().get(USER_ID_ATTRIBUTE);
        return value instanceof Long userId ? userId : null;
    }

    private String tokenId(WebSocketSession session) {
        Object value = session.getAttributes().get(TOKEN_ID_ATTRIBUTE);
        return value instanceof String tokenId && !tokenId.isBlank() ? tokenId : null;
    }

    private Instant tokenExpiresAt(WebSocketSession session) {
        Object value = session.getAttributes().get(TOKEN_EXPIRES_AT_ATTRIBUTE);
        return value instanceof Instant expiresAt ? expiresAt : null;
    }

    private void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (IOException exception) {
            log.debug("event=notification_ws_close_failed sessionId={} reason={}",
                    session.getId(), exception.getMessage());
        }
    }
}
