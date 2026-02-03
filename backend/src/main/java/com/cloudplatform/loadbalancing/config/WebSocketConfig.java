package com.cloudplatform.loadbalancing.config;

import com.cloudplatform.loadbalancing.websocket.MetricsWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket Configuration
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
@SuppressWarnings("null")
public class WebSocketConfig implements WebSocketConfigurer {

    private final MetricsWebSocketHandler metricsWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(metricsWebSocketHandler, "/ws/metrics")
                .setAllowedOrigins("*"); // Configure appropriately for production
    }
}
