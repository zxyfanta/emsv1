package com.ems.config;

import com.ems.websocket.DeviceWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置
 *
 * @author EMS Team
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final DeviceWebSocketHandler deviceWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 设备监控WebSocket
        registry.addHandler(deviceWebSocketHandler, "/ws/device/{deviceId}")
                .addHandler(deviceWebSocketHandler, "/ws/enterprise/{enterpriseId}")
                .setAllowedOrigins("*") // 在生产环境中应该设置具体的域名
                .withSockJS(); // 启用SockJS支持
    }
}