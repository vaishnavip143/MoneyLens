package com.moneylens.dto;

import com.moneylens.model.Category;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionUploadResponse {

    private Integer totalTransactions;
    private Integer successCount;
    private Integer failedCount;
    private Map<Category, Integer> categoryBreakdown;
    private List<AnomalyAlert> anomalies;
    private Double totalSpent;
    private Double totalEarned;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnomalyAlert {
        private String description;
        private Double amount;
        private String reason;
    }
}
