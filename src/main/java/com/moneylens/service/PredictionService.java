package com.moneylens.service;

import com.moneylens.dto.PredictionReport;
import com.moneylens.model.Category;
import com.moneylens.model.TransactionType;
import com.moneylens.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * Predicts future spending and detects anomalies using AI + statistical analysis.
 * Combines Gemini AI insights with local data calculations.
 */
@Service
@Slf4j
public class PredictionService {

    private final TransactionRepository transactionRepository;
    private final GeminiService geminiService;

    public PredictionService(TransactionRepository transactionRepository, GeminiService geminiService) {
        this.transactionRepository = transactionRepository;
        this.geminiService = geminiService;
    }

    /**
     * Generate a spending prediction report for the next month.
     */
    public PredictionReport predict(Long userId, int monthsOfHistory) {
        LocalDate now = LocalDate.now();
        LocalDate historyStart = now.minusMonths(monthsOfHistory);
        YearMonth nextMonth = YearMonth.now().plusMonths(1);

        // 1. Get monthly spending history
        Map<YearMonth, Double> monthlyTotals = calculateMonthlyTotals(userId, historyStart, now);

        // 2. Get category-wise monthly breakdown
        Map<Category, List<Double>> categoryHistory = calculateCategoryHistory(userId, historyStart, now);

        // 3. Simple linear prediction
        Double predictedTotal = predictNextValue(new ArrayList<>(monthlyTotals.values()));

        // 4. Predict by category
        Map<String, Double> predictedByCategory = new LinkedHashMap<>();
        for (Map.Entry<Category, List<Double>> entry : categoryHistory.entrySet()) {
            Double predicted = predictNextValue(entry.getValue());
            predictedByCategory.put(entry.getKey().getDisplayName(), Math.round(predicted * 100.0) / 100.0);
        }

        // 5. Detect trends
        List<PredictionReport.SpendingTrend> trends = analyzeTrends(categoryHistory);

        // 6. Detect anomalies
        List<PredictionReport.AnomalyAlert> alerts = detectAnomalies(userId, now.minusMonths(1));

        // 7. Get AI insight
        String aiInsight = generateAiInsight(userId, predictedTotal, predictedByCategory, trends);

        return PredictionReport.builder()
                .predictedTotal(Math.round(predictedTotal * 100.0) / 100.0)
                .predictedByCategory(predictedByCategory)
                .trends(trends)
                .alerts(alerts)
                .aiInsight(aiInsight)
                .build();
    }

    /**
     * Detect anomalies in recent transactions.
     */
    public List<PredictionReport.AnomalyAlert> detectAnomalies(Long userId, LocalDate referenceDate) {
        List<PredictionReport.AnomalyAlert> anomalies = new ArrayList<>();
        YearMonth refMonth = YearMonth.from(referenceDate);

        // Get current month totals by category
        LocalDate monthStart = refMonth.atDay(1);
        LocalDate monthEnd = refMonth.atEndOfMonth();

        Map<Category, Double> currentMonthSpend = getMonthlyCategorySpend(userId, monthStart, monthEnd);

        // Get average of previous 3 months
        Map<Category, Double> avgSpend = getAverageCategorySpend(userId, refMonth.minusMonths(3), refMonth.minusMonths(1));

        for (Map.Entry<Category, Double> entry : currentMonthSpend.entrySet()) {
            Category cat = entry.getKey();
            Double current = entry.getValue();
            Double avg = avgSpend.getOrDefault(cat, 0.0);

            if (avg > 0) {
                double percentChange = ((current - avg) / avg) * 100;

                if (percentChange > 50) {
                    anomalies.add(PredictionReport.AnomalyAlert.builder()
                            .description(cat.getEmoji() + " " + cat.getDisplayName() + " spending spiked")
                            .severity(percentChange > 100 ? "HIGH" : "MEDIUM")
                            .amount(current)
                            .expectedRange(String.format("%.0f - %.0f", avg * 0.8, avg * 1.2))
                            .suggestion(buildAnomalySuggestion(cat, percentChange))
                            .build());
                } else if (percentChange < -50) {
                    anomalies.add(PredictionReport.AnomalyAlert.builder()
                            .description(cat.getEmoji() + " " + cat.getDisplayName() + " spending dropped significantly")
                            .severity("LOW")
                            .amount(current)
                            .expectedRange(String.format("%.0f - %.0f", avg * 0.8, avg * 1.2))
                            .suggestion("Good job! Keep this trend going.")
                            .build());
                }
            }
        }

        return anomalies;
    }

    private Map<YearMonth, Double> calculateMonthlyTotals(Long userId, LocalDate start, LocalDate end) {
        Map<YearMonth, Double> totals = new LinkedHashMap<>();
        YearMonth current = YearMonth.from(start);

        while (!current.isAfter(YearMonth.from(end))) {
            Double sum = transactionRepository.sumDebitsInRange(userId,
                    current.atDay(1), current.atEndOfMonth());
            totals.put(current, sum != null ? sum : 0.0);
            current = current.plusMonths(1);
        }

        return totals;
    }

