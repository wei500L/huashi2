package com.huashi.eftransfer.app.modules.notification.ws;

import com.huashi.eftransfer.app.common.security.store.AuthTokenStore;
import com.huashi.eftransfer.app.modules.notification.vo.NotificationItemVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    public static final String USER_ID_ATTRIBUTE = "notification.userId";
    public static final String TOKEN_ID_ATTRIBUTE = "notification.tokenId";
    public static final String TOKEN_EXPIRES_AT_ATTRIBUTE = "notification.tokenExpiresAt";
    private static final Logger log = LoggerFactory.getLogger(NotificationWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final AuthTokenStore authTokenStore;
    private final Map<Long, Set<WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();

    public NotificationWebSocketHandler(ObjectMapper objectMapper, AuthTokenStore authTokenStore) {
        this.objectMapper = objectMapper;
        this.authTokenStore = authTokenStore;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = userId(session);
        if (userId == null || tokenId(session) == null || tokenExpiresAt(session) == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        sessionsByUser.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        unregister(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
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
