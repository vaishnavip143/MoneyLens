package com.moneylens.service;

import com.moneylens.dto.DailyDigestDTO;
import com.moneylens.model.*;
import com.moneylens.repository.DailyDigestRepository;
import com.moneylens.repository.TransactionRepository;
import com.moneylens.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Generates daily AI-powered financial digests.
 * Runs on a schedule every morning at 8 AM.
 */
@Service
@Slf4j
public class DigestService {

    private final TransactionRepository transactionRepository;
    private final DailyDigestRepository digestRepository;
    private final UserRepository userRepository;
    private final GeminiService geminiService;

    public DigestService(TransactionRepository transactionRepository,
                         DailyDigestRepository digestRepository,
                         UserRepository userRepository,
                         GeminiService geminiService) {
        this.transactionRepository = transactionRepository;
        this.digestRepository = digestRepository;
        this.userRepository = userRepository;
        this.geminiService = geminiService;
    }

    /**
     * Scheduled task: Generate daily digests for all users at 8 AM.
     */
    @Scheduled(cron = "${moneylens.digest.cron:0 0 8 * * *}")
    public void generateMorningDigests() {
        log.info("Running daily digest generation...");
        LocalDate yesterday = LocalDate.now().minusDays(1);

        for (User user : userRepository.findAll()) {
            try {
                generateDigest(user.getId(), yesterday);
            } catch (Exception e) {
                log.error("Failed to generate digest for user {}: {}", user.getId(), e.getMessage());
            }
        }
        log.info("Daily digest generation complete.");
    }

    /**
     * Generate (or retrieve cached) digest for a specific user and date.
     */
    public DailyDigestDTO getDigest(Long userId, LocalDate date) {
        return generateDigest(userId, date);
    }

    /**
     * Get today's digest.
     */
    public DailyDigestDTO getTodayDigest(Long userId) {
        return generateDigest(userId, LocalDate.now());
    }

    private DailyDigestDTO generateDigest(Long userId, LocalDate date) {
        // Check if digest already exists
        return digestRepository.findByUserIdAndDigestDate(userId, date)
                .map(this::mapToDTO)
                .orElseGet(() -> createNewDigest(userId, date));
    }

    private DailyDigestDTO createNewDigest(Long userId, LocalDate date) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return null;

        List<Transaction> dayTransactions = transactionRepository.findByUserIdAndDate(userId, date);

        Double totalSpent = dayTransactions.stream()
                .filter(t -> t.getType() == TransactionType.DEBIT)
                .mapToDouble(Transaction::getAmount)
                .sum();

        Double totalEarned = dayTransactions.stream()
                .filter(t -> t.getType() == TransactionType.CREDIT)
                .mapToDouble(Transaction::getAmount)
                .sum();

        // Calculate savings rate for the month
        LocalDate monthStart = date.withDayOfMonth(1);
        Double monthSpent = transactionRepository.sumDebitsInRange(userId, monthStart, date);
        Double monthEarned = transactionRepository.sumCreditsInRange(userId, monthStart, date);
        Double savingsRate = (monthEarned != null && monthEarned > 0 && monthSpent != null)
                ? ((monthEarned - monthSpent) / monthEarned) * 100
                : 0.0;

        // Generate AI summary
        String aiSummary = generateAiSummary(user, dayTransactions, totalSpent, savingsRate);
        String alerts = generateAlerts(user, totalSpent, monthSpent);

        // Save to database
        DailyDigest digest = DailyDigest.builder()
                .user(user)
                .digestDate(date)
                .totalSpent(totalSpent)
                .totalEarned(totalEarned)
                .transactionCount(dayTransactions.size())
                .aiSummary(aiSummary)
                .alerts(alerts)
                .savingsRate(Math.round(savingsRate * 10.0) / 10.0)
                .build();

        digestRepository.save(digest);
        return mapToDTO(digest);
    }

    private String generateAiSummary(User user, List<Transaction> transactions, Double totalSpent, Double savingsRate) {
        try {
            String txSummary = transactions.isEmpty()
                    ? "No transactions today."
                    : transactions.stream()
                            .map(t -> String.format("- %s: ₹%.0f (%s)",
                                    t.getRawDescription(), t.getAmount(), t.getType()))
                            .reduce((a, b) -> a + "\n" + b)
                            .orElse("");

            String prompt = String.format("""
                    Write a friendly daily financial briefing for %s.

                    Today's transactions:
                    %s

                    Total spent today: ₹%.0f
                    Monthly savings rate: %.1f%%
                    Monthly budget: %s

                    Write a concise, motivating 3-4 line briefing.
                    Mention the spending amount, any notable patterns, and end with an encouraging note.
                    Use emojis sparingly.""",
                    user.getName(), txSummary, totalSpent, savingsRate,
                    user.getMonthlyBudget() != null ? "₹" + user.getMonthlyBudget() : "Not set");

            return geminiService.generate(prompt);

        } catch (Exception e) {
            log.warn("AI summary generation failed, using template");
            return String.format("Hey %s! You spent ₹%.0f today. %s Keep tracking your spending!",
                    user.getName(), totalSpent,
                    savingsRate > 20 ? "Your savings rate is looking great!" : "Try to keep expenses in check.");
        }
    }

    private String generateAlerts(User user, Double todaySpend, Double monthSpend) {
        StringBuilder alerts = new StringBuilder();

        // Alert if daily spend is high
        if (user.getMonthlyBudget() != null && user.getMonthlyBudget() > 0) {
            double dailyBudget = user.getMonthlyBudget() / 30;
            if (todaySpend > dailyBudget * 1.5) {
                alerts.append("⚠️ Today's spending (₹")
                        .append(String.format("%.0f", todaySpend))
                        .append(") is above your daily budget target (₹")
                        .append(String.format("%.0f", dailyBudget))
                        .append(")\n");
            }
        }

        // Alert if monthly spend is approaching budget
        if (user.getMonthlyBudget() != null && monthSpend != null) {
            double percentUsed = (monthSpend / user.getMonthlyBudget()) * 100;
            if (percentUsed > 90) {
                alerts.append("🔴 You've used ")
                        .append(String.format("%.0f", percentUsed))
                        .append("% of your monthly budget!\n");
            } else if (percentUsed > 75) {
                alerts.append("🟡 You've used ")
                        .append(String.format("%.0f", percentUsed))
                        .append("% of your monthly budget.\n");
            }
        }

        return alerts.toString();
    }

    private DailyDigestDTO mapToDTO(DailyDigest digest) {
        List<String> alertList = digest.getAlerts() != null && !digest.getAlerts().isEmpty()
                ? List.of(digest.getAlerts().split("\n"))
                : List.of();

        String formatted = String.format(
                "💰 %s\n📊 Spent: ₹%.0f | Earned: ₹%.0f\n💳 Transactions: %d\n📈 Savings Rate: %.1f%%\n\n%s",
                digest.getDigestDate(),
                digest.getTotalSpent(),
                digest.getTotalEarned() != null ? digest.getTotalEarned() : 0,
                digest.getTransactionCount(),
                digest.getSavingsRate() != null ? digest.getSavingsRate() : 0,
                digest.getAiSummary()
        );

        return DailyDigestDTO.builder()
                .id(digest.getId())
                .date(digest.getDigestDate())
                .totalSpent(digest.getTotalSpent())
                .totalEarned(digest.getTotalEarned())
                .transactionCount(digest.getTransactionCount())
                .aiSummary(digest.getAiSummary())
                .alerts(alertList)
                .savingsRate(digest.getSavingsRate())
                .formattedDigest(formatted)
                .build();
    }
}
