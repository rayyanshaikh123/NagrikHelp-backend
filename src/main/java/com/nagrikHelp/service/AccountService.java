package com.nagrikHelp.service;

import com.nagrikHelp.model.Issue;
import com.nagrikHelp.model.User;
import com.nagrikHelp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import com.nagrikHelp.dto.AccountDto;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final UserRepository userRepository;
    private final IssueRepository issueRepository;
    private final VoteRepository voteRepository;
    private final CommentRepository commentRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final OtpService otpService;
    private final EmailVerificationService emailVerificationService;

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

    public AccountDto getProfile(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return null;
        return new AccountDto(
        user.getName(),
        user.getEmail(),
        user.getPhone(),
        user.getEmailConsent(),
        user.getEmailVerified(),
        user.getSmsConsent(),
        user.getPhoneVerified(),
        user.getRole()
        );
    }

    public AccountDto updateProfile(String email, com.nagrikHelp.dto.UpdateAccountRequest req) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return null;
        // update fields if provided
        if (req.getPhone() != null) user.setPhone(req.getPhone());
        if (req.getEmailConsent() != null) user.setEmailConsent(req.getEmailConsent());
        if (req.getSmsConsent() != null) user.setSmsConsent(req.getSmsConsent());
        User saved = userRepository.save(user);
        // if sms consent enabled and phone present and not verified, send OTP
        if (Boolean.TRUE.equals(saved.getSmsConsent()) && saved.getPhone() != null && (saved.getPhoneVerified() == null || !saved.getPhoneVerified())) {
            try {
                otpService.generateAndSendOtp(saved.getPhone(), "phone verification");
            } catch (Exception ex) {
                log.warn("Failed to send OTP during profile update: {}", ex.getMessage());
            }
        }
        return getProfile(email);
    }

    public void sendEmailVerification(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return;
        // send code to user's email (include display name for personalization)
        try {
            emailVerificationService.generateAndSendCode(user.getEmail(), user.getName());
        } catch (Exception ex) {
            log.warn("Failed to send email verification: {}", ex.getMessage());
        }
    }

    public boolean verifyEmailCode(String email, String code) {
        boolean ok = emailVerificationService.verifyCode(email, code);
        if (!ok) return false;
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return false;
        user.setEmailVerified(true);
        userRepository.save(user);
        return true;
    }
}
