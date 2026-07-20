package com.moneylens.controller;

import com.moneylens.dto.*;
import com.moneylens.repository.TransactionRepository;
import com.moneylens.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Business intelligence dashboard data")
public class DashboardController {

    private final TransactionService transactionService;
    private final PredictionService predictionService;
    private final DigestService digestService;
    private final TransactionRepository transactionRepository;

    public DashboardController(TransactionService transactionService,
                               PredictionService predictionService,
                               DigestService digestService,
                               TransactionRepository transactionRepository) {
        this.transactionService = transactionService;
        this.predictionService = predictionService;
        this.digestService = digestService;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get full dashboard data with AI insights")
    public ResponseEntity<DashboardDTO> getDashboard(@PathVariable Long userId) {
        YearMonth currentMonth = YearMonth.now();
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();

        // Monthly spend and income
        Double monthlySpend = transactionRepository.sumDebitsInRange(userId, monthStart, monthEnd);
        Double monthlyIncome = transactionRepository.sumCreditsInRange(userId, monthStart, monthEnd);

        // Category breakdown
        Map<String, Double> categoryBreakdown = transactionService
                .getCategoryBreakdown(userId, monthStart, monthEnd)
                .entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().getEmoji() + " " + e.getKey().getDisplayName(),
                        e -> Math.round(e.getValue() * 100.0) / 100.0,
                        (a, b) -> a,
                        LinkedHashMap::new));

        // Daily trend
        List<Object[]> dailyData = transactionRepository.dailySpendTrend(userId, monthStart, monthEnd);
        List<DashboardDTO.DailySpend> dailyTrend = dailyData.stream()
                .map(row -> DashboardDTO.DailySpend.builder()
                        .date(String.valueOf(row[0]))
                        .amount((Double) row[1])
                        .build())
                .collect(Collectors.toList());

        // Top merchants
        List<Object[]> merchantData = transactionRepository.topMerchantsInRange(userId, monthStart, monthEnd);
        List<DashboardDTO.TopMerchant> topMerchants = merchantData.stream()
                .limit(5)
                .map(row -> DashboardDTO.TopMerchant.builder()
                        .name((String) row[0])
                        .totalAmount((Double) row[1])
                        .transactionCount(((Long) row[2]).intValue())
                        .build())
                .collect(Collectors.toList());

        // Predictions
        PredictionReport predictions = predictionService.predict(userId, 3);

        // Today's digest
        DailyDigestDTO todayDigest = digestService.getTodayDigest(userId);

        // Budget
        Double savingsRate = (monthlyIncome != null && monthlyIncome > 0 && monthlySpend != null)
                ? ((monthlyIncome - monthlySpend) / monthlyIncome) * 100 : 0.0;

        return ResponseEntity.ok(DashboardDTO.builder()
                .monthlySpend(monthlySpend != null ? monthlySpend : 0.0)
                .monthlyIncome(monthlyIncome != null ? monthlyIncome : 0.0)
                .savingsRate(Math.round(savingsRate * 10.0) / 10.0)
                .budgetRemaining(monthlyIncome != null ? monthlyIncome - (monthlySpend != null ? monthlySpend : 0) : 0.0)
                .categoryBreakdown(categoryBreakdown)
                .dailyTrend(dailyTrend)
                .topMerchants(topMerchants)
                .predictionReport(predictions)
                .todayDigest(todayDigest)
                .build());
    }
}
