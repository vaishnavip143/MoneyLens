package com.moneylens.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyDigestDTO {

    private Long id;
    private LocalDate date;
    private Double totalSpent;
    private Double totalEarned;
    private Integer transactionCount;
    private String aiSummary;
    private List<String> alerts;
    private Double savingsRate;
    private String formattedDigest;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategorySpend {
        private String category;
        private Double amount;
        private Double percentage;
    }
}
