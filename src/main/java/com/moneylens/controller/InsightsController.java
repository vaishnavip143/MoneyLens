package com.moneylens.controller;

import com.moneylens.dto.DailyDigestDTO;
import com.moneylens.dto.PredictionReport;
import com.moneylens.service.DigestService;
import com.moneylens.service.PredictionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/insights")
@Tag(name = "Insights", description = "AI-powered spending predictions and anomalies")
public class InsightsController {

    private final PredictionService predictionService;
    private final DigestService digestService;

    public InsightsController(PredictionService predictionService, DigestService digestService) {
        this.predictionService = predictionService;
        this.digestService = digestService;
    }

    @GetMapping("/predictions/{userId}")
    @Operation(summary = "Get AI-powered spending predictions for next month")
    public ResponseEntity<PredictionReport> getPredictions(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "3") int months) {

        PredictionReport report = predictionService.predict(userId, months);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/anomalies/{userId}")
    @Operation(summary = "Detect spending anomalies using AI")
    public ResponseEntity<PredictionReport> getAnomalies(@PathVariable Long userId) {
        PredictionReport report = predictionService.predict(userId, 3);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/digest/{userId}")
    @Operation(summary = "Get AI-generated daily spending digest")
    public ResponseEntity<DailyDigestDTO> getDigest(@PathVariable Long userId) {
        DailyDigestDTO digest = digestService.getTodayDigest(userId);
        return ResponseEntity.ok(digest);
    }

    @GetMapping("/digest/{userId}/{date}")
    @Operation(summary = "Get AI digest for a specific date")
    public ResponseEntity<DailyDigestDTO> getDigestForDate(
            @PathVariable Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        DailyDigestDTO digest = digestService.getDigest(userId, date);
        return ResponseEntity.ok(digest);
    }
}
