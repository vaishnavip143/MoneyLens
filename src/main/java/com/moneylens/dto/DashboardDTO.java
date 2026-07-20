package com.moneylens.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardDTO {

    private Double monthlySpend;
    private Double monthlyIncome;
    private Double savingsRate;
    private Double budgetRemaining;
    private Map<String, Double> categoryBreakdown;
    private List<DailySpend> dailyTrend;
    private List<TopMerchant> topMerchants;
    private PredictionReport predictionReport;
    private DailyDigestDTO todayDigest;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailySpend {
        private String date;
        private Double amount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopMerchant {
        private String name;
        private Double totalAmount;
        private Integer transactionCount;
    }
}
