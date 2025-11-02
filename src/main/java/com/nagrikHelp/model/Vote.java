package com.nagrikHelp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "issue_votes")
@CompoundIndexes({
        @CompoundIndex(name = "uniq_issue_user", def = "{issueId:1,userId:1}", unique = true)
})
public class Vote {
    @Id
    private String id;
    private String issueId;
    private String userId; // email or user id
    private VoteValue value; // UP / DOWN
    private long createdAt;
    private long updatedAt;
}
