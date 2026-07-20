package com.moneylens.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "goals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(name = "target_amount", nullable = false)
    private Double targetAmount;

    @Column(name = "current_amount")
    @Builder.Default
    private Double currentAmount = 0.0;

    @Column(name = "deadline")
    private LocalDate deadline;

    @Column(name = "ai_feasibility_note")
    private String aiFeasibilityNote;

    @Column(name = "monthly_required_saving")
    private Double monthlyRequiredSaving;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private GoalStatus status = GoalStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum GoalStatus {
        ACTIVE, COMPLETED, ABANDONED
    }
}
