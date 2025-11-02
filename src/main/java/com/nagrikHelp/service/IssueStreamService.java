package com.nagrikHelp.service;

import com.nagrikHelp.dto.CommentResponseDto;
import com.nagrikHelp.dto.IssueVoteSummaryDto;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class IssueStreamService {

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private static final long TIMEOUT = 30 * 60 * 1000L; // 30 min

    public SseEmitter addEmitter(String issueId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        emitters.computeIfAbsent(issueId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(issueId, emitter));
        emitter.onTimeout(() -> removeEmitter(issueId, emitter));
        emitter.onError(e -> removeEmitter(issueId, emitter));
        // send initial ping
        try {
            emitter.send(SseEmitter.event().name("ping").data("ok").reconnectTime(3000));
        } catch (IOException ignored) {}
        return emitter;
    }

    private void removeEmitter(String issueId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(issueId);
        if (list != null) list.remove(emitter);
    }

    public void broadcastVote(IssueVoteSummaryDto summary) {
        broadcast(summary.getIssueId(), "vote", String.format("{\"issueId\":\"%s\",\"upVotes\":%d,\"downVotes\":%d}", summary.getIssueId(), summary.getUpVotes(), summary.getDownVotes()));
    }

    public void broadcastComment(String issueId, long commentsCount, CommentResponseDto comment) {
        String payload = String.format("{\"issueId\":\"%s\",\"commentsCount\":%d,\"comment\":{\"id\":\"%s\",\"userName\":\"%s\",\"text\":%s,\"createdAt\":%d}}",
                issueId,
                commentsCount,
                comment.getId(),
                escape(comment.getUserName()),
                jsonString(comment.getText()),
                comment.getCreatedAt());
        broadcast(issueId, "comment", payload);
    }

    private void broadcast(String issueId, String eventName, String json) {
        List<SseEmitter> list = emitters.get(issueId);
        if (list == null || list.isEmpty()) return;
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(json, MediaType.APPLICATION_JSON));
            } catch (IOException e) {
                removeEmitter(issueId, emitter);
            }
        }
    }

    private String escape(String v) { return v == null ? "" : v.replace("\\", "\\\\").replace("\"", "\\\""); }
    private String jsonString(String v) { return v == null ? "\"\"" : "\"" + escape(v) + "\""; }
}
