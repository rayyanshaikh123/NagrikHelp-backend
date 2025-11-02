package com.nagrikHelp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "notifications")
public class UserNotification {
    @Id
    private String id;

    @Indexed
    private String userEmail;

    private String issueId; // optional context

    private String type; // ISSUE_STATUS, ISSUE_VOTE, etc.

    private String message;

    private long createdAt;

    private boolean read;
}
