package com.financeflow.assistant.service;

import com.financeflow.assistant.client.GeminiClient;
import com.financeflow.assistant.client.OllamaClient;
import com.financeflow.assistant.config.GeminiProperties;
import com.financeflow.assistant.config.OllamaProperties;
import com.financeflow.assistant.dto.AssistantChatRequest;
import com.financeflow.assistant.dto.AssistantChatResponse;
import com.financeflow.budgets.domain.Budget;
import com.financeflow.budgets.repository.BudgetRepository;
import com.financeflow.goals.domain.Goal;
import com.financeflow.goals.repository.GoalRepository;
import com.financeflow.installments.service.InstallmentService;
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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Optional;

/**
 * Assistente financeiro que responde perguntas sobre dados do usuário.
 * Utiliza apenas dados do usuário autenticado.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialAssistantService {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Locale.forLanguageTag("pt-BR"));
    private static final DateTimeFormatter MONTH_SHORT = DateTimeFormatter.ofPattern("MM/yyyy");

    private final TransactionRepository transactionRepository;
    private final GoalRepository goalRepository;
    private final BudgetRepository budgetRepository;
    private final InstallmentService installmentService;
    private final GeminiProperties geminiProperties;
    private final OllamaProperties ollamaProperties;
    private final AssistantContextBuilder contextBuilder;
    private final Optional<GeminiClient> geminiClient;
    private final OllamaClient ollamaClient;

    /**
     * Fluxo total IA: quando Gemini ou Ollama está habilitado, toda pergunta vai primeiro para a IA com contexto.
     * Prioridade: Gemini (se configurado) → Ollama → regras (regex) como fallback.
     */
    @Transactional(readOnly = true)
    public AssistantChatResponse processMessage(AssistantChatRequest request) {
        UUID userId = getCurrentUserId();
        String message = request.getMessage().trim();
        String messageLower = message.toLowerCase();

        if (message.isEmpty()) {
            return AssistantChatResponse.builder()
                .message("Por favor, faça uma pergunta sobre suas finanças. Você pode perguntar sobre saldo, gastos, receitas, metas, orçamentos ou parcelamentos.")
                .build();
        }

        // Prioridade: Gemini (se habilitado e configurado) → depois Ollama
        if (geminiProperties.isConfigured() && geminiClient.isPresent()) {
            try {
                String context = contextBuilder.buildContext(userId);
                String reply = geminiClient.get().chat(context, message);
                if (reply != null && !reply.isBlank()) {
                    return AssistantChatResponse.builder()
                        .message(reply.trim())
                        .build();
                }
            } catch (Exception e) {
                log.warn("Gemini falhou, tentando Ollama ou regras: {}", e.getMessage());
            }
        }
        if (ollamaProperties.isEnabled()) {
            try {
                String context = contextBuilder.buildContext(userId);
                String ollamaReply = ollamaClient.chat(context, message);
                if (ollamaReply != null && !ollamaReply.isBlank()) {
                    return AssistantChatResponse.builder()
                        .message(ollamaReply.trim())
                        .build();
                }
            } catch (Exception e) {
                log.warn("Ollama falhou, usando regras como fallback: {}", e.getMessage());
            }
        }

        // Fallback: regras por regex (quando IA desabilitada ou indisponível)
        ParsedQuery parsed = parseQuery(messageLower);
        if (parsed.intent == null) {
            return AssistantChatResponse.builder()
                .message("Não entendi sua pergunta. Tente:\n\n• Quanto gastei com mercado este mês?\n• Quanto ganhei este mês?\n• Qual foi o total de despesas?\n• Qual a porcentagem da minha meta?\n• Como estão os orçamentos?\n• Quanto tenho em parcelamentos? Quais parcelas vencem este mês?")
                .build();
        }

        LocalDate startDate = parsed.startDate;
        LocalDate endDate = parsed.endDate;
        String periodLabel = formatPeriod(startDate, endDate);

        try {
            return switch (parsed.intent) {
                case EXPENSE_BY_KEYWORD -> answerExpenseByKeyword(userId, parsed.keyword, startDate, endDate, periodLabel);
                case EXPENSE_TOTAL -> answerExpenseTotal(userId, startDate, endDate, periodLabel);
                case INCOME_TOTAL -> answerIncomeTotal(userId, startDate, endDate, periodLabel);
                case GOALS_PROGRESS -> answerGoalsProgress(userId);
                case BUDGET_STATUS -> answerBudgetStatus(userId);
                case INSTALLMENTS -> answerInstallments(userId);
            };
        } catch (Exception e) {
            log.error("Erro ao processar pergunta do assistente", e);
            return AssistantChatResponse.builder()
                .message("Ocorreu um erro ao buscar suas informações. Tente novamente.")
                .build();
        }
    }

    private AssistantChatResponse answerExpenseByKeyword(UUID userId, String keyword, LocalDate start, LocalDate end, String periodLabel) {
        BigDecimal total = transactionRepository.sumExpensesByKeywordAndDateRange(userId, keyword, start, end);
        if (total == null) total = BigDecimal.ZERO;

        String message = total.compareTo(BigDecimal.ZERO) == 0
            ? String.format("Você não teve gastos com \"%s\" %s.", keyword, periodLabel)
            : String.format("Você gastou R$ %s com \"%s\" %s.", formatAmount(total), keyword, periodLabel);

        return AssistantChatResponse.builder()
            .message(message)
            .amount(total.setScale(2, RoundingMode.HALF_UP))
            .period(periodLabel)
            .category(keyword)
            .build();
    }

    private AssistantChatResponse answerExpenseTotal(UUID userId, LocalDate start, LocalDate end, String periodLabel) {
        BigDecimal total = transactionRepository.sumExpensesByDateRange(userId, start, end);
        if (total == null) total = BigDecimal.ZERO;

        String message = total.compareTo(BigDecimal.ZERO) == 0
            ? String.format("Você não teve despesas %s.", periodLabel)
            : String.format("Suas despesas totalizaram R$ %s %s.", formatAmount(total), periodLabel);

        return AssistantChatResponse.builder()
            .message(message)
            .amount(total.setScale(2, RoundingMode.HALF_UP))
            .period(periodLabel)
            .build();
    }

    private AssistantChatResponse answerIncomeTotal(UUID userId, LocalDate start, LocalDate end, String periodLabel) {
        BigDecimal total = transactionRepository.sumIncomeByDateRange(userId, start, end);
        if (total == null) total = BigDecimal.ZERO;

        String message = total.compareTo(BigDecimal.ZERO) == 0
            ? String.format("Você não teve receitas %s.", periodLabel)
            : String.format("Suas receitas totalizaram R$ %s %s.", formatAmount(total), periodLabel);

        return AssistantChatResponse.builder()
            .message(message)
            .amount(total.setScale(2, RoundingMode.HALF_UP))
            .period(periodLabel)
            .build();
    }

    private AssistantChatResponse answerGoalsProgress(UUID userId) {
        List<Goal> goals = goalRepository.findAllByUserId(userId).stream()
            .filter(g -> g.getStatus() == Goal.GoalStatus.ACTIVE)
            .toList();

        if (goals.isEmpty()) {
            return AssistantChatResponse.builder()
                .message("Você não tem metas ativas no momento.")
                .build();
        }

        List<String> lines = new ArrayList<>();
        for (Goal g : goals) {
            BigDecimal percent = g.getTargetAmount().compareTo(BigDecimal.ZERO) > 0
                ? g.getCurrentAmount().multiply(BigDecimal.valueOf(100)).divide(g.getTargetAmount(), 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
            lines.add(String.format("• %s: %.0f%% (R$ %s / R$ %s)",
                g.getName(),
                percent.doubleValue(),
                formatAmount(g.getCurrentAmount()),
                formatAmount(g.getTargetAmount())));
        }

        String message = "Progresso das suas metas:\n\n" + String.join("\n", lines);
        return AssistantChatResponse.builder()
            .message(message)
            .build();
    }

    private AssistantChatResponse answerBudgetStatus(UUID userId) {
        YearMonth currentMonth = YearMonth.now();
        LocalDate startMonth = currentMonth.atDay(1);
        LocalDate endMonth = currentMonth.atEndOfMonth();

        List<Budget> budgets = budgetRepository.findAllByUserIdAndMonthRange(userId, startMonth, endMonth);

        if (budgets.isEmpty()) {
            return AssistantChatResponse.builder()
                .message("Você não tem orçamentos configurados para este mês.")
                .build();
        }

        List<String> lines = new ArrayList<>();
        for (Budget b : budgets) {
            if (b.getDeletedAt() != null) continue;

            BigDecimal spent = transactionRepository.sumExpensesByCategoryAndDateRange(
                userId, b.getCategory().getId(), startMonth, endMonth);
            if (spent == null) spent = BigDecimal.ZERO;

            BigDecimal percent = b.getLimitAmount().compareTo(BigDecimal.ZERO) > 0
                ? spent.multiply(BigDecimal.valueOf(100)).divide(b.getLimitAmount(), 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

            String status = percent.compareTo(BigDecimal.valueOf(100)) > 0 ? "excedido" :
                percent.compareTo(BigDecimal.valueOf(80)) >= 0 ? "atenção" : "ok";
            lines.add(String.format("• %s: %.0f%% (%s) - R$ %s / R$ %s",
                b.getCategory().getName(),
                percent.doubleValue(),
                status,
                formatAmount(spent),
                formatAmount(b.getLimitAmount())));
        }

        String message = "Orçamentos deste mês:\n\n" + String.join("\n", lines);
        return AssistantChatResponse.builder()
            .message(message)
            .build();
    }

    private AssistantChatResponse answerInstallments(UUID userId) {
        String summary = installmentService.buildInstallmentSummaryForContext(userId);
        String message = summary.replace("### Parcelamentos ativos (compras parceladas, financiamentos)\n", "Seus parcelamentos ativos:\n\n");
        message = message.replace("- Nenhum parcelamento ativo no momento.\n\n", "Você não tem parcelamentos ativos no momento.");
        message = message.replace("\n\n- TOTAL em parcelamentos (saldo devedor):", "\n\nTotal em parcelamentos (saldo devedor):");
        if (message.endsWith("\n\n")) {
            message = message.trim();
        }
        return AssistantChatResponse.builder()
            .message(message)
            .build();
    }

    private ParsedQuery parseQuery(String message) {
        ParsedQuery parsed = new ParsedQuery();
        parsed.startDate = LocalDate.now().withDayOfMonth(1);
        parsed.endDate = LocalDate.now();

        // Período: este mês, mês passado, este ano
        if (message.contains("este mês") || message.contains("mês atual") || message.contains("mês corrente")) {
            parsed.startDate = LocalDate.now().withDayOfMonth(1);
            parsed.endDate = LocalDate.now();
        } else if (message.contains("mês passado") || message.contains("último mês")) {
            YearMonth lastMonth = YearMonth.now().minusMonths(1);
            parsed.startDate = lastMonth.atDay(1);
            parsed.endDate = lastMonth.atEndOfMonth();
        } else if (message.contains("este ano") || message.contains("ano atual")) {
            parsed.startDate = LocalDate.now().withDayOfYear(1);
            parsed.endDate = LocalDate.now();
        } else if (message.contains("mês") && message.matches(".*\\d{1,2}.*")) {
            Pattern p = Pattern.compile("(\\d{1,2})/(\\d{4})");
            Matcher m = p.matcher(message);
            if (m.find()) {
                int month = Integer.parseInt(m.group(1));
                int year = Integer.parseInt(m.group(2));
                YearMonth ym = YearMonth.of(year, month);
                parsed.startDate = ym.atDay(1);
                parsed.endDate = ym.atEndOfMonth();
            }
        }

        // Intent: gasto com X, quanto gastei, despesas, receitas
        if (message.contains("quanto gastei") || message.contains("quanto eu gastei") || message.contains("total gasto")) {
            String keyword = extractKeyword(message);
            if (keyword != null && !keyword.isEmpty()) {
                parsed.intent = Intent.EXPENSE_BY_KEYWORD;
                parsed.keyword = keyword;
            } else {
                parsed.intent = Intent.EXPENSE_TOTAL;
            }
        } else if (message.contains("despesas") || message.contains("gastos totais") || message.contains("total de despesas")) {
            parsed.intent = Intent.EXPENSE_TOTAL;
        } else if ((message.contains("gastos") || message.contains("gasto")) && (message.contains("mês") || message.contains("mes") || message.contains("meus") || message.contains("meu ") || message.contains("como esta") || message.contains("como estão") || message.contains("esse") || message.contains("este "))) {
            parsed.intent = Intent.EXPENSE_TOTAL;
        } else if (message.contains("quanto ganhei") || message.contains("quanto eu ganhei") || message.contains("receitas") || message.contains("total de receitas")) {
            parsed.intent = Intent.INCOME_TOTAL;
        } else if (message.contains("meta") || message.contains("metas") || message.contains("objetivo") || message.contains("objetivos") ||
            message.contains("minha meta") || message.contains("minhas metas") ||
            (message.contains("porcentagem") || message.contains("percentual")) && (message.contains("meta") || message.contains("objetivo")) ||
            message.contains("progresso") && (message.contains("meta") || message.contains("objetivo"))) {
            parsed.intent = Intent.GOALS_PROGRESS;
        } else if (message.contains("orçamento") || message.contains("orcamento") || message.contains("orçamentos") || message.contains("orcamentos") ||
            message.contains("gastei do orçamento") || message.contains("como estão os orçamentos") ||
            message.contains("como estão") && message.contains("orçamento")) {
            parsed.intent = Intent.BUDGET_STATUS;
        } else if (message.contains("parcelamento") || message.contains("parcelas") || message.contains("parcela") ||
            message.contains("quanto devo") || message.contains("quanto tenho em parcelas") ||
            message.contains("financiamento") || message.contains("compras parceladas") ||
            message.contains("próximas parcelas") || message.contains("parcelas vencem") || message.contains("vencimento")) {
            parsed.intent = Intent.INSTALLMENTS;
        }

        return parsed;
    }

    private String extractKeyword(String message) {
        String[] patterns = {
            "com\\s+([^\\s?]+(?:\\s+[^\\s?]+)?)\\s+(?:este|mês|passado|ano)",
            "em\\s+([^\\s?]+(?:\\s+[^\\s?]+)?)\\s+(?:este|mês|passado|ano)",
            "com\\s+([^\\s?]+(?:\\s+[^\\s?]+)?)\\s*\\?",
            "em\\s+([^\\s?]+(?:\\s+[^\\s?]+)?)\\s*\\?",
            "em\\s+([^\\s?]+)",
            "com\\s+([^\\s?]+)"
        };

        for (String p : patterns) {
            Matcher m = Pattern.compile(p).matcher(message);
            if (m.find()) {
                String kw = m.group(1).trim();
                if (kw.length() >= 2 && !kw.matches("^(este|mês|passado|ano|o|a|os|as)$")) {
                    return kw;
                }
            }
        }
        return null;
    }

    private String formatPeriod(LocalDate start, LocalDate end) {
        if (start.equals(end)) {
            return "em " + start.format(MONTH_FORMAT);
        }
        if (start.getMonth() == end.getMonth() && start.getYear() == end.getYear()) {
            return "em " + start.format(MONTH_FORMAT);
        }
        return "de " + start.format(MONTH_SHORT) + " a " + end.format(MONTH_SHORT);
    }

    private String formatAmount(BigDecimal amount) {
        return NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR")).format(amount);
    }

    private UUID getCurrentUserId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UUID)) {
            throw new RuntimeException("Usuário não autenticado");
        }
        return (UUID) auth.getPrincipal();
    }

    private enum Intent {
        EXPENSE_BY_KEYWORD,
        EXPENSE_TOTAL,
        INCOME_TOTAL,
        GOALS_PROGRESS,
        BUDGET_STATUS,
        INSTALLMENTS
    }

    private static class ParsedQuery {
        Intent intent;
        String keyword;
        LocalDate startDate;
        LocalDate endDate;
    }
}
