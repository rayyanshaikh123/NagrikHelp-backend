package com.nagrikHelp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIValidation {
    private boolean valid;
    private String suggestedCategory; // enum name suggestion
    private double confidence;
    private String message;
    private String provider; // gemini, fallback
    private Date evaluatedAt;
    private String rawLabel; // original model label if different from mapped
}
