package com.financeflow.assistant.service;

import com.financeflow.accounts.domain.Account;
import com.financeflow.accounts.repository.AccountRepository;
import com.financeflow.budgets.domain.Budget;
import com.financeflow.budgets.repository.BudgetRepository;
import com.financeflow.goals.domain.Goal;
import com.financeflow.goals.repository.GoalRepository;
import com.financeflow.installments.service.InstallmentService;
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
import java.util.*;

/**
 * Monta o contexto financeiro do usuário para envio à IA (Gemini ou Ollama): resumo, contas, metas, orçamentos, parcelamentos, top categorias.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AssistantContextBuilder {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final GoalRepository goalRepository;
    private final BudgetRepository budgetRepository;
    private final InstallmentService installmentService;

    @Transactional(readOnly = true)
    public String buildContext(UUID userId) {
        YearMonth currentMonth = YearMonth.now();
        LocalDate startMonth = currentMonth.atDay(1);
        LocalDate endMonth = currentMonth.atEndOfMonth();

        BigDecimal totalIncome = transactionRepository.sumIncomeByDateRange(userId, startMonth, endMonth);
        BigDecimal totalExpense = transactionRepository.sumExpensesByDateRange(userId, startMonth, endMonth);
        if (totalIncome == null) totalIncome = BigDecimal.ZERO;
        if (totalExpense == null) totalExpense = BigDecimal.ZERO;
        BigDecimal totalBalance = accountRepository.findAllByUserId(userId).stream()
                .filter(a -> a.getDeletedAt() == null)
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        StringBuilder sb = new StringBuilder();
        sb.append("## Dados financeiros do usuário (use apenas para responder)\n\n");
        sb.append("### Resumo do mês atual (use estes valores para perguntas como 'quanto gastei/ganhei este mês')\n");
        sb.append("- Saldo total: R$ ").append(formatAmount(totalBalance)).append("\n");
        sb.append("- Receitas do mês: R$ ").append(formatAmount(totalIncome)).append("\n");
        sb.append("- Despesas do mês: R$ ").append(formatAmount(totalExpense)).append("\n");
        sb.append("(Para 'quanto gastei este mês' use Despesas do mês; para 'quanto ganhei' use Receitas do mês.)\n\n");

        List<Account> accounts = accountRepository.findAllByUserId(userId).stream()
                .filter(a -> a.getDeletedAt() == null)
                .toList();
        sb.append("### Contas\n");
        if (accounts.isEmpty()) {
            sb.append("- Nenhuma conta cadastrada\n");
        } else {
            for (Account a : accounts) {
                sb.append("- ").append(a.getName()).append(" | ").append(a.getType().name()).append(" | R$ ").append(formatAmount(a.getBalance())).append("\n");
            }
        }
        sb.append("\n");

        List<Goal> goals = goalRepository.findAllByUserId(userId).stream()
                .filter(g -> g.getStatus() == Goal.GoalStatus.ACTIVE)
                .toList();
        sb.append("### Metas ativas\n");
        if (goals.isEmpty()) {
            sb.append("- Nenhuma meta ativa\n");
        } else {
            for (Goal g : goals) {
                BigDecimal pct = g.getTargetAmount().compareTo(BigDecimal.ZERO) > 0
                        ? g.getCurrentAmount().multiply(BigDecimal.valueOf(100)).divide(g.getTargetAmount(), 1, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                sb.append("- ").append(g.getName()).append(" | Valor alvo R$ ").append(formatAmount(g.getTargetAmount()))
                        .append(" | Valor atual R$ ").append(formatAmount(g.getCurrentAmount()))
                        .append(" | ").append(pct).append("% concluído\n");
            }
        }
        sb.append("\n");

        List<Budget> budgets = budgetRepository.findAllByUserIdAndMonthRange(userId, startMonth, endMonth);
        sb.append("### Orçamentos deste mês\n");
        if (budgets.isEmpty()) {
            sb.append("- Nenhum orçamento este mês\n");
        } else {
            for (Budget b : budgets) {
                if (b.getDeletedAt() != null) continue;
                BigDecimal spent = transactionRepository.sumExpensesByCategoryAndDateRange(
                        userId, b.getCategory().getId(), startMonth, endMonth);
                if (spent == null) spent = BigDecimal.ZERO;
                BigDecimal pct = b.getLimitAmount().compareTo(BigDecimal.ZERO) > 0
                        ? spent.multiply(BigDecimal.valueOf(100)).divide(b.getLimitAmount(), 1, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                sb.append("- ").append(b.getCategory().getName()).append(" | Limite R$ ").append(formatAmount(b.getLimitAmount()))
                        .append(" | Gasto R$ ").append(formatAmount(spent)).append(" | ").append(pct).append("% usado\n");
            }
        }
        sb.append("\n");

        List<Transaction> monthExpenses = transactionRepository.findUpcomingExpensesByUserId(userId, startMonth, endMonth);
        Map<String, BigDecimal> byCategory = new HashMap<>();
        for (Transaction t : monthExpenses) {
            String name = t.getCategory().getName();
            byCategory.merge(name, t.getAmount(), BigDecimal::add);
        }
        List<Map.Entry<String, BigDecimal>> top5 = byCategory.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(5)
                .toList();
        sb.append("### Top 5 categorias de gastos (este mês)\n");
        if (top5.isEmpty()) {
            sb.append("- Nenhum gasto neste mês\n");
        } else {
            for (Map.Entry<String, BigDecimal> e : top5) {
                sb.append("- ").append(e.getKey()).append(" | R$ ").append(formatAmount(e.getValue())).append("\n");
            }
        }
        sb.append("\n");

        sb.append(installmentService.buildInstallmentSummaryForContext(userId));

        return sb.toString();
    }

    private static String formatAmount(BigDecimal amount) {
        return NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR")).format(amount != null ? amount : BigDecimal.ZERO);
    }
}
