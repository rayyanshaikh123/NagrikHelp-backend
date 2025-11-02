package com.nagrikHelp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VoteRequestDto {
    @NotBlank
    private String value; // "UP" or "DOWN"
}
