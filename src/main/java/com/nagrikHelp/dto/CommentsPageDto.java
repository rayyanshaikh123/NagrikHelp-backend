package com.nagrikHelp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentsPageDto {
    private String issueId;
    private int page;
    private int size;
    private long total;
    private List<CommentResponseDto> items;
}
