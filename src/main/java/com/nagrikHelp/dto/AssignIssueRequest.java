package com.nagrikHelp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssignIssueRequest {
    @NotBlank
    private String assignedTo; // staff or team identifier/name
}