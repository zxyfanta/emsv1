package com.cdutetc.ems.service;

import com.cdutetc.ems.dto.event.DeviceDataEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE推送服务
 * 管理所有SSE连接，向客户端推送实时设备数据
 */
@Slf4j
@Service
public class SseEmitterService {

    // 存储所有活跃的SSE连接: companyId -> List<SseEmitter>
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> companyEmitters = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 为指定企业创建SSE连接
     */
    public SseEmitter createEmitter(Long companyId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // 30分钟超时

        companyEmitters.computeIfAbsent(companyId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // 设置完成和超时回调
        emitter.onCompletion(() -> removeEmitter(companyId, emitter));
        emitter.onTimeout(() -> removeEmitter(companyId, emitter));

        // 发送连接成功消息
        sendMessage(emitter, "connected", Map.of(
            "message", "SSE连接成功",
            "companyId", companyId,
            "timestamp", System.currentTimeMillis()
        ));

        log.info("创建SSE连接: companyId={}, 当前连接数={}", companyId,
                companyEmitters.getOrDefault(companyId, new CopyOnWriteArrayList<>()).size());

        return emitter;
    }

    /**
     * 向指定企业的所有连接推送设备数据
     * 数据结构扁平化：直接将设备数据展开到顶层，避免前端二次解析
     */
    public void broadcastDeviceData(Long companyId, DeviceDataEvent event) {
        CopyOnWriteArrayList<SseEmitter> emitters = companyEmitters.get(companyId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        // 构建扁平化的数据负载
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("deviceCode", event.getDeviceCode());
        payload.put("deviceType", event.getDeviceType());
        payload.put("timestamp", event.getEventTime().toString());  // 使用 getEventTime() 替代 getTimestamp()

        // 将实际数据直接展开到顶层
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> actualData = objectMapper.readValue(
                objectMapper.writeValueAsString(event.getData()),
                Map.class
            );
            payload.putAll(actualData);
        } catch (Exception e) {
            log.error("解析设备数据失败", e);
            return;
        }

        // 推送给该企业的所有连接
        emitters.forEach(emitter -> {
            try {
                sendMessage(emitter, event.getEventType(), payload);
            } catch (Exception e) {
                log.error("SSE推送失败，将移除该连接", e);
                removeEmitter(companyId, emitter);
            }
        });
    }

    /**
     * 广播给所有企业（用于系统级通知）
     */
    public void broadcastToAll(DeviceDataEvent event) {
        companyEmitters.keySet().forEach(companyId ->
            broadcastDeviceData(companyId, event)
        );
    }

    /**
     * 发送消息
     */
    private void sendMessage(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                .name(eventName)
                .data(data)
                .id(String.valueOf(System.currentTimeMillis()))
                .reconnectTime(3000) // 断线3秒后重连
            );
        } catch (IOException e) {
            log.error("发送SSE消息失败", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 移除失效的连接
     */
    private void removeEmitter(Long companyId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = companyEmitters.get(companyId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                companyEmitters.remove(companyId);
            }
        }
    }

    /**
     * 获取当前连接数
     */
    public int getConnectionCount() {
        return companyEmitters.values().stream()
                .mapToInt(list -> list.size())
                .sum();
    }

    /**
     * 获取指定企业的连接数
     */
    public int getConnectionCount(Long companyId) {
        CopyOnWriteArrayList<SseEmitter> emitters = companyEmitters.get(companyId);
        return emitters == null ? 0 : emitters.size();
    }
}
