package com.nagrikHelp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "issue_comments")
public class Comment {
    @Id
    private String id;
    @Indexed
    private String issueId;
    private String userId; // email or user id
    private String userName; // snapshot of name
    private String text;
    @Indexed
    private long createdAt;
    private long updatedAt;
}
