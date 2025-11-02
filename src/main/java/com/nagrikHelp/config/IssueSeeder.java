package com.nagrikHelp.config;

import com.nagrikHelp.model.Issue;
import com.nagrikHelp.model.IssueCategory;
import com.nagrikHelp.model.IssueStatus;
import com.nagrikHelp.repository.IssueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class IssueSeeder {

    private final IssueRepository issueRepository;

    @Value("${app.seed.enabled:false}")
    private boolean seedEnabled;

    @Value("${app.seed.target:200}")
    private int targetCount;

    private static final String[] USERS = {
            "seed_user_1@example.com",
            "seed_user_2@example.com",
            "seed_user_3@example.com",
            "seed_user_4@example.com",
            "seed_user_5@example.com"
    };
    private static final String[] USER_NAMES = {
            "Seed User 1","Seed User 2","Seed User 3","Seed User 4","Seed User 5"
    };
    private static final String[] WORDS = {
            "civic","issue","road","repair","urgent","public","safety","waste","light","water","update","local","community","status","pending","report","infrastructure","damage","clean","hazard"};

    private final SecureRandom random = new SecureRandom();

    @EventListener(ApplicationReadyEvent.class)
    public void seedIfNeeded() {
        if (!seedEnabled) {
            log.info("IssueSeeder: seeding disabled (app.seed.enabled=false)");
            return;
        }
        long existing = issueRepository.count();
        if (existing >= targetCount) {
            log.info("IssueSeeder: existing issues ({} ) >= target ({}), skip seeding", existing, targetCount);
            return;
        }
        int toCreate = (int) (targetCount - existing);
        log.info("IssueSeeder: creating {} demo issues (current={}, target={})", toCreate, existing, targetCount);
        List<Issue> batch = new ArrayList<>(toCreate);
        long now = System.currentTimeMillis();
        for (int i = 0; i < toCreate; i++) {
            int userIdx = i % USERS.length;
            long createdAt = now - random.nextInt(14 * 24 * 3600 * 1000); // within last 14 days
            IssueCategory category = IssueCategory.values()[random.nextInt(IssueCategory.values().length)];
            String title = "Auto Issue #" + (existing + i + 1) + " " + sentence(3);
            String description = sentence(20) + ".";
            String location = randomCoords();
            IssueStatus status = IssueStatus.OPEN;
            // random transitions
            double p = random.nextDouble();
            if (p < 0.35) status = IssueStatus.IN_PROGRESS;
            if (p < 0.15) status = IssueStatus.RESOLVED;
            Issue issue = Issue.builder()
                    .title(title)
                    .description(description)
                    .location(location)
                    .status(status)
                    .category(category)
                    .createdBy(USERS[userIdx])
                    .createdByName(USER_NAMES[userIdx])
                    .createdAt(createdAt)
                    .updatedAt(Date.from(Instant.ofEpochMilli(createdAt)))
                    .shareToken(UUID.randomUUID().toString())
                    .build();
            batch.add(issue);
            if (batch.size() == 100) {
                issueRepository.saveAll(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) issueRepository.saveAll(batch);
        log.info("IssueSeeder: seeding complete. Total now={}", issueRepository.count());
    }

    private String sentence(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(' ');
            sb.append(WORDS[random.nextInt(WORDS.length)]);
        }
        return sb.toString();
    }

    private String randomCoords() {
        double lat = 8 + random.nextDouble() * 20; // 8-28
        double lon = 68 + random.nextDouble() * 20; // 68-88
        return String.format(Locale.ROOT, "%.6f,%.6f", lat, lon);
    }
}
