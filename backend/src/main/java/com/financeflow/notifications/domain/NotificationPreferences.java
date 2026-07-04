package com.financeflow.notifications.domain;

import com.financeflow.users.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences", indexes = {
    @Index(name = "idx_notification_preferences_user_id", columnList = "user_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "budget_exceeded_enabled", nullable = false)
    @Builder.Default
    private Boolean budgetExceededEnabled = true;

    @Column(name = "low_balance_enabled", nullable = false)
    @Builder.Default
    private Boolean lowBalanceEnabled = true;

    @Column(name = "bills_due_enabled", nullable = false)
    @Builder.Default
    private Boolean billsDueEnabled = true;

    @Column(name = "goal_due_enabled", nullable = false)
    @Builder.Default
    private Boolean goalDueEnabled = true;

    @Column(name = "low_balance_threshold", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal lowBalanceThreshold = new BigDecimal("100.00");

    @Column(name = "email_enabled", nullable = false)
    @Builder.Default
    private Boolean emailEnabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
