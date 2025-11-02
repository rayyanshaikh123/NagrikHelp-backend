package com.nagrikHelp.service;

import com.nagrikHelp.dto.IssueVoteSummaryDto;
import com.nagrikHelp.model.Vote;
import com.nagrikHelp.model.VoteValue;
import com.nagrikHelp.repository.VoteRepository;
import com.nagrikHelp.repository.IssueRepository;
import com.nagrikHelp.model.Issue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteRepository voteRepository;
    private final IssueStreamService issueStreamService;
    private final IssueRepository issueRepository;
    private final NotificationService notificationService;

    public IssueVoteSummaryDto castVote(String issueId, String userId, VoteValue value) {
        long now = System.currentTimeMillis();
        Vote existing = voteRepository.findByIssueIdAndUserId(issueId, userId).orElse(null);
        if (existing != null) {
            if (existing.getValue() == value) {
                // toggle off (remove vote)
                voteRepository.delete(existing);
            } else {
                existing.setValue(value);
                existing.setUpdatedAt(now);
                voteRepository.save(existing);
            }
        } else {
            Vote v = Vote.builder()
                    .issueId(issueId)
                    .userId(userId)
                    .value(value)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            voteRepository.save(v);
        }
        long up = voteRepository.countByIssueIdAndValue(issueId, VoteValue.UP);
        long down = voteRepository.countByIssueIdAndValue(issueId, VoteValue.DOWN);
        String userVote = voteRepository.findByIssueIdAndUserId(issueId, userId)
                .map(v -> v.getValue().name())
                .orElse(null);
        IssueVoteSummaryDto summary = new IssueVoteSummaryDto(issueId, up, down, userVote);
        issueStreamService.broadcastVote(summary);
        try {
            Issue issue = issueRepository.findById(issueId).orElse(null);
            if (issue != null) {
                notificationService.notifyOwnerOnVote(issue, userId, up, down);
            }
        } catch (Exception ignored) {}
        return summary;
    }

    public IssueVoteSummaryDto summarize(String issueId, String userId) {
        long up = voteRepository.countByIssueIdAndValue(issueId, VoteValue.UP);
        long down = voteRepository.countByIssueIdAndValue(issueId, VoteValue.DOWN);
        String userVote = voteRepository.findByIssueIdAndUserId(issueId, userId)
                .map(v -> v.getValue().name())
                .orElse(null);
        return new IssueVoteSummaryDto(issueId, up, down, userVote);
    }
}
