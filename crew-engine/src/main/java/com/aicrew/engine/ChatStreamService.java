package com.aicrew.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** SSE 实时推送：按 taskId 维护订阅者，消息/状态变化时推给前端 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatStreamService {

    private final ObjectMapper om;

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long taskId) {
        SseEmitter emitter = new SseEmitter(0L); // 不超时
        List<SseEmitter> list = emitters.computeIfAbsent(taskId, k -> new CopyOnWriteArrayList<>());
        list.add(emitter);
        Runnable remove = () -> list.remove(emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            remove.run();
        }
        return emitter;
    }

    public void push(Long taskId, String event, Object data) {
        List<SseEmitter> list = emitters.get(taskId);
        if (list == null || list.isEmpty()) {
            return;
        }
        String json;
        try {
            json = om.writeValueAsString(data);
        } catch (Exception e) {
            log.warn("SSE 数据序列化失败", e);
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name(event).data(json));
            } catch (Exception e) {
                list.remove(emitter);
            }
        }
    }

}
