package com.moneylens.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictionReport {

    private Double predictedTotal;
    private Map<String, Double> predictedByCategory;
    private List<SpendingTrend> trends;
    private List<AnomalyAlert> alerts;
    private String aiInsight;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SpendingTrend {
        private String category;
        private String trend;       // "increasing", "decreasing", "stable"
        private Double percentChange;
        private String aiNarrative;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnomalyAlert {
        private String description;
        private String severity;    // "HIGH", "MEDIUM", "LOW"
        private Double amount;
        private String expectedRange;
        private String suggestion;
    }
}
