package com.nagrikHelp.repository;

import com.nagrikHelp.model.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CommentRepository extends MongoRepository<Comment, String> {
    List<Comment> findByIssueIdOrderByCreatedAtDesc(String issueId, Pageable pageable);
    long countByIssueId(String issueId);
}
