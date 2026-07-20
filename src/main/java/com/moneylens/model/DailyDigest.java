package com.moneylens.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_digests", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "digest_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyDigest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "digest_date", nullable = false)
    private LocalDate digestDate;

    @Column(name = "total_spent", nullable = false)
    private Double totalSpent;

    @Column(name = "total_earned")
    private Double totalEarned;

    @Column(name = "transaction_count")
    private Integer transactionCount;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "alerts", columnDefinition = "TEXT")
    private String alerts;

    @Column(name = "savings_rate")
    private Double savingsRate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
