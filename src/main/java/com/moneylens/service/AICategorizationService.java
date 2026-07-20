package com.moneylens.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneylens.dto.CategorizedTransaction;
import com.moneylens.exception.AIServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Uses Gemini AI to categorize raw bank transaction descriptions
 * into meaningful spending categories with confidence scores.
 */
@Service
@Slf4j
public class AICategorizationService {

    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    // Categories used in prompts — must match our Category enum
    private static final String CATEGORIES = """
            FOOD, TRANSPORT, SALARY, SUBSCRIPTION, RENT, INVESTMENT,
            ENTERTAINMENT, UTILITIES, HEALTHCARE, SHOPPING, EDUCATION, OTHER
            """;

    public AICategorizationService(GeminiService geminiService, ObjectMapper objectMapper) {
        this.geminiService = geminiService;
        this.objectMapper = objectMapper;
    }

    /**
     * Categorize a batch of transaction descriptions using AI.
     * Sends them all in one prompt to minimize API calls.
     */
    public List<CategorizedTransaction> categorizeBatch(List<String> descriptions) {
        if (descriptions == null || descriptions.isEmpty()) {
            return List.of();
        }

        String prompt = buildCategorizationPrompt(descriptions);

        try {
            String response = geminiService.generateJson(prompt);
            return parseCategorizationResponse(response, descriptions);
        } catch (AIServiceException e) {
            log.warn("AI categorization failed, using fallback: {}", e.getMessage());
            return fallbackCategorize(descriptions);
        }
    }

    /**
     * Categorize a single transaction description.
     */
    public CategorizedTransaction categorizeSingle(String description) {
        List<CategorizedTransaction> result = categorizeBatch(List.of(description));
        return result.isEmpty() ? fallbackSingle(description) : result.get(0);
    }

    private String buildCategorizationPrompt(List<String> descriptions) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a financial transaction analyzer. Categorize these bank transaction descriptions.\n\n");
        prompt.append("Available categories: ").append(CATEGORIES).append("\n\n");
        prompt.append("For each transaction, return:\n");
        prompt.append("- description: the original description\n");
        prompt.append("- category: one of the categories above\n");
        prompt.append("- confidence: 0.0 to 1.0\n");
        prompt.append("- isRecurring: true if it looks like a subscription/recurring payment\n");
        prompt.append("- merchant: extracted merchant name if identifiable\n\n");
        prompt.append("Return a JSON array. Example:\n");
        prompt.append("""
                [
                  {"description":"SWIGGY BANGALORE","category":"FOOD","confidence":0.95,"isRecurring":false,"merchant":"Swiggy"},
                  {"description":"NETFLIX SUBSCRIPTION","category":"SUBSCRIPTION","confidence":0.99,"isRecurring":true,"merchant":"Netflix"}
                ]

                Transactions to categorize:
                """);

        for (int i = 0; i < descriptions.size(); i++) {
            prompt.append(i + 1).append(". ").append(descriptions.get(i)).append("\n");
        }

        return prompt.toString();
    }

    private List<CategorizedTransaction> parseCategorizationResponse(String response, List<String> originals) {
        try {
            // Clean the response — remove markdown code blocks if present
            String cleaned = response.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();

            List<Map<String, Object>> parsed = objectMapper.readValue(
                    cleaned, new TypeReference<>() {});

            List<CategorizedTransaction> results = new ArrayList<>();

            for (Map<String, Object> item : parsed) {
                CategorizedTransaction tx = CategorizedTransaction.builder()
                        .description((String) item.getOrDefault("description", ""))
                        .category((String) item.getOrDefault("category", "OTHER"))
                        .confidence(item.get("confidence") != null ? ((Number) item.get("confidence")).doubleValue() : 0.5)
                        .isRecurring(item.get("isRecurring") != null ? (Boolean) item.get("isRecurring") : false)
                        .merchant((String) item.getOrDefault("merchant", ""))
                        .build();
                results.add(tx);
            }

            // If AI returned fewer results than input, fill gaps with fallback
            while (results.size() < originals.size()) {
                int idx = results.size();
                results.add(fallbackSingle(originals.get(idx)));
            }

            return results;

        } catch (Exception e) {
            log.warn("Failed to parse AI categorization response: {}", e.getMessage());
            return fallbackCategorize(originals);
        }
    }

    /**
     * Simple keyword-based fallback when AI is unavailable.
     */
    private List<CategorizedTransaction> fallbackCategorize(List<String> descriptions) {
        List<CategorizedTransaction> results = new ArrayList<>();
        for (String desc : descriptions) {
            results.add(fallbackSingle(desc));
        }
        return results;
    }

    private CategorizedTransaction fallbackSingle(String description) {
        String upper = description.toUpperCase();
        String category = "OTHER";
        boolean isRecurring = false;

        if (upper.contains("SWIGGY") || upper.contains("ZOMATO") || upper.contains("FOOD")
                || upper.contains("RESTAURANT") || upper.contains("CAFE")) {
            category = "FOOD";
        } else if (upper.contains("UBER") || upper.contains("OLA") || upper.contains("PETROL")
                || upper.contains("DIESEL") || upper.contains("METRO")) {
            category = "TRANSPORT";
        } else if (upper.contains("SALARY") || upper.contains("WAGE") || upper.contains("CREDIT")
                && upper.contains("TRANSFER")) {
            category = "SALARY";
        } else if (upper.contains("NETFLIX") || upper.contains("SPOTIFY") || upper.contains("SUBSCRIPTION")
                || upper.contains("PRIME")) {
            category = "SUBSCRIPTION";
            isRecurring = true;
        } else if (upper.contains("RENT") || upper.contains("LEASE")) {
            category = "RENT";
        } else if (upper.contains("ELECTRIC") || upper.contains("WATER") || upper.contains("GAS")
                || upper.contains("BROADBAND") || upper.contains("JIO") || upper.contains("AIRTEL")) {
            category = "UTILITIES";
        } else if (upper.contains("HOSPITAL") || upper.contains("PHARMACY") || upper.contains("MEDICAL")) {
            category = "HEALTHCARE";
        } else if (upper.contains("AMAZON") || upper.contains("FLIPKART") || upper.contains("MEESHO")) {
            category = "SHOPPING";
        }

        return CategorizedTransaction.builder()
                .description(description)
                .category(category)
                .confidence(0.4)
                .isRecurring(isRecurring)
                .merchant(extractMerchant(description))
                .build();
    }

    private String extractMerchant(String description) {
        // Take first 2-3 words as merchant name
        String[] words = description.split("\\s+");
        StringBuilder merchant = new StringBuilder();
        for (int i = 0; i < Math.min(3, words.length); i++) {
            if (i > 0) merchant.append(" ");
            merchant.append(words[i]);
        }
        return merchant.toString();
    }
}
