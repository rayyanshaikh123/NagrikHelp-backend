package com.nagrikHelp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "issues")
public class Issue {
    @Id
    private String id;

    private String title;
    private String description;
    private String location;
    private String photoUrl;

    private IssueStatus status;

    // Phase 2 fields
    private IssueCategory category;
    private String imageBase64;

    @Indexed
    private String createdBy; // creator email (username)
    private String createdById;
    private String createdByName;

    // Phase 3: assignedTo (friendly staff/team id or name)
    private String assignedTo;

    // Timestamps
    private long createdAt; // keep epoch millis for existing FE
    private Date updatedAt; // now a Date per Phase 3 spec

    private String shareToken; // public share token (UUID)

    @Builder.Default
    private java.util.List<String> followerPhones = new java.util.ArrayList<>(); // phone numbers to notify
    @Builder.Default
    private java.util.List<String> followerEmails = new java.util.ArrayList<>();
    @Builder.Default
    private java.util.List<String> followerWebhookUrls = new java.util.ArrayList<>();
}
