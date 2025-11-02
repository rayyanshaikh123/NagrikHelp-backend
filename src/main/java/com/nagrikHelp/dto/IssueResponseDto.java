package com.nagrikHelp.dto;

import com.nagrikHelp.model.Issue;
import com.nagrikHelp.model.IssueCategory;
import com.nagrikHelp.model.IssueStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IssueResponseDto {
    private String id;
    private String title;
    private String description;
    private IssueCategory category;
    private IssueStatus status;
    private String location;
    private Date createdAt;
    private CreatedBy createdBy;
    private long upVotes;
    private long downVotes;
    private String userVote; // "UP", "DOWN" or null
    private long commentsCount;
    private List<CommentResponseDto> recentComments;
    // added media fields so FE can show images
    private String photoUrl;
    private String imageBase64;
    private String shareToken; // added

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatedBy {
        private String id;
        private String name;
    }

    public static IssueResponseDto from(Issue i) {
        IssueResponseDto dto = new IssueResponseDto();
        dto.setId(i.getId());
        dto.setTitle(i.getTitle());
        dto.setDescription(i.getDescription());
        dto.setCategory(i.getCategory());
        dto.setStatus(i.getStatus());
        dto.setLocation(i.getLocation());
        dto.setCreatedAt(new Date(i.getCreatedAt()));
        dto.setCreatedBy(new CreatedBy(i.getCreatedById(), i.getCreatedByName()));
        dto.setUpVotes(0);
        dto.setDownVotes(0);
        dto.setUserVote(null);
        dto.setCommentsCount(0);
        dto.setRecentComments(Collections.emptyList());
        // populate new media fields
        dto.setPhotoUrl(i.getPhotoUrl());
        dto.setImageBase64(i.getImageBase64());
        dto.setShareToken(i.getShareToken());
        return dto;
    }

    public static IssueResponseDto from(Issue i, long up, long down, String userVote) {
        IssueResponseDto dto = from(i);
        dto.setUpVotes(up);
        dto.setDownVotes(down);
        dto.setUserVote(userVote);
        return dto;
    }

    public IssueResponseDto withComments(long count, List<CommentResponseDto> recent) {
        this.setCommentsCount(count);
        this.setRecentComments(recent);
        return this;
    }
}
