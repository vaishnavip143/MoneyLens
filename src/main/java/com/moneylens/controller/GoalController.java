package com.moneylens.controller;

import com.moneylens.dto.GoalDTO;
import com.moneylens.service.GoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goals")
@Tag(name = "Goals", description = "Savings goals with AI feasibility analysis")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping("/{userId}")
    @Operation(summary = "Create a savings goal with AI feasibility check")
    public ResponseEntity<GoalDTO> createGoal(
            @PathVariable Long userId,
            @RequestBody GoalDTO.CreateGoalRequest request) {
        return ResponseEntity.ok(goalService.createGoal(userId, request));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get all goals for a user")
    public ResponseEntity<List<GoalDTO>> getGoals(@PathVariable Long userId) {
        return ResponseEntity.ok(goalService.getUserGoals(userId));
    }

    @PutMapping("/{goalId}/progress")
    @Operation(summary = "Update goal progress")
    public ResponseEntity<GoalDTO> updateProgress(
            @PathVariable Long goalId,
            @RequestParam Double amount) {
        return ResponseEntity.ok(goalService.updateGoalProgress(goalId, amount));
    }
}
