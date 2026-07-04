package com.financeflow.gamification.service;

import com.financeflow.accounts.repository.AccountRepository;
import com.financeflow.budgets.domain.Budget;
import com.financeflow.budgets.repository.BudgetRepository;
import com.financeflow.gamification.domain.UserAchievement;
import com.financeflow.gamification.domain.UserStreak;
import com.financeflow.gamification.dto.GamificationSummaryResponse;
import com.financeflow.gamification.repository.UserAchievementRepository;
import com.financeflow.gamification.repository.UserStreakRepository;
import com.financeflow.goals.domain.Goal;
import com.financeflow.goals.repository.GoalRepository;
import com.financeflow.categories.repository.CategoryRepository;
import com.financeflow.transactions.repository.TransactionRepository;
import com.financeflow.users.domain.User;
import com.financeflow.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GamificationService {

    private final UserRepository userRepository;
    private final UserStreakRepository userStreakRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final BudgetRepository budgetRepository;
    private final GoalRepository goalRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public GamificationSummaryResponse getSummary() {
        UUID userId = getCurrentUserId();
        User user = userRepository.findById(userId).orElseThrow();

        int healthScore = calculateHealthScore(userId);
        int streak = calculateStreak(userId);
        checkAndGrantAchievements(userId, streak);

        List<UserAchievement> allAchievements = userAchievementRepository.findAllByUserId(userId);
        List<GamificationSummaryResponse.AchievementDto> achievements = allAchievements.stream()
            .map(this::toAchievementDto)
            .collect(Collectors.toList());

        List<GamificationSummaryResponse.AchievementDto> recent = allAchievements.stream()
            .limit(5)
            .map(this::toAchievementDto)
            .collect(Collectors.toList());

        return GamificationSummaryResponse.builder()
            .healthScore(healthScore)
            .currentStreak(streak)
            .achievements(achievements)
            .recentAchievements(recent)
            .build();
    }

    @Transactional
    public void updateStreakAfterTransaction(UUID userId) {
        LocalDate today = LocalDate.now();
        UserStreak streak = userStreakRepository.findByUserId(userId).orElse(null);

        User user = userRepository.findById(userId).orElseThrow();

        if (streak == null) {
            streak = UserStreak.builder()
                .user(user)
                .lastActivityDate(today)
                .currentStreak(1)
                .build();
        } else {
            LocalDate last = streak.getLastActivityDate();
            if (last.equals(today)) {
                return; // já registrou hoje
            }
            if (last.plusDays(1).equals(today)) {
                streak.setCurrentStreak(streak.getCurrentStreak() + 1);
                streak.setLastActivityDate(today);
            } else {
                streak.setCurrentStreak(1);
                streak.setLastActivityDate(today);
            }
        }
        userStreakRepository.save(streak);
    }

    private int calculateHealthScore(UUID userId) {
        int budgetScore = 0, liquidityScore = 0, goalsScore = 0, consistencyScore = 0;

        // 1. Orçamentos (0-25): % dentro do limite
        YearMonth now = YearMonth.now();
        List<Budget> budgets = budgetRepository.findAllByUserIdAndMonthRange(
            userId, now.atDay(1), now.atEndOfMonth());
        if (!budgets.isEmpty()) {
            int ok = 0;
            for (Budget b : budgets) {
                if (b.getDeletedAt() != null) continue;
                BigDecimal spent = transactionRepository.sumExpensesByCategoryAndDateRange(
                    userId, b.getCategory().getId(), now.atDay(1), now.atEndOfMonth());
                if (spent == null) spent = BigDecimal.ZERO;
                if (spent.compareTo(b.getLimitAmount()) <= 0) ok++;
            }
            budgetScore = (int) (25.0 * ok / budgets.size());
        } else {
            budgetScore = 25; // sem orçamento = não penaliza
        }

        // 2. Liquidez (0-25): saldo positivo
        BigDecimal totalBalance = accountRepository.findAllByUserId(userId).stream()
            .filter(a -> a.getDeletedAt() == null)
            .map(a -> a.getBalance() != null ? a.getBalance() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        liquidityScore = totalBalance.compareTo(BigDecimal.ZERO) >= 0 ? 25 : 0;

        // 3. Metas (0-25): progresso médio
        List<Goal> goals = goalRepository.findAllByUserId(userId).stream()
            .filter(g -> g.getStatus() == Goal.GoalStatus.ACTIVE)
            .toList();
        if (goals.isEmpty()) {
            goalsScore = 25;
        } else {
            BigDecimal avg = goals.stream()
                .map(g -> g.getTargetAmount().compareTo(BigDecimal.ZERO) > 0
                    ? g.getCurrentAmount().multiply(BigDecimal.valueOf(100)).divide(g.getTargetAmount(), 2, RoundingMode.HALF_UP)
                    : BigDecimal.valueOf(100))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(goals.size()), 2, RoundingMode.HALF_UP);
            goalsScore = Math.min(25, avg.intValue() / 4);
        }

        // 4. Consistência (0-25): streak
        int streak = calculateStreak(userId);
        consistencyScore = Math.min(25, streak * 4); // 6+ dias = 25

        return Math.min(100, budgetScore + liquidityScore + goalsScore + consistencyScore);
    }

    private int calculateStreak(UUID userId) {
        // Primeiro tentar dados da tabela
        Optional<UserStreak> opt = userStreakRepository.findByUserId(userId);
        if (opt.isPresent()) {
            UserStreak s = opt.get();
            LocalDate today = LocalDate.now();
            if (s.getLastActivityDate().equals(today) || s.getLastActivityDate().equals(today.minusDays(1))) {
                return s.getCurrentStreak();
            }
        }

        // Fallback: calcular a partir das transações
        LocalDate since = LocalDate.now().minusDays(60);
        List<LocalDate> dates = transactionRepository.findDistinctDatesSince(userId, since);
        if (dates.isEmpty()) return 0;

        LocalDate today = LocalDate.now();
        // Se não tem today, começar de ontem
        LocalDate start = dates.contains(today) ? today : today.minusDays(1);
        if (!dates.contains(start)) return 0;

        int count = 0;
        LocalDate d = start;
        while (dates.contains(d)) {
            count++;
            d = d.minusDays(1);
        }
        return count;
    }

    private void checkAndGrantAchievements(UUID userId, int streak) {
        // FIRST_TRANSACTION
        long txCount = transactionRepository.findAllByUserIdForReport(userId).size();
        if (txCount >= 1) grantIfNotExists(userId, UserAchievement.AchievementCode.FIRST_TRANSACTION);

        // BUDGET_ONTIME
        YearMonth now = YearMonth.now();
        List<Budget> budgets = budgetRepository.findAllByUserIdAndMonthRange(userId, now.atDay(1), now.atEndOfMonth());
        if (!budgets.isEmpty()) {
            boolean allOk = true;
            for (Budget b : budgets) {
                if (b.getDeletedAt() != null) continue;
                BigDecimal spent = transactionRepository.sumExpensesByCategoryAndDateRange(
                    userId, b.getCategory().getId(), now.atDay(1), now.atEndOfMonth());
                if (spent == null) spent = BigDecimal.ZERO;
                if (spent.compareTo(b.getLimitAmount()) > 0) allOk = false;
            }
            if (allOk) grantIfNotExists(userId, UserAchievement.AchievementCode.BUDGET_ONTIME);
        }

        // GOAL_ACHIEVED - verificar se tem alguma meta completa
        boolean hasCompleted = goalRepository.findAllByUserId(userId).stream()
            .anyMatch(g -> g.getStatus() == Goal.GoalStatus.COMPLETED);
        if (hasCompleted) grantIfNotExists(userId, UserAchievement.AchievementCode.GOAL_ACHIEVED);

        // STREAK_7, STREAK_30
        if (streak >= 7) grantIfNotExists(userId, UserAchievement.AchievementCode.STREAK_7);
        if (streak >= 30) grantIfNotExists(userId, UserAchievement.AchievementCode.STREAK_30);

        // ACCOUNT_CREATED
        long accountCount = accountRepository.findAllByUserId(userId).stream()
            .filter(a -> a.getDeletedAt() == null).count();
        if (accountCount >= 1) grantIfNotExists(userId, UserAchievement.AchievementCode.ACCOUNT_CREATED);

        // CATEGORY_CREATED
        long categoryCount = categoryRepository.findAllByUserId(userId).size();
        if (categoryCount >= 1) grantIfNotExists(userId, UserAchievement.AchievementCode.CATEGORY_CREATED);
    }

    @Transactional
    protected void grantIfNotExists(UUID userId, UserAchievement.AchievementCode code) {
        if (!userAchievementRepository.existsByUserIdAndAchievementCode(userId, code.name())) {
            User user = userRepository.findById(userId).orElseThrow();
            UserAchievement ua = UserAchievement.builder()
                .user(user)
                .achievementCode(code.name())
                .build();
            userAchievementRepository.save(ua);
        }
    }

    private GamificationSummaryResponse.AchievementDto toAchievementDto(UserAchievement ua) {
        UserAchievement.AchievementCode code = UserAchievement.AchievementCode.valueOf(ua.getAchievementCode());
        return GamificationSummaryResponse.AchievementDto.builder()
            .code(ua.getAchievementCode())
            .title(code.getTitle())
            .description(code.getDescription())
            .unlockedAt(ua.getUnlockedAt())
            .build();
    }

    private UUID getCurrentUserId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UUID)) {
            throw new RuntimeException("Usuário não autenticado");
        }
        return (UUID) auth.getPrincipal();
    }
}
