package com.nagrikHelp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IssueVoteSummaryDto {
    private String issueId;
    private long upVotes;
    private long downVotes;
    private String userVote; // "UP", "DOWN" or null
}
