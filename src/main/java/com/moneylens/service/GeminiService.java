package com.moneylens.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneylens.exception.AIServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Core service for interacting with Google Gemini API (free tier).
 * Handles prompt construction, API calls, and response parsing.
 */
@Service
@Slf4j
public class GeminiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String baseUrl;

    @Value("${gemini.api.model:gemini-2.0-flash}")
    private String model;

    public GeminiService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Send a prompt to Gemini and get a text response.
     */
    public String generate(String prompt) {
        try {
            String url = String.format("%s/%s:generateContent?key=%s", baseUrl, model, apiKey);

            Map<String, Object> requestBody = Map.of(
                    "contents", new Object[]{
                            Map.of("parts", new Object[]{
                                    Map.of("text", prompt)
                            })
                    },
                    "generationConfig", Map.of(
                            "temperature", 0.3,
                            "maxOutputTokens", 2048
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, String.class);

            return extractTextFromResponse(response.getBody());

        } catch (AIServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage(), e);
            throw new AIServiceException("Failed to communicate with AI service: " + e.getMessage(), e);
        }
    }

    /**
     * Send a prompt expecting JSON response.
     */
    public String generateJson(String prompt) {
        String enhancedPrompt = prompt + "\n\nIMPORTANT: Return ONLY valid JSON, no markdown formatting, no code blocks.";
        return generate(enhancedPrompt);
    }

    /**
     * Extract text content from Gemini API response.
     */
    private String extractTextFromResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.get("candidates");

            if (candidates == null || candidates.isEmpty()) {
                throw new AIServiceException("No candidates in Gemini response");
            }

            JsonNode content = candidates.get(0).get("content");
            if (content == null) {
                throw new AIServiceException("No content in Gemini response");
            }

            JsonNode parts = content.get("parts");
            if (parts == null || parts.isEmpty()) {
                throw new AIServiceException("No parts in Gemini response");
            }

            StringBuilder text = new StringBuilder();
            for (JsonNode part : parts) {
                if (part.has("text")) {
                    text.append(part.get("text").asText());
                }
            }

            String result = text.toString().trim();
            if (result.isEmpty()) {
                throw new AIServiceException("Empty text in Gemini response");
            }

            return result;

        } catch (AIServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", e.getMessage());
            throw new AIServiceException("Failed to parse AI response", e);
        }
    }
}
