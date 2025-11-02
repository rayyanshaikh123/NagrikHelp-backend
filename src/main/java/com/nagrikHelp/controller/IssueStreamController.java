package com.nagrikHelp.controller;

import com.nagrikHelp.service.IssueStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/issues/{issueId}/stream")
@RequiredArgsConstructor
public class IssueStreamController {

    private final IssueStreamService issueStreamService;

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String issueId) {
        return issueStreamService.addEmitter(issueId);
    }
}
