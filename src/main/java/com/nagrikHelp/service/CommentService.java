package com.nagrikHelp.service;

import com.nagrikHelp.dto.CommentResponseDto;
import com.nagrikHelp.model.Comment;
import com.nagrikHelp.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final IssueStreamService issueStreamService;

    public CommentResponseDto addComment(String issueId, String userId, String userName, String text) {
        long now = System.currentTimeMillis();
        Comment c = Comment.builder()
                .issueId(issueId)
                .userId(userId)
                .userName(userName)
                .text(text)
                .createdAt(now)
                .updatedAt(now)
                .build();
        commentRepository.save(c);
        CommentResponseDto dto = CommentResponseDto.from(c);
        long count = commentRepository.countByIssueId(issueId);
        issueStreamService.broadcastComment(issueId, count, dto);
        return dto;
    }

    public List<CommentResponseDto> getComments(String issueId, int page, int size) {
        return commentRepository.findByIssueIdOrderByCreatedAtDesc(issueId, PageRequest.of(page, size))
                .stream().map(CommentResponseDto::from).toList();
    }

    public long count(String issueId) {
        return commentRepository.countByIssueId(issueId);
    }

    public List<CommentResponseDto> recent(String issueId, int limit) {
        return getComments(issueId, 0, limit);
    }
}
