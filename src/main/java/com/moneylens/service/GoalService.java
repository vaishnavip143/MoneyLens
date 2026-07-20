package com.moneylens.service;

import com.moneylens.dto.GoalDTO;
import com.moneylens.exception.ResourceNotFoundException;
import com.moneylens.model.Goal;
import com.moneylens.model.User;
import com.moneylens.repository.GoalRepository;
import com.moneylens.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GoalService {

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;
    private final GeminiService geminiService;

    public GoalService(GoalRepository goalRepository, UserRepository userRepository, GeminiService geminiService) {
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
        this.geminiService = geminiService;
    }

    public GoalDTO createGoal(Long userId, GoalDTO.CreateGoalRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Goal goal = Goal.builder()
                .user(user)
                .name(request.getName())
                .targetAmount(request.getTargetAmount())
                .deadline(request.getDeadline())
                .build();

        // Calculate monthly required saving
        if (request.getDeadline() != null) {
            long months = ChronoUnit.MONTHS.between(LocalDate.now(), request.getDeadline());
            if (months > 0) {
                goal.setMonthlyRequiredSaving(request.getTargetAmount() / months);
            }
        }

        // Get AI feasibility check
        goal.setAiFeasibilityNote(generateFeasibilityNote(user, goal));

        goal = goalRepository.save(goal);
        return mapToDTO(goal);
    }

    public List<GoalDTO> getUserGoals(Long userId) {
        return goalRepository.findByUserId(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public GoalDTO updateGoalProgress(Long goalId, Double additionalAmount) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found"));

        goal.setCurrentAmount(goal.getCurrentAmount() + additionalAmount);

        if (goal.getCurrentAmount() >= goal.getTargetAmount()) {
            goal.setStatus(Goal.GoalStatus.COMPLETED);
        }

        goal = goalRepository.save(goal);
        return mapToDTO(goal);
    }

    private String generateFeasibilityNote(User user, Goal goal) {
        try {
            String prompt = String.format("""
                    As a financial advisor, evaluate this savings goal:

                    Goal: %s
                    Target: ₹%.0f
                    Current savings: ₹%.0f
                    Deadline: %s
                    Monthly budget: %s

                    Give a brief (2-3 lines) feasibility assessment:
                    - Is it realistic?
                    - What monthly saving is needed?
                    - One practical tip.""",
                    goal.getName(), goal.getTargetAmount(), goal.getCurrentAmount(),
                    goal.getDeadline() != null ? goal.getDeadline().toString() : "No deadline",
                    user.getMonthlyBudget() != null ? "₹" + user.getMonthlyBudget() : "Not set");

            return geminiService.generate(prompt);
        } catch (Exception e) {
            return "Keep saving consistently. You can do it!";
        }
    }

    private GoalDTO mapToDTO(Goal goal) {
        Double progress = goal.getTargetAmount() > 0
                ? (goal.getCurrentAmount() / goal.getTargetAmount()) * 100
                : 0.0;

        return GoalDTO.builder()
                .id(goal.getId())
                .name(goal.getName())
                .targetAmount(goal.getTargetAmount())
                .currentAmount(goal.getCurrentAmount())
                .deadline(goal.getDeadline())
                .progressPercentage(Math.round(progress * 10.0) / 10.0)
                .monthlyRequiredSaving(goal.getMonthlyRequiredSaving())
                .aiFeasibilityNote(goal.getAiFeasibilityNote())
                .status(goal.getStatus().name())
                .build();
    }
}
