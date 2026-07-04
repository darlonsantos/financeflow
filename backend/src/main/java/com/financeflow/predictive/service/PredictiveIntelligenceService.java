package com.financeflow.predictive.service;

import com.financeflow.accounts.repository.AccountRepository;
import com.financeflow.budgets.domain.Budget;
import com.financeflow.budgets.repository.BudgetRepository;
import com.financeflow.goals.domain.Goal;
import com.financeflow.goals.repository.GoalRepository;
import com.financeflow.installments.domain.InstallmentItem;
import com.financeflow.installments.repository.InstallmentItemRepository;
import com.financeflow.predictive.dto.PredictiveAlertDto;
import com.financeflow.predictive.dto.PredictiveReportResponse;
import com.financeflow.projections.dto.BalanceProjectionResponse;
import com.financeflow.projections.service.BalanceProjectionService;
import com.financeflow.transactions.domain.Transaction;
import com.financeflow.transactions.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Módulo de inteligência financeira preditiva.
 * Analisa histórico, metas, orçamentos e recorrências para prever riscos e gerar alertas preventivos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PredictiveIntelligenceService {

    private static final int HISTORICAL_MONTHS = 6;
    private static final int PROJECTION_MONTHS = 3;
    private static final BigDecimal BUDGET_RISK_THRESHOLD = new BigDecimal("0.80");   // 80% do limite
    private static final BigDecimal INSTALLMENT_BURDEN_THRESHOLD = new BigDecimal("0.40"); // 40% da renda
    private static final BigDecimal RECURRING_COMMITMENT_THRESHOLD = new BigDecimal("0.50"); // 50% da renda
    private static final int GOAL_RISK_DAYS = 90;

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final GoalRepository goalRepository;
    private final InstallmentItemRepository installmentItemRepository;
    private final BalanceProjectionService balanceProjectionService;

    @Transactional(readOnly = true)
    public PredictiveReportResponse generateReport() {
        UUID userId = getCurrentUserId();
        List<PredictiveAlertDto> alerts = new ArrayList<>();

        YearMonth now = YearMonth.now();
        LocalDate startHistory = now.minusMonths(HISTORICAL_MONTHS).atDay(1);
        LocalDate endHistory = now.atEndOfMonth();

        // Médias históricas
        BigDecimal totalIncome = transactionRepository.sumIncomeByDateRange(userId, startHistory, endHistory);
        BigDecimal totalExpense = transactionRepository.sumExpensesByDateRange(userId, startHistory, endHistory);
        if (totalIncome == null) totalIncome = BigDecimal.ZERO;
        if (totalExpense == null) totalExpense = BigDecimal.ZERO;
        BigDecimal avgMonthlyIncome = totalIncome.divide(BigDecimal.valueOf(HISTORICAL_MONTHS), 2, RoundingMode.HALF_UP);
        BigDecimal avgMonthlyExpense = totalExpense.divide(BigDecimal.valueOf(HISTORICAL_MONTHS), 2, RoundingMode.HALF_UP);

        // Saldo atual
        BigDecimal currentBalance = accountRepository.findAllByUserId(userId).stream()
            .filter(a -> a.getDeletedAt() == null)
            .map(a -> a.getBalance() != null ? a.getBalance() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 1. Risco de orçamento (categoria próxima ou acima do limite)
        addBudgetRiskAlerts(userId, now, alerts);

        // 2. Saldo negativo projetado
        addNegativeBalanceAlerts(userId, currentBalance, alerts);

        // 3. Metas em risco (prazo próximo e progresso insuficiente)
        addGoalRiskAlerts(userId, alerts);

        // 4. Parcelamentos altos em relação à renda
        addInstallmentBurdenAlerts(userId, avgMonthlyIncome, alerts);

        // 5. Comprometimento recorrente alto
        addRecurringCommitmentAlerts(userId, avgMonthlyIncome, alerts);

        // Resumo e cenário
        String summary = buildSummary(alerts, currentBalance, avgMonthlyIncome, avgMonthlyExpense);
        String scenario = buildScenarioText(userId, avgMonthlyIncome, avgMonthlyExpense);

        return PredictiveReportResponse.builder()
            .generatedAt(java.time.Instant.now())
            .summary(summary)
            .scenarioNextMonths(scenario)
            .alerts(alerts)
            .historicalMonths(HISTORICAL_MONTHS)
            .projectionMonths(PROJECTION_MONTHS)
            .build();
    }

    private void addBudgetRiskAlerts(UUID userId, YearMonth now, List<PredictiveAlertDto> alerts) {
        LocalDate monthStart = now.atDay(1);
        LocalDate monthEnd = now.atEndOfMonth();
        List<Budget> budgets = budgetRepository.findAllByUserIdAndMonthRange(userId, monthStart, monthEnd);

        for (Budget b : budgets) {
            if (b.getDeletedAt() != null) continue;
            BigDecimal spent = transactionRepository.sumExpensesByCategoryAndDateRange(
                userId, b.getCategory().getId(), monthStart, monthEnd);
            if (spent == null) spent = BigDecimal.ZERO;
            if (b.getLimitAmount().compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal pct = spent.divide(b.getLimitAmount(), 4, RoundingMode.HALF_UP);
            if (pct.compareTo(BigDecimal.ONE) >= 0) {
                alerts.add(PredictiveAlertDto.builder()
                    .riskType("BUDGET_EXCEEDED")
                    .severity("HIGH")
                    .title("Orçamento excedido")
                    .message(String.format("A categoria '%s' já excedeu o limite deste mês (R$ %s / R$ %s).",
                        b.getCategory().getName(), formatAmount(spent), formatAmount(b.getLimitAmount())))
                    .suggestion("Revise gastos nesta categoria ou ajuste o limite do orçamento.")
                    .entityType("Budget")
                    .entityId(b.getId())
                    .build());
            } else if (pct.compareTo(BUDGET_RISK_THRESHOLD) >= 0) {
                alerts.add(PredictiveAlertDto.builder()
                    .riskType("BUDGET_AT_RISK")
                    .severity("MEDIUM")
                    .title("Orçamento próximo do limite")
                    .message(String.format("A categoria '%s' está em %.0f%% do limite (R$ %s / R$ %s).",
                        b.getCategory().getName(), pct.multiply(BigDecimal.valueOf(100)).doubleValue(),
                        formatAmount(spent), formatAmount(b.getLimitAmount())))
                    .suggestion("Evite novos gastos nesta categoria até o fim do mês ou ajuste o orçamento.")
                    .entityType("Budget")
                    .entityId(b.getId())
                    .build());
            }
        }
    }

    private void addNegativeBalanceAlerts(UUID userId, BigDecimal currentBalance, List<PredictiveAlertDto> alerts) {
        BalanceProjectionResponse projection = balanceProjectionService.projectBalance(PROJECTION_MONTHS);
        if (projection.getProjections() == null) return;
        for (BalanceProjectionResponse.MonthProjection month : projection.getProjections()) {
            if (month.getBalance().compareTo(BigDecimal.ZERO) < 0) {
                alerts.add(PredictiveAlertDto.builder()
                    .riskType("NEGATIVE_BALANCE_PROJECTED")
                    .severity("HIGH")
                    .title("Saldo negativo projetado")
                    .message(String.format("A projeção indica saldo negativo em %s (R$ %s).",
                        month.getMonthLabel(), formatAmount(month.getBalance())))
                    .suggestion("Reduza despesas ou aumente receitas para evitar saldo negativo.")
                    .entityType(null)
                    .entityId(null)
                    .build());
                break; // um alerta basta
            }
        }
    }

    private void addGoalRiskAlerts(UUID userId, List<PredictiveAlertDto> alerts) {
        LocalDate now = LocalDate.now();
        LocalDate limitDate = now.plusDays(GOAL_RISK_DAYS);
        List<Goal> goals = goalRepository.findAllByUserId(userId).stream()
            .filter(g -> g.getStatus() == Goal.GoalStatus.ACTIVE && g.getDueDate() != null
                && !g.getDueDate().isBefore(now) && !g.getDueDate().isAfter(limitDate))
            .toList();

        for (Goal g : goals) {
            if (g.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) continue;
            LocalDate created = g.getCreatedAt().toLocalDate();
            long totalDays = ChronoUnit.DAYS.between(created, g.getDueDate());
            if (totalDays <= 0) continue;
            long daysSinceCreation = ChronoUnit.DAYS.between(created, now);
            // Progresso esperado: proporção do tempo já decorrido em relação ao prazo total
            double expectedProgress = Math.min(1.0, Math.max(0.0, (double) daysSinceCreation / totalDays));
            BigDecimal expectedAmount = g.getTargetAmount().multiply(BigDecimal.valueOf(expectedProgress)).setScale(2, RoundingMode.HALF_UP);
            if (g.getCurrentAmount().compareTo(expectedAmount) < 0) {
                long daysLeft = ChronoUnit.DAYS.between(now, g.getDueDate());
                alerts.add(PredictiveAlertDto.builder()
                    .riskType("GOAL_AT_RISK")
                    .severity("MEDIUM")
                    .title("Meta em risco")
                    .message(String.format("A meta '%s' vence em %d dias. Valor atual: R$ %s / Meta: R$ %s.",
                        g.getName(), daysLeft, formatAmount(g.getCurrentAmount()), formatAmount(g.getTargetAmount())))
                    .suggestion("Aumente as contribuições ou estenda o prazo da meta para alcançar o valor desejado.")
                    .entityType("Goal")
                    .entityId(g.getId())
                    .build());
            }
        }
    }

    private void addInstallmentBurdenAlerts(UUID userId, BigDecimal avgMonthlyIncome, List<PredictiveAlertDto> alerts) {
        if (avgMonthlyIncome.compareTo(BigDecimal.ZERO) <= 0) return;
        LocalDate from = LocalDate.now();
        LocalDate to = YearMonth.now().plusMonths(PROJECTION_MONTHS).atEndOfMonth();
        List<InstallmentItem> pending = installmentItemRepository.findPendingByUserIdAndDueDateBetween(userId, from, to);
        BigDecimal totalInstallments = pending.stream()
            .map(InstallmentItem::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgInstallmentsPerMonth = totalInstallments.divide(BigDecimal.valueOf(PROJECTION_MONTHS), 2, RoundingMode.HALF_UP);
        BigDecimal burdenRatio = avgInstallmentsPerMonth.divide(avgMonthlyIncome, 4, RoundingMode.HALF_UP);
        if (burdenRatio.compareTo(INSTALLMENT_BURDEN_THRESHOLD) >= 0) {
            alerts.add(PredictiveAlertDto.builder()
                .riskType("HIGH_INSTALLMENT_BURDEN")
                .severity("MEDIUM")
                .title("Parcelamentos altos")
                .message(String.format("Nos próximos %d meses você tem em média R$ %s em parcelas (%.0f%% da sua renda média).",
                    PROJECTION_MONTHS, formatAmount(avgInstallmentsPerMonth), burdenRatio.multiply(BigDecimal.valueOf(100)).doubleValue()))
                .suggestion("Evite novas compras parceladas e priorize quitar parcelas com juros mais altos.")
                .entityType(null)
                .entityId(null)
                .build());
        }
    }

    private void addRecurringCommitmentAlerts(UUID userId, BigDecimal avgMonthlyIncome, List<PredictiveAlertDto> alerts) {
        if (avgMonthlyIncome.compareTo(BigDecimal.ZERO) <= 0) return;
        List<Transaction> recurring = transactionRepository.findAllRecurringByUserId(userId);
        BigDecimal monthlyRecurring = BigDecimal.ZERO;
        for (Transaction t : recurring) {
            if (t.getType() == Transaction.TransactionType.EXPENSE) {
                BigDecimal impact = getMonthlyImpact(t.getAmount(), t.getRecurringPattern());
                monthlyRecurring = monthlyRecurring.add(impact);
            }
        }
        BigDecimal ratio = monthlyRecurring.divide(avgMonthlyIncome, 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(RECURRING_COMMITMENT_THRESHOLD) >= 0) {
            alerts.add(PredictiveAlertDto.builder()
                .riskType("HIGH_RECURRING_COMMITMENT")
                .severity("MEDIUM")
                .title("Comprometimento recorrente alto")
                .message(String.format("Suas despesas recorrentes (R$ %s/mês) representam %.0f%% da sua renda média.",
                    formatAmount(monthlyRecurring), ratio.multiply(BigDecimal.valueOf(100)).doubleValue()))
                .suggestion("Revise assinaturas e gastos fixos para liberar margem para imprevistos e metas.")
                .entityType(null)
                .entityId(null)
                .build());
        }
    }

    private static BigDecimal getMonthlyImpact(BigDecimal amount, String pattern) {
        if (amount == null) return BigDecimal.ZERO;
        if (pattern == null) return amount;
        return switch (pattern.toUpperCase()) {
            case "DAILY" -> amount.multiply(BigDecimal.valueOf(30));
            case "WEEKLY" -> amount.multiply(BigDecimal.valueOf(4.33)).setScale(2, RoundingMode.HALF_UP);
            case "MONTHLY" -> amount;
            case "YEARLY" -> amount.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
            default -> amount;
        };
    }

    private String buildSummary(List<PredictiveAlertDto> alerts, BigDecimal balance, BigDecimal avgIncome, BigDecimal avgExpense) {
        if (alerts.isEmpty()) {
            return String.format("Sua situação financeira está estável. Saldo atual R$ %s. Média mensal: receitas R$ %s, despesas R$ %s.",
                formatAmount(balance), formatAmount(avgIncome), formatAmount(avgExpense));
        }
        long high = alerts.stream().filter(a -> "HIGH".equals(a.getSeverity())).count();
        if (high > 0) {
            return String.format("Identificamos %d alerta(s) de alta prioridade e %d no total. Recomendamos atenção aos itens destacados.",
                high, alerts.size());
        }
        return String.format("Identificamos %d ponto(s) de atenção. Revise as sugestões para manter sua saúde financeira.", alerts.size());
    }

    private String buildScenarioText(UUID userId, BigDecimal avgIncome, BigDecimal avgExpense) {
        BalanceProjectionResponse proj = balanceProjectionService.projectBalance(PROJECTION_MONTHS);
        if (proj.getProjections() == null || proj.getProjections().isEmpty()) {
            return String.format("Próximos %d meses: projeção baseada em média de receitas R$ %s e despesas R$ %s ao mês.",
                PROJECTION_MONTHS, formatAmount(avgIncome), formatAmount(avgExpense));
        }
        BalanceProjectionResponse.MonthProjection last = proj.getProjections().get(proj.getProjections().size() - 1);
        return String.format("Nos próximos %d meses, considerando sua média histórica e parcelamentos, o saldo projetado em %s é R$ %s.",
            PROJECTION_MONTHS, last.getMonthLabel(), formatAmount(last.getBalance()));
    }

    private static String formatAmount(BigDecimal amount) {
        return NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR")).format(amount != null ? amount : BigDecimal.ZERO);
    }

    private UUID getCurrentUserId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UUID)) {
            throw new RuntimeException("Usuário não autenticado");
        }
        return (UUID) auth.getPrincipal();
    }
}
