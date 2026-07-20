package com.moneylens.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalDTO {

    private Long id;
    private String name;
    private Double targetAmount;
    private Double currentAmount;
    private LocalDate deadline;
    private Double progressPercentage;
    private Double monthlyRequiredSaving;
    private String aiFeasibilityNote;
    private String status;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateGoalRequest {
        private String name;
        private Double targetAmount;
        private LocalDate deadline;
    }
}
