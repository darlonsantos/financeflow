package com.financeflow.projections.service;

import com.financeflow.accounts.repository.AccountRepository;
import com.financeflow.installments.domain.InstallmentItem;
import com.financeflow.installments.repository.InstallmentItemRepository;
import com.financeflow.projections.dto.BalanceProjectionResponse;
import com.financeflow.transactions.domain.Transaction;
import com.financeflow.transactions.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Serviço de projeção de saldo futuro.
 * Considera transações recorrentes e padrões históricos (média de receitas/despesas dos últimos meses).
 * Não altera cálculos financeiros existentes.
 */
@Service
@RequiredArgsConstructor
public class BalanceProjectionService {

    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM/yyyy", Locale.forLanguageTag("pt-BR"));
    private static final int DEFAULT_HISTORICAL_MONTHS = 6;

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final InstallmentItemRepository installmentItemRepository;

    @Transactional(readOnly = true)
    public BalanceProjectionResponse projectBalance(int months) {
        UUID userId = getCurrentUserId();
        if (months < 1 || months > 24) {
            months = 12;
        }

        // Saldo atual (todas as contas)
        BigDecimal currentBalance = accountRepository.findAllByUserId(userId).stream()
            .filter(a -> a.getDeletedAt() == null)
            .map(a -> a.getBalance() != null ? a.getBalance() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        YearMonth now = YearMonth.now();

        // Impacto mensal das transações recorrentes
        BigDecimal monthlyRecurringIncome = BigDecimal.ZERO;
        BigDecimal monthlyRecurringExpense = BigDecimal.ZERO;

        List<Transaction> recurring = transactionRepository.findAllRecurringByUserId(userId);
        for (Transaction t : recurring) {
            BigDecimal monthlyImpact = getMonthlyImpact(t.getAmount(), t.getRecurringPattern());
            if (t.getType() == Transaction.TransactionType.INCOME) {
                monthlyRecurringIncome = monthlyRecurringIncome.add(monthlyImpact);
            } else {
                monthlyRecurringExpense = monthlyRecurringExpense.add(monthlyImpact);
            }
        }

        // Média histórica (últimos N meses) - inclui recorrentes e variáveis
        LocalDate historicalStart = now.minusMonths(DEFAULT_HISTORICAL_MONTHS).atDay(1);
        LocalDate historicalEnd = now.atEndOfMonth();

        BigDecimal avgIncome = transactionRepository.sumIncomeByDateRange(userId, historicalStart, historicalEnd);
        BigDecimal avgExpense = transactionRepository.sumExpensesByDateRange(userId, historicalStart, historicalEnd);

        if (avgIncome == null) avgIncome = BigDecimal.ZERO;
        if (avgExpense == null) avgExpense = BigDecimal.ZERO;

        BigDecimal avgMonthlyIncome = avgIncome.divide(BigDecimal.valueOf(DEFAULT_HISTORICAL_MONTHS), 2, RoundingMode.HALF_UP);
        BigDecimal avgMonthlyExpense = avgExpense.divide(BigDecimal.valueOf(DEFAULT_HISTORICAL_MONTHS), 2, RoundingMode.HALF_UP);

        // Projeção: usar média histórica se houver; senão usar só recorrentes
        BigDecimal projectedIncome;
        BigDecimal projectedExpense;
        if (avgIncome.compareTo(BigDecimal.ZERO) > 0 || avgExpense.compareTo(BigDecimal.ZERO) > 0) {
            projectedIncome = avgMonthlyIncome;
            projectedExpense = avgMonthlyExpense;
        } else {
            projectedIncome = monthlyRecurringIncome;
            projectedExpense = monthlyRecurringExpense;
        }

        // Gerar projeções mês a mês (incluindo parcelas pendentes em cada mês)
        List<BalanceProjectionResponse.MonthProjection> projections = new ArrayList<>();
        BigDecimal runningBalance = currentBalance;

        for (int i = 1; i <= months; i++) {
            YearMonth futureMonth = now.plusMonths(i);
            LocalDate monthStart = futureMonth.atDay(1);
            LocalDate monthEnd = futureMonth.atEndOfMonth();

            BigDecimal installmentExpense = BigDecimal.ZERO;
            List<InstallmentItem> pendingInMonth = installmentItemRepository.findPendingByUserIdAndDueDateBetween(userId, monthStart, monthEnd);
            for (InstallmentItem item : pendingInMonth) {
                installmentExpense = installmentExpense.add(item.getAmount());
            }

            BigDecimal totalExpenseForMonth = projectedExpense.add(installmentExpense);
            BigDecimal monthlyNet = projectedIncome.subtract(totalExpenseForMonth);
            runningBalance = runningBalance.add(monthlyNet);

            projections.add(BalanceProjectionResponse.MonthProjection.builder()
                .monthLabel(futureMonth.format(MONTH_LABEL))
                .monthStart(monthStart)
                .balance(runningBalance.setScale(2, RoundingMode.HALF_UP))
                .projectedIncome(projectedIncome.setScale(2, RoundingMode.HALF_UP))
                .projectedExpense(totalExpenseForMonth.setScale(2, RoundingMode.HALF_UP))
                .build());
        }

        return BalanceProjectionResponse.builder()
            .currentBalance(currentBalance.setScale(2, RoundingMode.HALF_UP))
            .projectionStartDate(now.plusMonths(1).atDay(1))
            .monthsProjected(months)
            .projections(projections)
            .build();
    }

    /**
     * Retorna o impacto mensal de uma transação recorrente com base no padrão.
     */
    private BigDecimal getMonthlyImpact(BigDecimal amount, String pattern) {
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

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UUID)) {
            throw new RuntimeException("Usuário não autenticado");
        }
        return (UUID) auth.getPrincipal();
    }
}
