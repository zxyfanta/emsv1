package com.cdutetc.ems.controller;

import com.cdutetc.ems.entity.User;
import com.cdutetc.ems.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * SSE推送控制器
 * 提供SSE订阅端点
 */
@Slf4j
@RestController
@RequestMapping("/sse")
@RequiredArgsConstructor
public class SseController {

    private final SseEmitterService sseEmitterService;

    /**
     * 订阅SSE推送
     *
     * 前端调用: new EventSource('/api/sse/subscribe')
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        // 获取当前用户的企业ID
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        Long companyId = currentUser.getCompany().getId();

        log.info("用户订阅SSE: userId={}, companyId={}", currentUser.getId(), companyId);

        return sseEmitterService.createEmitter(companyId);
    }

    /**
     * 获取连接状态（用于调试）
     */
    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        Long companyId = currentUser.getCompany().getId();

        return ResponseEntity.ok(Map.of(
            "totalConnections", sseEmitterService.getConnectionCount(),
            "companyConnections", sseEmitterService.getConnectionCount(companyId),
            "companyId", companyId
        ));
    }
}
