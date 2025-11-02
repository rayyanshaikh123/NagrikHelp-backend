package com.nagrikHelp.dto;

import lombok.Data;

@Data
public class AIValidationDto {
    private boolean valid;
    private String suggestedCategory;
    private double confidence;
    private String message;
    private String provider;
}
