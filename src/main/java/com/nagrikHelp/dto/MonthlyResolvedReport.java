package com.nagrikHelp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyResolvedReport {
    private int year;
    private int month; // 1-12
    private long totalResolved;
    private List<DayCount> daily;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayCount {
        private String date; // ISO yyyy-MM-dd
        private long count;
    }
}