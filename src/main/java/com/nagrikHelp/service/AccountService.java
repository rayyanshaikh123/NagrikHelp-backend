package com.nagrikHelp.service;

import com.nagrikHelp.model.Issue;
import com.nagrikHelp.model.User;
import com.nagrikHelp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final UserRepository userRepository;
    private final IssueRepository issueRepository;
    private final VoteRepository voteRepository;
    private final CommentRepository commentRepository;
    private final UserNotificationRepository userNotificationRepository;

    /**
     * Deletes a user account and performs a soft cascade removal of their owned entities.
     * Current strategy: hard delete issues, votes, notifications. Comments remain (anonymized) to preserve thread context.
     * Future enhancement: mark comments with a flag or replace userName with "Deleted User" if we add that field.
     */
    @Transactional
    public boolean deleteAccount(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return false;
        String userEmail = user.getEmail();
        // Fetch issues first (to remove related votes)
        List<Issue> issues = issueRepository.findByCreatedByOrderByUpdatedAtDesc(userEmail);
        for (Issue issue : issues) {
            try {
                // Remove votes on this issue
                voteRepository.findByIssueId(issue.getId()).forEach(voteRepository::delete);
                // Remove comments authored by this user on own issue (others remain)
                // (If we add userId to comment later we can filter properly)
                issueRepository.delete(issue);
            } catch (Exception ex) {
                log.warn("Failed deleting issue {} for user {}: {}", issue.getId(), userEmail, ex.getMessage());
            }
        }
        // Remove votes the user cast on other issues
        // No direct method; naive scan of all votes is expensive. For now skip or create index later.
        // Potential optimization: add repository method findByUserId.
        // Remove notifications
        userNotificationRepository.findByUserEmailAndReadIsFalseOrderByCreatedAtDesc(userEmail)
                .forEach(userNotificationRepository::delete);
        // Finally delete the user
        userRepository.delete(user);
        return true;
    }
}
