package com.moneylens.controller;

import com.moneylens.dto.TransactionUploadResponse;
import com.moneylens.model.Transaction;
import com.moneylens.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transactions", description = "Upload, view, and manage transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload CSV of transactions — AI categorizes them automatically")
    public ResponseEntity<TransactionUploadResponse> uploadTransactions(
            @RequestParam Long userId,
            @RequestParam("file") MultipartFile file) {

        TransactionUploadResponse response = transactionService.uploadTransactions(userId, file);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get all transactions for a user")
    public ResponseEntity<List<Transaction>> getTransactions(@PathVariable Long userId) {
        return ResponseEntity.ok(transactionService.getUserTransactions(userId));
    }

    @GetMapping("/{userId}/category-breakdown")
    @Operation(summary = "Get spending breakdown by category")
    public ResponseEntity<Map<String, Double>> getCategoryBreakdown(
            @PathVariable Long userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        return ResponseEntity.ok(
                transactionService.getCategoryBreakdown(userId,
                        java.time.LocalDate.parse(startDate),
                        java.time.LocalDate.parse(endDate))
                        .entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                e -> e.getKey().getDisplayName(),
                                Map.Entry::getValue)));
    }
}
