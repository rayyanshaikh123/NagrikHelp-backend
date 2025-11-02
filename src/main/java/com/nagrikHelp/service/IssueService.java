package com.nagrikHelp.service;

import com.nagrikHelp.dto.*;
import com.nagrikHelp.model.Issue;
import com.nagrikHelp.model.IssueStatus;
import com.nagrikHelp.model.User;
import com.nagrikHelp.repository.IssueRepository;
import com.nagrikHelp.repository.UserRepository;
import com.nagrikHelp.service.VoteService;
import com.nagrikHelp.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IssueService {

    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final VoteService voteService;
    private final CommentService commentService;
    private final NotificationService notificationService;

    // Phase 1 existing API (kept for compatibility)
    public IssueResponse createIssue(String createdBy, CreateIssueRequest req) {
        long now = System.currentTimeMillis();
        Issue issue = Issue.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .location(req.getLocation())
                .photoUrl(req.getPhotoUrl())
                .status(IssueStatus.OPEN)
                .createdBy(createdBy)
                .createdAt(now)
                .updatedAt(new Date(now))
                .shareToken(UUID.randomUUID().toString())
                .build();
        issueRepository.save(issue);
        return IssueResponse.from(issue);
    }

    public List<IssueResponse> getIssuesForUser(String email) {
        return issueRepository.findByCreatedByOrderByUpdatedAtDesc(email)
                .stream().map(IssueResponse::from).toList();
    }

    // New Phase 2 variant returning enriched DTOs (with category, image, votes, comments)
    public List<IssueResponseDto> getIssuesForUserDto(String email) {
        return issueRepository.findByCreatedByOrderByUpdatedAtDesc(email)
                .stream().map(i -> {
                    IssueVoteSummaryDto vs = voteService.summarize(i.getId(), "__anon__");
                    IssueResponseDto dto = IssueResponseDto.from(i, vs.getUpVotes(), vs.getDownVotes(), null);
                    long cCount = commentService.count(i.getId());
                    dto.withComments(cCount, commentService.recent(i.getId(), 3));
                    return dto;
                }).toList();
    }

    public List<IssueResponse> getAllIssuesCompat() {
        return issueRepository.findAllByOrderByUpdatedAtDesc()
                .stream().map(IssueResponse::from).toList();
    }

    public Optional<IssueResponse> updateIssue(String id, UpdateIssueRequest req) {
        return issueRepository.findById(id).map(existing -> {
            IssueStatus prev = existing.getStatus();
            if (req.getStatus() != null && !req.getStatus().isBlank()) {
                existing.setStatus(parseStatus(req.getStatus()));
            }
            existing.setUpdatedAt(new Date());
            issueRepository.save(existing);
            // If status changed, notify followers and owner
            try {
                if (prev != existing.getStatus()) {
                    try { notificationService.notifyFollowersOnStatusChange(existing); } catch (Exception ex) { log.warn("notifyFollowersOnStatusChange failed: {}", ex.getMessage()); }
                    try { notificationService.notifyOwnerOnStatusChange(existing); } catch (Exception ex) { log.warn("notifyOwnerOnStatusChange failed: {}", ex.getMessage()); }
                }
            } catch (Exception ignore) {}
            return IssueResponse.from(existing);
        });
    }

    private IssueStatus parseStatus(String value) {
        String v = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        return IssueStatus.valueOf(v);
    }

    // Phase 2 API
    public IssueResponseDto createIssue(IssueRequestDto dto, UserDetails userDetails) {
        long now = System.currentTimeMillis();
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email).orElse(null);
        Issue issue = Issue.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .category(dto.getCategory())
                .imageBase64(dto.getImageBase64())
                .location(dto.getLocation())
                .status(IssueStatus.OPEN)
                .createdBy(email)
                .createdById(user != null ? user.getId() : null)
                .createdByName(user != null ? user.getName() : null)
                .createdAt(now)
                .updatedAt(new Date(now))
                .shareToken(UUID.randomUUID().toString())
                .build();
        issueRepository.save(issue);
        return IssueResponseDto.from(issue);
    }

    public List<IssueResponseDto> getAllIssues() {
        return issueRepository.findAllByOrderByUpdatedAtDesc()
                .stream().map(i -> {
                    IssueVoteSummaryDto vs = voteService.summarize(i.getId(), "__anon__");
                    IssueResponseDto dto = IssueResponseDto.from(i, vs.getUpVotes(), vs.getDownVotes(), null);
                    long cCount = commentService.count(i.getId());
                    dto.withComments(cCount, commentService.recent(i.getId(), 3));
                    return dto;
                }).toList();
    }

    public Optional<IssueResponseDto> getIssueById(String id, String userId) {
        return issueRepository.findById(id).map(i -> {
            IssueVoteSummaryDto vs = voteService.summarize(i.getId(), userId == null ? "__anon__" : userId);
            IssueResponseDto dto = IssueResponseDto.from(i, vs.getUpVotes(), vs.getDownVotes(), vs.getUserVote());
            long cCount = commentService.count(i.getId());
            dto.withComments(cCount, commentService.recent(i.getId(), 20));
            return dto;
        });
    }

    // Retain old signature for existing controller usage
    public Optional<IssueResponseDto> getIssueById(String id) {
        return getIssueById(id, null);
    }

    public Optional<PublicIssueResponse> getIssueByShareToken(String token) {
        return getIssueByShareToken(token, null, null);
    }

    public Optional<PublicIssueResponse> getIssueByShareToken(String token, String email, String phone) {
        if (token == null || token.isBlank()) return Optional.empty();
        return issueRepository.findByShareToken(token.trim()).map(i -> {
            // Ensure backward compatibility for pre-existing issues without token
            if (i.getShareToken() == null) {
                i.setShareToken(UUID.randomUUID().toString());
                issueRepository.save(i);
            }
            IssueVoteSummaryDto vs = voteService.summarize(i.getId(), "__anon__");
            return PublicIssueResponse.from(i, vs.getUpVotes(), email, phone);
        });
    }

    // Phase 3 admin methods --------------------------------------------------

    private static final Map<IssueStatus, Set<IssueStatus>> VALID_TRANSITIONS = Map.of(
            IssueStatus.OPEN, Set.of(IssueStatus.IN_PROGRESS),
            IssueStatus.IN_PROGRESS, Set.of(IssueStatus.RESOLVED),
            IssueStatus.RESOLVED, Set.of() // terminal
    );

    public Optional<IssueResponse> updateIssueStatus(String issueId, IssueStatus nextStatus, String adminUser) {
        log.info("updateIssueStatus: issueId={}, nextStatus={}, admin={}", issueId, nextStatus, adminUser);
        return issueRepository.findById(issueId).map(issue -> {
            IssueStatus current = issue.getStatus();
            log.debug("updateIssueStatus: currentStatus={}, followers(emails)={}, owner={} ", current,
                    issue.getFollowerEmails() == null ? 0 : issue.getFollowerEmails().size(), issue.getCreatedBy());
            if (current == nextStatus) {
                issue.setUpdatedAt(new Date());
                issueRepository.save(issue);
                try {
                    log.debug("updateIssueStatus: status unchanged; notifying owner for id={}", issueId);
                    notificationService.notifyOwnerOnStatusChange(issue);
                } catch (Exception ex) { log.warn("updateIssueStatus notify (no change) failed: {}", ex.getMessage()); }
                return IssueResponse.from(issue);
            }
            Set<IssueStatus> allowed = VALID_TRANSITIONS.getOrDefault(current, Collections.emptySet());
            if (!allowed.contains(nextStatus)) {
                log.warn("updateIssueStatus: invalid transition {} -> {} for issueId={}", current, nextStatus, issueId);
                throw new IllegalStateException("Invalid status transition: " + current + " -> " + nextStatus);
            }
            issue.setStatus(nextStatus);
            issue.setUpdatedAt(new Date());
            issueRepository.save(issue);
            log.info("updateIssueStatus: saved new status {} for issueId={}", nextStatus, issueId);
            try {
                // Admin-triggered status changes notify the owner only (not followers)
                notificationService.notifyOwnerOnStatusChange(issue);
                log.debug("updateIssueStatus: owner notified for issueId={}", issueId);
            } catch (Exception ex) { log.warn("updateIssueStatus notify failed issueId={} error={}", issueId, ex.getMessage()); }
            return IssueResponse.from(issue);
        });
    }

    // Admin-specific update: similar to updateIssue but records admin action and notifies owner only
    public Optional<IssueResponse> updateIssueAsAdmin(String id, UpdateIssueRequest req, String adminUser) {
        return issueRepository.findById(id).map(existing -> {
            IssueStatus prev = existing.getStatus();
            if (req.getStatus() != null && !req.getStatus().isBlank()) {
                existing.setStatus(parseStatus(req.getStatus()));
            }
            existing.setUpdatedAt(new Date());
            issueRepository.save(existing);
            // If status changed, notify the owner only
            try {
                if (prev != existing.getStatus()) {
                    try { notificationService.notifyOwnerOnStatusChange(existing); } catch (Exception ex) { log.warn("notifyOwnerOnStatusChange failed: {}", ex.getMessage()); }
                }
            } catch (Exception ignore) {}
            return IssueResponse.from(existing);
        });
    }

    public Optional<IssueResponse> assignIssue(String issueId, String assignee, String adminUser) {
        return issueRepository.findById(issueId).map(issue -> {
            issue.setAssignedTo(assignee == null || assignee.isBlank() ? null : assignee.trim());
            issue.setUpdatedAt(new Date());
            issueRepository.save(issue);
            return IssueResponse.from(issue);
        });
    }

    public List<IssueResponse> getIssuesByStatus(IssueStatus status) {
        if (status == null) {
            return issueRepository.findAllByOrderByUpdatedAtDesc().stream().map(IssueResponse::from).toList();
        }
        return issueRepository.findByStatusOrderByUpdatedAtDesc(status).stream().map(IssueResponse::from).toList();
    }

    // Monthly resolved report
    public MonthlyResolvedReport monthlyResolvedReport(int year, int month) { // month 1-12
        ZoneId zone = ZoneId.systemDefault();
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1);
        Date startDate = Date.from(start.atStartOfDay(zone).toInstant());
        Date endDate = Date.from(end.atStartOfDay(zone).toInstant());
        List<Issue> resolved = issueRepository.findByStatusAndUpdatedAtBetween(IssueStatus.RESOLVED, startDate, endDate);
        Map<LocalDate, Long> counts = resolved.stream().collect(Collectors.groupingBy(i -> i.getUpdatedAt().toInstant().atZone(zone).toLocalDate(), Collectors.counting()));
        List<MonthlyResolvedReport.DayCount> daily = new ArrayList<>();
        for (LocalDate d = start; d.isBefore(end); d = d.plusDays(1)) {
            long c = counts.getOrDefault(d, 0L);
            if (c > 0) {
                daily.add(new MonthlyResolvedReport.DayCount(d.toString(), c));
            }
        }
        long total = resolved.size();
        return new MonthlyResolvedReport(year, month, total, daily);
    }

    public Optional<IssueResponse> updateCitizenIssue(String userEmail, String issueId, CitizenUpdateIssueRequest req) {
        return issueRepository.findByIdAndCreatedBy(issueId, userEmail).map(issue -> {
            boolean changed = false;
            if (req.getTitle() != null && !req.getTitle().isBlank()) { issue.setTitle(req.getTitle().trim()); changed = true; }
            if (req.getDescription() != null && !req.getDescription().isBlank()) { issue.setDescription(req.getDescription().trim()); changed = true; }
            if (req.getLocation() != null && !req.getLocation().isBlank()) { issue.setLocation(req.getLocation().trim()); changed = true; }
            if (req.getCategory() != null) { issue.setCategory(req.getCategory()); changed = true; }
            if (req.getImageBase64() != null && !req.getImageBase64().isBlank()) { issue.setImageBase64(req.getImageBase64()); changed = true; }
            if (changed) {
                issue.setUpdatedAt(new Date());
                issueRepository.save(issue);
            }
            return IssueResponse.from(issue);
        });
    }

    public boolean deleteCitizenIssue(String userEmail, String issueId) {
        return issueRepository.findByIdAndCreatedBy(issueId, userEmail).map(i -> {
            issueRepository.deleteById(i.getId());
            return true;
        }).orElse(false);
    }

    public Optional<IssueResponse> followByShareToken(String token, FollowRequest req) {
        if (token == null || token.isBlank()) return Optional.empty();
        return issueRepository.findByShareToken(token.trim()).map(issue -> {
            boolean changed = false;
            if (req.getPhone() != null && !req.getPhone().isBlank()) {
                if (!issue.getFollowerPhones().contains(req.getPhone().trim())) {
                    issue.getFollowerPhones().add(req.getPhone().trim()); changed = true; }
            }
            if (req.getEmail() != null && !req.getEmail().isBlank()) {
                if (!issue.getFollowerEmails().contains(req.getEmail().trim())) {
                    issue.getFollowerEmails().add(req.getEmail().trim()); changed = true; }
            }
            if (req.getWebhookUrl() != null && !req.getWebhookUrl().isBlank()) {
                if (!issue.getFollowerWebhookUrls().contains(req.getWebhookUrl().trim())) {
                    issue.getFollowerWebhookUrls().add(req.getWebhookUrl().trim()); changed = true; }
            }
            if (changed) {
                issue.setUpdatedAt(new Date());
                issueRepository.save(issue);
            }
            return IssueResponse.from(issue);
        });
    }

    public Optional<IssueResponse> unfollowByShareToken(String token, FollowRequest req) {
        if (token == null || token.isBlank()) return Optional.empty();
        return issueRepository.findByShareToken(token.trim()).map(issue -> {
            boolean changed = false;
            if (req.getPhone() != null && issue.getFollowerPhones().remove(req.getPhone().trim())) changed = true;
            if (req.getEmail() != null && issue.getFollowerEmails().remove(req.getEmail().trim())) changed = true;
            if (req.getWebhookUrl() != null && issue.getFollowerWebhookUrls().remove(req.getWebhookUrl().trim())) changed = true;
            if (changed) {
                issue.setUpdatedAt(new Date());
                issueRepository.save(issue);
            }
            return IssueResponse.from(issue);
        });
    }
}
