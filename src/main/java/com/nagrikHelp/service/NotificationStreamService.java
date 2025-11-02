package com.nagrikHelp.service;

import com.nagrikHelp.model.UserNotification;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NotificationStreamService {

    private static final long TIMEOUT = 30 * 60 * 1000L; // 30 min
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>(); // key = userEmail

    public SseEmitter addEmitter(String userEmail) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        emitters.computeIfAbsent(userEmail, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(userEmail, emitter));
        emitter.onTimeout(() -> remove(userEmail, emitter));
        emitter.onError(e -> remove(userEmail, emitter));
        try {
            emitter.send(SseEmitter.event().name("ping").data("ok").reconnectTime(5000));
        } catch (IOException ignored) {}
        return emitter;
    }

    private void remove(String userEmail, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(userEmail);
        if (list != null) list.remove(emitter);
    }

    public void broadcast(UserNotification n) {
        if (n == null || n.getUserEmail() == null) return;
        List<SseEmitter> list = emitters.get(n.getUserEmail());
        if (list == null || list.isEmpty()) return;
        String json = toJson(n);
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(json, MediaType.APPLICATION_JSON));
            } catch (IOException e) {
                remove(n.getUserEmail(), emitter);
            }
        }
    }

    private String esc(String v) { return v == null ? "" : v.replace("\\", "\\\\").replace("\"", "\\\""); }
    private String toJson(UserNotification n) {
        return new StringBuilder(160)
                .append('{')
                .append("\"id\":\"").append(esc(n.getId())).append('\"')
                .append(",\"issueId\":").append(n.getIssueId()==null?"null":"\""+esc(n.getIssueId())+"\"")
                .append(",\"type\":\"").append(esc(n.getType())).append('\"')
                .append(",\"message\":\"").append(esc(n.getMessage())).append('\"')
                .append(",\"createdAt\":").append(n.getCreatedAt())
                .append(",\"read\":").append(n.isRead())
                .append('}').toString();
    }
}
