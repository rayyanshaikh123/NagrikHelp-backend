package com.nagrikHelp.dto;

import com.nagrikHelp.model.Comment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponseDto {
    private String id;
    private String issueId;
    private String userId;
    private String userName;
    private String text;
    private long createdAt;

    public static CommentResponseDto from(Comment c) {
        return new CommentResponseDto(
                c.getId(),
                c.getIssueId(),
                c.getUserId(),
                c.getUserName(),
                c.getText(),
                c.getCreatedAt()
        );
    }
}