    private Map<Category, List<Double>> calculateCategoryHistory(Long userId, LocalDate start, LocalDate end) {
        Map<Category, List<Double>> history = new LinkedHashMap<>();
        YearMonth current = YearMonth.from(start);

        while (!current.isAfter(YearMonth.from(end))) {
            List<Object[]> results = transactionRepository.sumByCategoryInRange(userId,
                    current.atDay(1), current.atEndOfMonth());

            Set<Category> seen = new HashSet<>();
            for (Object[] row : results) {
                Category cat = Category.valueOf((String) row[0]);
                Double sum = (Double) row[1];
                history.computeIfAbsent(cat, k -> new ArrayList<>()).add(sum);
                seen.add(cat);
            }

            // Add 0 for categories not present this month
            for (Category cat : Category.values()) {
                if (!seen.contains(cat) && !cat.equals(Category.SALARY)) {
                    history.computeIfAbsent(cat, k -> new ArrayList<>()).add(0.0);
                }
            }

            current = current.plusMonths(1);
        }

        return history;
    }

    /**
     * Simple linear regression prediction.
     */
    private Double predictNextValue(List<Double> historicalValues) {
        if (historicalValues.isEmpty()) return 0.0;
        if (historicalValues.size() == 1) return historicalValues.get(0);

        int n = historicalValues.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += historicalValues.get(i);
            sumXY += i * historicalValues.get(i);
            sumX2 += i * i;
        }

        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;

        double predicted = slope * n + intercept;

        // Clamp: prediction should be at least 0 and not more than 2x max historical
        double maxHistorical = historicalValues.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        return Math.max(0, Math.min(predicted, maxHistorical * 2));
    }

    private List<PredictionReport.SpendingTrend> analyzeTrends(Map<Category, List<Double>> categoryHistory) {
        List<PredictionReport.SpendingTrend> trends = new ArrayList<>();

        for (Map.Entry<Category, List<Double>> entry : categoryHistory.entrySet()) {
            List<Double> values = entry.getValue();
            if (values.size() < 2) continue;

            Double recent = values.get(values.size() - 1);
            Double previous = values.get(values.size() - 2);

            if (previous == 0 && recent == 0) continue;

            double percentChange = previous > 0 ? ((recent - previous) / previous) * 100 : 100;
            String trendDirection = percentChange > 5 ? "increasing" : percentChange < -5 ? "decreasing" : "stable";

            trends.add(PredictionReport.SpendingTrend.builder()
                    .category(entry.getKey().getEmoji() + " " + entry.getKey().getDisplayName())
                    .trend(trendDirection)
                    .percentChange(Math.round(percentChange * 10.0) / 10.0)
                    .aiNarrative(trendDirection.equals("increasing")
                            ? "Watch this category — spending is going up"
                            : trendDirection.equals("decreasing")
                                    ? "Nice! Spending here is going down"
                                    : "Staying steady")
                    .build());
        }

        return trends;
    }

    private String generateAiInsight(Long userId, Double predicted, Map<String, Double> byCategory,
                                     List<PredictionReport.SpendingTrend> trends) {
        try {
            String trendSummary = trends.stream()
                    .map(t -> t.getCategory() + ": " + t.getTrend() + " (" + t.getPercentChange() + "%)")
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("Not enough data");

            String prompt = String.format("""
                    You are a personal finance advisor. Analyze this spending data and give 3-4 concise, actionable insights.

                    Predicted next month total: %.0f
                    Category predictions:
                    %s

                    Trend analysis:
                    %s

                    Write insights in a friendly, motivating tone. Be specific with numbers.
                    Keep it under 100 words.""",
                    predicted, byCategory, trendSummary);

            return geminiService.generate(prompt);
        } catch (Exception e) {
            log.warn("Failed to generate AI insight, using fallback");
            return String.format("Predicted spending next month: %.0f. " +
                    "Review your top categories to find savings opportunities.", predicted);
        }
    }

    private String buildAnomalySuggestion(Category category, double percentChange) {
        return switch (category) {
            case FOOD -> "Food delivery spending is unusually high. Try cooking more this week?";
            case SUBSCRIPTION -> "Check if you've signed up for new subscriptions recently.";
            case SHOPPING -> "Consider implementing a 24-hour rule before non-essential purchases.";
            case ENTERTAINMENT -> "Entertainment spending is up. Look for free alternatives?";
            case TRANSPORT -> "Transport costs spiked. Consider carpooling or public transport?";
            default -> String.format("Spending is %.0f%% above your usual. Review recent transactions.", percentChange);
        };
    }

    private Map<Category, Double> getMonthlyCategorySpend(Long userId, LocalDate start, LocalDate end) {
        List<Object[]> results = transactionRepository.sumByCategoryInRange(userId, start, end);
        Map<Category, Double> spend = new HashMap<>();
        for (Object[] row : results) {
            spend.put(Category.valueOf((String) row[0]), (Double) row[1]);
        }
        return spend;
    }

    private Map<Category, Double> getAverageCategorySpend(Long userId, YearMonth start, YearMonth end) {
        Map<Category, Double> totals = new HashMap<>();
        Map<Category, Integer> counts = new HashMap<>();

        YearMonth current = start;
        while (!current.isAfter(end)) {
            List<Object[]> results = transactionRepository.sumByCategoryInRange(userId,
                    current.atDay(1), current.atEndOfMonth());
            for (Object[] row : results) {
                Category cat = Category.valueOf((String) row[0]);
                Double sum = (Double) row[1];
                totals.merge(cat, sum, Double::sum);
                counts.merge(cat, 1, Integer::sum);
            }
            current = current.plusMonths(1);
        }

        Map<Category, Double> avg = new HashMap<>();
        for (Map.Entry<Category, Double> entry : totals.entrySet()) {
            avg.put(entry.getKey(), entry.getValue() / counts.get(entry.getKey()));
        }
        return avg;
    }
}
