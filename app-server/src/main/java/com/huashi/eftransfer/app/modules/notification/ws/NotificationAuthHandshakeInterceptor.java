package com.huashi.eftransfer.app.modules.notification.ws;

import com.huashi.eftransfer.app.common.security.JwtPrincipal;
import com.huashi.eftransfer.app.common.security.JwtTokenProvider;
import com.huashi.eftransfer.app.common.security.store.AuthTokenStore;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

@Component
public class NotificationAuthHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthTokenStore authTokenStore;

    public NotificationAuthHandshakeInterceptor(JwtTokenProvider jwtTokenProvider, AuthTokenStore authTokenStore) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.authTokenStore = authTokenStore;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        try {
            JwtPrincipal principal = jwtTokenProvider.parseAccessToken(token);
            if (authTokenStore.isAccessTokenBlacklisted(principal.tokenId())) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            attributes.put(NotificationWebSocketHandler.USER_ID_ATTRIBUTE, principal.userId());
            return true;
        } catch (JwtException exception) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        // no-op
    }

    private String resolveToken(ServerHttpRequest request) {
        List<String> authorizationValues = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (authorizationValues != null) {
            for (String value : authorizationValues) {
                if (value != null && value.startsWith("Bearer ")) {
                    return value.substring(7);
                }
            }
        }
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String accessToken = servletRequest.getServletRequest().getParameter("access_token");
            if (StringUtils.hasText(accessToken)) {
                return accessToken;
            }
        }
        return null;
    }
}
