package com.flashticket.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

/**
 * SSE 连接注册表（M14 P0-C/D）。
 *
 * <p>每个登录用户可持有多个在线连接（多设备/多标签页），按 userId 聚合。
 * 连接对象只保存在本实例内存（Java 对象不可序列化到 Redis）；多实例扩展时
 * 用 Redis Pub/Sub 分发在线事件（P1）。推送为尽力而为：SSE 失败不重试业务
 * 消费，离线用户通过 REST 补查。
 */
@Component
public class SseConnectionRegistry {

    private static final Logger log = LoggerFactory.getLogger(SseConnectionRegistry.class);

    private final ObjectMapper objectMapper;
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseConnectionRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 注册一个用户连接，返回待返回给客户端的 emitter。 */
    public SseEmitter register(Long userId) {
        SseEmitter emitter = new SseEmitter(0L);
        List<SseEmitter> list = emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());
        list.add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(e -> remove(userId, emitter));
        return emitter;
    }

    /** 向某用户的所有在线连接推送指定事件（data 为任意对象，序列化为 JSON）。 */
    public void pushToUser(Long userId, String eventName, Object data) {
        List<SseEmitter> list = emitters.get(userId);
        if (list == null || list.isEmpty()) {
            return;
        }
        String payload = toJson(data);
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
            } catch (Exception e) {
                remove(userId, emitter);
            }
        }
    }

    /** 用户当前在线连接数。 */
    public int activeCount(Long userId) {
        List<SseEmitter> list = emitters.get(userId);
        return list == null ? 0 : list.size();
    }

    /** 心跳：定时向所有连接发送空事件，探测死连接并及时清理。 */
    @Scheduled(fixedRate = 20000)
    public void heartbeat() {
        for (Map.Entry<Long, List<SseEmitter>> entry : emitters.entrySet()) {
            for (SseEmitter emitter : entry.getValue()) {
                try {
                    emitter.send(SseEmitter.event().name("heartbeat").data(""));
                } catch (Exception e) {
                    remove(entry.getKey(), emitter);
                }
            }
        }
    }

    private void remove(Long userId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(userId);
        if (list == null) {
            return;
        }
        list.remove(emitter);
        if (list.isEmpty()) {
            emitters.remove(userId, list);
        }
    }

    private String toJson(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.warn("SSE 事件序列化失败: {}", e.getMessage());
            return "{}";
        }
    }
}
