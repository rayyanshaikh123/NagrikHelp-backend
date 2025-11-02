package com.nagrikHelp.repository;

import com.nagrikHelp.model.Vote;
import com.nagrikHelp.model.VoteValue;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface VoteRepository extends MongoRepository<Vote, String> {
    Optional<Vote> findByIssueIdAndUserId(String issueId, String userId);
    long countByIssueIdAndValue(String issueId, VoteValue value);
    List<Vote> findByIssueId(String issueId);
}
