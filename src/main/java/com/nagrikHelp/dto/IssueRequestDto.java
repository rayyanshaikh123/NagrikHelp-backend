package com.nagrikHelp.dto;

import com.nagrikHelp.model.IssueCategory;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class IssueRequestDto {
    @NotBlank
    private String title;
    @NotBlank
    private String description;
    private IssueCategory category;
    private String imageBase64;
    private String location;
}
