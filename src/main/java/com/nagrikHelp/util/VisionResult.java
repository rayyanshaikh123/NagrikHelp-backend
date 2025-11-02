package com.nagrikHelp.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VisionResult {
    private boolean issue;
    private String label;
    private double confidence;
    private String reasoning;
    public boolean isIssue() { return issue; }
}
