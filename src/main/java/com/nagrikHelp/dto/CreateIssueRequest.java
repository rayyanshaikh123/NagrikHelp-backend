package com.nagrikHelp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateIssueRequest {
    @NotBlank
    private String title;
    @NotBlank
    private String description;
    @NotBlank
    private String location;
    private String photoUrl;
}
