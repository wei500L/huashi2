package com.huashi.eftransfer.app.modules.notification.ws;

import com.huashi.eftransfer.app.common.config.CorsProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class NotificationWebSocketConfig implements WebSocketConfigurer {

    private final NotificationWebSocketHandler notificationWebSocketHandler;
    private final NotificationAuthHandshakeInterceptor notificationAuthHandshakeInterceptor;
    private final CorsProperties corsProperties;

    public NotificationWebSocketConfig(
            NotificationWebSocketHandler notificationWebSocketHandler,
            NotificationAuthHandshakeInterceptor notificationAuthHandshakeInterceptor,
            CorsProperties corsProperties
    ) {
        this.notificationWebSocketHandler = notificationWebSocketHandler;
        this.notificationAuthHandshakeInterceptor = notificationAuthHandshakeInterceptor;
        this.corsProperties = corsProperties;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationWebSocketHandler, "/ws/notifications")
                .addInterceptors(notificationAuthHandshakeInterceptor)
                .setAllowedOrigins(corsProperties.getAllowedOrigins().toArray(String[]::new));
    }
}
