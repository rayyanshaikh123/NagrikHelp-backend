package com.nagrikHelp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateStatusRequest {
    @NotBlank
    private String status; // expects IN_PROGRESS or RESOLVED
}