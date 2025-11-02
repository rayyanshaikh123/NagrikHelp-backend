package com.nagrikHelp.dto;

import com.nagrikHelp.model.IssueCategory;
import lombok.Data;

@Data
public class CitizenUpdateIssueRequest {
    private String title;
    private String description;
    private String location;
    private IssueCategory category;
    private String imageBase64; // optional replacement image
}
