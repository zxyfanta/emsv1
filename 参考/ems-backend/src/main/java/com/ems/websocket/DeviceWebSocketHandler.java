package com.ems.websocket;

import com.ems.dto.websocket.DeviceRealTimeData;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 设备WebSocket处理器
 *
 * @author EMS Team
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DeviceWebSocketHandler implements WebSocketHandler {

    private final ObjectMapper objectMapper;

    // 存储设备ID到会话的映射
    private final Map<String, Set<WebSocketSession>> deviceSessions = new ConcurrentHashMap<>();

    // 存储企业ID到会话的映射
    private final Map<String, Set<WebSocketSession>> enterpriseSessions = new ConcurrentHashMap<>();

    // 存储会话到路径参数的映射
    private final Map<WebSocketSession, Map<String, String>> sessionParams = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String path = session.getUri().getPath();
        Map<String, String> params = extractPathParams(session.getUri());
        sessionParams.put(session, params);

        if (path.contains("/device/")) {
            String deviceId = params.get("deviceId");
            if (StringUtils.hasText(deviceId)) {
                deviceSessions.computeIfAbsent(deviceId, k -> ConcurrentHashMap.newKeySet()).add(session);
                log.info("Device {} monitor connected: {}", deviceId, session.getId());
            }
        } else if (path.contains("/enterprise/")) {
            String enterpriseId = params.get("enterpriseId");
            if (StringUtils.hasText(enterpriseId)) {
                enterpriseSessions.computeIfAbsent(enterpriseId, k -> ConcurrentHashMap.newKeySet()).add(session);
                log.info("Enterprise {} monitor connected: {}", enterpriseId, session.getId());
            }
        }

        // 发送连接成功消息
        sendConnectionMessage(session, true);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        // 处理客户端发送的消息，这里暂时不需要处理
        log.debug("Received message from session {}: {}", session.getId(), message.getPayload());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
        cleanupSession(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket connection closed for session {}: {}", session.getId(), status);
        cleanupSession(session);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 广播设备数据到设备监控客户端
     */
    public void broadcastDeviceData(String deviceId, Object data) {
        Set<WebSocketSession> sessions = deviceSessions.get(deviceId);
        if (sessions != null) {
            String message = createMessage("device_data", data);
            broadcastToSessions(sessions, message);
        }
    }

    /**
     * 广播企业设备数据到企业监控客户端
     */
    public void broadcastEnterpriseData(String enterpriseId, Object data) {
        Set<WebSocketSession> sessions = enterpriseSessions.get(enterpriseId);
        if (sessions != null) {
            String message = createMessage("enterprise_data", data);
            broadcastToSessions(sessions, message);
        }
    }

    /**
     * 发送设备状态更新
     */
    public void sendDeviceStatusUpdate(String deviceId, String status) {
        Map<String, Object> statusData = Map.of(
            "deviceId", deviceId,
            "status", status,
            "timestamp", LocalDateTime.now().toString()
        );

        broadcastDeviceData(deviceId, statusData);
    }

    /**
     * 清理会话
     */
    private void cleanupSession(WebSocketSession session) {
        // 从所有映射中移除会话
        deviceSessions.values().forEach(sessions -> sessions.remove(session));
        enterpriseSessions.values().forEach(sessions -> sessions.remove(session));
        sessionParams.remove(session);
    }

    /**
     * 提取路径参数
     */
    private Map<String, String> extractPathParams(URI uri) {
        String path = uri.getPath();
        Map<String, String> params = new ConcurrentHashMap<>();

        if (path.contains("/device/")) {
            String[] parts = path.split("/");
            for (int i = 0; i < parts.length; i++) {
                if ("device".equals(parts[i]) && i + 1 < parts.length) {
                    params.put("deviceId", parts[i + 1]);
                    break;
                }
            }
        } else if (path.contains("/enterprise/")) {
            String[] parts = path.split("/");
            for (int i = 0; i < parts.length; i++) {
                if ("enterprise".equals(parts[i]) && i + 1 < parts.length) {
                    params.put("enterpriseId", parts[i + 1]);
                    break;
                }
            }
        }

        return params;
    }

    /**
     * 创建WebSocket消息
     */
    private String createMessage(String type, Object data) {
        try {
            Map<String, Object> message = Map.of(
                "type", type,
                "data", data,
                "timestamp", LocalDateTime.now().toString()
            );
            return objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("Error creating WebSocket message", e);
            return "{\"type\":\"error\",\"message\":\"Failed to create message\"}";
        }
    }

    /**
     * 向会话集合广播消息
     */
    private void broadcastToSessions(Set<WebSocketSession> sessions, String message) {
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        sessions.removeIf(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                    return false;
                } else {
                    return true; // 移除已关闭的会话
                }
            } catch (Exception e) {
                log.error("Error sending message to session {}: {}", session.getId(), e.getMessage());
                return true; // 移除出错的会话
            }
        });
    }

    /**
     * 发送连接状态消息
     */
    private void sendConnectionMessage(WebSocketSession session, boolean connected) {
        try {
            String message = createMessage("connection", Map.of(
                "connected", connected,
                "sessionId", session.getId(),
                "timestamp", LocalDateTime.now().toString()
            ));
            session.sendMessage(new TextMessage(message));
        } catch (IOException e) {
            log.error("Error sending connection message", e);
        }
    }

    /**
     * 获取连接统计信息
     */
    public Map<String, Object> getConnectionStats() {
        return Map.of(
            "totalDeviceConnections", deviceSessions.values().stream().mapToInt(Set::size).sum(),
            "totalEnterpriseConnections", enterpriseSessions.values().stream().mapToInt(Set::size).sum(),
            "deviceCount", deviceSessions.size(),
            "enterpriseCount", enterpriseSessions.size()
        );
    }
}