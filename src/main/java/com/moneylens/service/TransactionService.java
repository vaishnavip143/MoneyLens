package com.moneylens.service;

import com.moneylens.dto.CategorizedTransaction;
import com.moneylens.dto.TransactionUploadResponse;
import com.moneylens.exception.ResourceNotFoundException;
import com.moneylens.model.*;
import com.moneylens.repository.TransactionRepository;
import com.moneylens.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final AICategorizationService categorizationService;

    public TransactionService(TransactionRepository transactionRepository,
                              UserRepository userRepository,
                              AICategorizationService categorizationService) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.categorizationService = categorizationService;
    }

    /**
     * Upload a CSV file of transactions and categorize them with AI.
     */
    @Transactional
    public TransactionUploadResponse uploadTransactions(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        List<String[]> rawRows = parseCsv(file);
        List<String> descriptions = rawRows.stream()
                .map(row -> row.length > 1 ? row[1] : row[0])
                .collect(Collectors.toList());

        // AI categorization in one batch call
        List<CategorizedTransaction> categorized = categorizationService.categorizeBatch(descriptions);

        List<Transaction> savedTransactions = new ArrayList<>();
        List<TransactionUploadResponse.AnomalyAlert> anomalies = new ArrayList<>();

        for (int i = 0; i < rawRows.size(); i++) {
            String[] row = rawRows.get(i);
            CategorizedTransaction cat = categorized.get(i);

            Transaction tx = buildTransaction(user, row, cat);
            savedTransactions.add(transactionRepository.save(tx));
        }

        // Detect anomalies after all are saved
        anomalies = detectUploadAnomalies(userId, savedTransactions);

        // Build response
        Map<Category, Integer> breakdown = savedTransactions.stream()
                .collect(Collectors.groupingBy(Transaction::getCategory, Collectors.summingInt(t -> 1)));

        Double totalSpent = savedTransactions.stream()
                .filter(t -> t.getType() == TransactionType.DEBIT)
                .mapToDouble(Transaction::getAmount)
                .sum();

        Double totalEarned = savedTransactions.stream()
                .filter(t -> t.getType() == TransactionType.CREDIT)
                .mapToDouble(Transaction::getAmount)
                .sum();

        return TransactionUploadResponse.builder()
                .totalTransactions(rawRows.size())
                .successCount(savedTransactions.size())
                .failedCount(0)
                .categoryBreakdown(breakdown)
                .anomalies(anomalies)
                .totalSpent(totalSpent)
                .totalEarned(totalEarned)
                .build();
    }

    /**
     * Parse CSV file into rows.
     * Expected format: date, description, amount, type(CREDIT/DEBIT)
     */
    private List<String[]> parseCsv(MultipartFile file) {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean headerSkipped = false;
            while ((line = reader.readLine()) != null) {
                if (!headerSkipped && (line.toLowerCase().contains("date") || line.toLowerCase().contains("description"))) {
                    headerSkipped = true;
                    continue;
                }
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                // Trim each part
                for (int i = 0; i < parts.length; i++) {
                    parts[i] = parts[i].trim().replace("\"", "");
                }
                rows.add(parts);
            }
        } catch (Exception e) {
            log.error("Failed to parse CSV: {}", e.getMessage());
            throw new RuntimeException("Failed to parse CSV file: " + e.getMessage(), e);
        }
        return rows;
    }

    private Transaction buildTransaction(User user, String[] row, CategorizedTransaction cat) {
        // CSV format: date, description, amount, type
        LocalDate date = parseDate(row.length > 0 ? row[0] : "2024-01-01");
        String description = row.length > 1 ? row[1] : "Unknown";
        Double amount = row.length > 2 ? Double.parseDouble(row[2].replace(",", "")) : 0.0;
        TransactionType type = row.length > 3 && row[3].toUpperCase().contains("CREDIT")
                ? TransactionType.CREDIT : TransactionType.DEBIT;

        Category category = mapStringToCategory(cat.getCategory());

        return Transaction.builder()
                .user(user)
                .rawDescription(description)
                .cleanDescription(cat.getDescription())
                .amount(amount)
                .type(type)
                .category(category)
                .aiCategory(cat.getCategory())
                .aiConfidence(cat.getConfidence())
                .isRecurring(cat.getIsRecurring())
                .merchant(cat.getMerchant())
                .transactionDate(date)
                .build();
    }

    private LocalDate parseDate(String dateStr) {
        try {
            // Try multiple formats
            if (dateStr.contains("/")) {
                String[] parts = dateStr.split("/");
                if (parts.length == 3) {
                    return LocalDate.of(Integer.parseInt(parts[2]), Integer.parseInt(parts[1]), Integer.parseInt(parts[0]));
                }
            }
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            log.warn("Failed to parse date: {}, using today", dateStr);
            return LocalDate.now();
        }
    }

    private Category mapStringToCategory(String categoryStr) {
        try {
            return Category.valueOf(categoryStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Category.OTHER;
        }
    }

    private List<TransactionUploadResponse.AnomalyAlert> detectUploadAnomalies(Long userId, List<Transaction> transactions) {
        List<TransactionUploadResponse.AnomalyAlert> anomalies = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();

        // Get historical average daily spend
        Double avgDailySpend = transactionRepository.avgDailySpend(userId, monthStart.minusMonths(3));
        if (avgDailySpend == null) avgDailySpend = 0.0;

        for (Transaction tx : transactions) {
            if (tx.getType() == TransactionType.CREDIT) continue;

            // Flag transactions > 3x average daily spend
            if (avgDailySpend > 0 && tx.getAmount() > avgDailySpend * 3) {
                anomalies.add(TransactionUploadResponse.AnomalyAlert.builder()
                        .description(tx.getRawDescription())
                        .amount(tx.getAmount())
                        .reason("Amount is " + String.format("%.1f", tx.getAmount() / avgDailySpend) + "x your average daily spend")
                        .build());
            }
        }
        return anomalies;
    }

    public List<Transaction> getUserTransactions(Long userId) {
        return transactionRepository.findByUserIdOrderByTransactionDateDesc(userId);
    }

    public Map<Category, Double> getCategoryBreakdown(Long userId, LocalDate start, LocalDate end) {
        List<Object[]> results = transactionRepository.sumByCategoryInRange(userId, start, end);
        Map<Category, Double> breakdown = new LinkedHashMap<>();
        for (Object[] row : results) {
            Category cat = Category.valueOf((String) row[0]);
            Double sum = (Double) row[1];
            breakdown.put(cat, sum);
        }
        return breakdown;
    }
}
