package com.nagrikHelp.controller;

import com.nagrikHelp.dto.CommentRequestDto;
import com.nagrikHelp.dto.CommentResponseDto;
import com.nagrikHelp.dto.CommentsPageDto;
import com.nagrikHelp.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/issues/{issueId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentResponseDto> add(@PathVariable String issueId,
                                                  @Valid @RequestBody CommentRequestDto req,
                                                  @AuthenticationPrincipal UserDetails user) {
        String userId = user.getUsername();
        String userName = user.getUsername(); // adjust if User entity has name lookup
        CommentResponseDto created = commentService.addComment(issueId, userId, userName, req.getText());
        return ResponseEntity.ok(created);
    }

    @GetMapping
    public ResponseEntity<CommentsPageDto> list(@PathVariable String issueId,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        List<CommentResponseDto> items = commentService.getComments(issueId, page, size);
        long total = commentService.count(issueId);
        return ResponseEntity.ok(new CommentsPageDto(issueId, page, size, total, items));
    }
}
