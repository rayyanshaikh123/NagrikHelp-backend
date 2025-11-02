package com.nagrikHelp.repository;

import com.nagrikHelp.model.Issue;
import com.nagrikHelp.model.IssueStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface IssueRepository extends MongoRepository<Issue, String> {
    List<Issue> findByCreatedByOrderByUpdatedAtDesc(String createdBy);
    List<Issue> findAllByOrderByUpdatedAtDesc();
    List<Issue> findByStatusOrderByUpdatedAtDesc(IssueStatus status);
    // Added for monthly resolved report
    List<Issue> findByStatusAndUpdatedAtBetween(IssueStatus status, Date start, Date end);
    Optional<Issue> findByIdAndCreatedBy(String id, String createdBy);
    List<Issue> findByCreatedAtBetween(long start, long end);
    Optional<Issue> findByShareToken(String shareToken);
}
