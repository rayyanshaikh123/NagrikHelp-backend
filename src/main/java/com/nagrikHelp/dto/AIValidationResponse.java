package com.nagrikHelp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIValidationResponse {
    private boolean isValid;
    private String suggestedCategory;
    private double confidence;
    private String message;
    private String provider;
    // New fields for graceful errors
    private boolean error;
    private String errorCode; // e.g., AI_KEY_INVALID, AI_UNAVAILABLE, OCR_EMPTY
}
