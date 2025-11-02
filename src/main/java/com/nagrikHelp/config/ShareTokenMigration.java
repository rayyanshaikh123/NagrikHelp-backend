package com.nagrikHelp.config;

import com.nagrikHelp.model.Issue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShareTokenMigration {

    private final MongoTemplate mongoTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void backfillShareTokens() {
        try {
            Query q = new Query(new Criteria().orOperator(
                    Criteria.where("shareToken").is(null),
                    Criteria.where("shareToken").is("")
            ));
            List<Issue> missing = mongoTemplate.find(q, Issue.class);
            if (!missing.isEmpty()) {
                log.info("ShareToken migration: backfilling {} issues without token", missing.size());
                Set<String> used = new HashSet<>();
                // collect existing non-null tokens to avoid rare collision
                mongoTemplate.findAll(Issue.class).forEach(i -> { if (i.getShareToken() != null) used.add(i.getShareToken()); });
                for (Issue issue : missing) {
                    String token;
                    do { token = UUID.randomUUID().toString(); } while (used.contains(token));
                    used.add(token);
                    issue.setShareToken(token);
                    mongoTemplate.save(issue);
                }
                log.info("ShareToken migration: completed");
            } else {
                log.info("ShareToken migration: no issues needed backfill");
            }
            // Ensure sparse unique index (so null/absent still ignored in future if any)
            try {
                mongoTemplate.indexOps(Issue.class)
                        .ensureIndex(new Index().on("shareToken", Sort.Direction.ASC).unique().sparse());
                log.info("ShareToken migration: ensured sparse unique index on shareToken");
            } catch (Exception ix) {
                log.warn("ShareToken migration: index creation failed: {}", ix.getMessage());
            }
        } catch (Exception e) {
            log.warn("ShareToken migration encountered an error: {}", e.getMessage());
        }
    }
}
