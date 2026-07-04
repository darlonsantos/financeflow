package com.financeflow.gamification.domain;

import com.financeflow.users.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_achievements", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "achievement_code"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "achievement_code", nullable = false, length = 50)
    private String achievementCode;

    @Column(name = "unlocked_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime unlockedAt;

    public enum AchievementCode {
        FIRST_TRANSACTION("Primeira Transação", "Registrou a primeira transação"),
        BUDGET_ONTIME("Orçamento em Dia", "Manteve todos os orçamentos dentro do limite no mês"),
        GOAL_ACHIEVED("Meta Alcançada", "Concluiu uma meta financeira"),
        STREAK_7("Semana Consistente", "7 dias seguidos registrando transações"),
        STREAK_30("Mês Consistente", "30 dias seguidos registrando transações"),
        ACCOUNT_CREATED("Conta Criada", "Criou a primeira conta"),
        CATEGORY_CREATED("Organizado", "Criou categorias para organizar gastos");

        private final String title;
        private final String description;

        AchievementCode(String title, String description) {
            this.title = title;
            this.description = description;
        }

        public String getTitle() { return title; }
        public String getDescription() { return description; }
    }
}
