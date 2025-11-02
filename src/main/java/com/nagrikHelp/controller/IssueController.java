package com.nagrikHelp.controller;

import com.nagrikHelp.dto.IssueRequestDto;
import com.nagrikHelp.dto.IssueResponseDto;
import com.nagrikHelp.service.IssueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/issues")
@RequiredArgsConstructor
public class IssueController {

    private final IssueService issueService;

    @PostMapping
    public ResponseEntity<IssueResponseDto> create(
            @Valid @RequestBody IssueRequestDto request,
            @AuthenticationPrincipal UserDetails user
    ) {
        IssueResponseDto created = issueService.createIssue(request, user);
        return ResponseEntity.ok(created);
    }

    @GetMapping
    public List<IssueResponseDto> all() {
        return issueService.getAllIssues();
    }

    @GetMapping("/{id}")
    public ResponseEntity<IssueResponseDto> getOne(@PathVariable String id) {
        return issueService.getIssueById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
