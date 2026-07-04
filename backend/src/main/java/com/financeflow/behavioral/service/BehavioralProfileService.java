package com.financeflow.behavioral.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financeflow.assistant.client.GeminiClient;
import com.financeflow.assistant.config.GeminiProperties;
import com.financeflow.assistant.service.AssistantContextBuilder;
import com.financeflow.behavioral.dto.BehavioralProfileResponse;
import com.financeflow.goals.domain.Goal;
import com.financeflow.goals.repository.GoalRepository;
import com.financeflow.installments.domain.InstallmentItem;
import com.financeflow.installments.repository.InstallmentItemRepository;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Perfil financeiro comportamental: classificação por IA (Gemini) ou por regras.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BehavioralProfileService {

    private static final int MONTHS_ANALYSIS = 6;
    private static final String BEHAVIORAL_REQUEST = """
        Você é um especialista em comportamento financeiro, educação financeira e análise de padrões de consumo.
        Tarefa: Analise os dados fornecidos e classifique o perfil financeiro comportamental do usuário, identificando padrões, riscos e oportunidades de melhoria.
        Critérios de análise (use os dados quando disponíveis):
        - Percentual de gastos em relação à renda
        - Regularidade dos gastos (variação mês a mês)
        - Frequência de compras (quantidade de despesas pode indicar impulso)
        - Uso de parcelamentos e endividamento
        - Existência de metas financeiras
        - Capacidade de poupança (sobra renda - despesas)
        (Atrasos em pagamentos não são registrados; use endividamento/parcelamentos como indicador.)
        Retorne OBRIGATORIAMENTE um JSON válido, sem texto antes ou depois:
        {"profileType":"<Conservador|Planejador|Impulsivo|Equilibrado|Arrojado|Desorganizado>","riskLevel":"<Baixo|Médio|Alto>","patterns":["padrão1","padrão2"],"criticalPoints":["ponto1","ponto2"],"suggestions":["sugestão1","sugestão2"]}
        Use no máximo 5 itens em patterns, criticalPoints e suggestions. Seja objetivo e em português brasileiro.
        """;

    private final AssistantContextBuilder contextBuilder;
    private final TransactionRepository transactionRepository;
    private final GoalRepository goalRepository;
    private final InstallmentItemRepository installmentItemRepository;
    private final GeminiProperties geminiProperties;
    private final Optional<GeminiClient> geminiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public BehavioralProfileResponse getProfile() {
        UUID userId = getCurrentUserId();
        String context = buildBehavioralContext(userId);

        if (geminiProperties.isConfigured() && geminiClient.isPresent()) {
            String response = geminiClient.get().chat(context, BEHAVIORAL_REQUEST);
            BehavioralProfileResponse parsed = parseGeminiResponse(response);
            if (parsed != null) {
                return BehavioralProfileResponse.builder()
                    .profileType(parsed.getProfileType())
                    .riskLevel(parsed.getRiskLevel())
                    .patterns(parsed.getPatterns() != null ? parsed.getPatterns() : List.of())
                    .criticalPoints(parsed.getCriticalPoints() != null ? parsed.getCriticalPoints() : List.of())
                    .suggestions(parsed.getSuggestions() != null ? parsed.getSuggestions() : List.of())
                    .generatedAt(java.time.Instant.now())
                    .fromAi(true)
                    .build();
            }
            log.warn("Falha ao interpretar resposta do Gemini para perfil comportamental, usando fallback.");
        }

        return buildRuleBasedProfile(userId, context);
    }

    private String buildBehavioralContext(UUID userId) {
        StringBuilder sb = new StringBuilder();
        sb.append(contextBuilder.buildContext(userId));

        YearMonth now = YearMonth.now();
        BigDecimal totalIncome6 = BigDecimal.ZERO;
        BigDecimal totalExpense6 = BigDecimal.ZERO;
        int transactionCount = 0;
        List<BigDecimal> monthlyExpenses = new ArrayList<>();

        for (int i = 0; i < MONTHS_ANALYSIS; i++) {
            YearMonth ym = now.minusMonths(i);
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();
            BigDecimal inc = transactionRepository.sumIncomeByDateRange(userId, start, end);
            BigDecimal exp = transactionRepository.sumExpensesByDateRange(userId, start, end);
            if (inc == null) inc = BigDecimal.ZERO;
            if (exp == null) exp = BigDecimal.ZERO;
            totalIncome6 = totalIncome6.add(inc);
            totalExpense6 = totalExpense6.add(exp);
            monthlyExpenses.add(exp);
        }

        long expenseCount = transactionRepository.findAllByUserIdAndDateRangeForReport(
            userId,
            now.minusMonths(MONTHS_ANALYSIS).atDay(1),
            now.atEndOfMonth()
        ).stream().filter(t -> t.getType() == Transaction.TransactionType.EXPENSE).count();
        transactionCount = (int) expenseCount;

        BigDecimal avgIncome = totalIncome6.divide(BigDecimal.valueOf(MONTHS_ANALYSIS), 2, RoundingMode.HALF_UP);
        BigDecimal avgExpense = totalExpense6.divide(BigDecimal.valueOf(MONTHS_ANALYSIS), 2, RoundingMode.HALF_UP);
        BigDecimal expenseIncomeRatio = avgIncome.compareTo(BigDecimal.ZERO) > 0
            ? totalExpense6.divide(totalIncome6, 4, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        BigDecimal savings6 = totalIncome6.subtract(totalExpense6);
        BigDecimal savingsRate = totalIncome6.compareTo(BigDecimal.ZERO) > 0
            ? savings6.divide(totalIncome6, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
            : BigDecimal.ZERO;

        List<Goal> goals = goalRepository.findAllByUserId(userId).stream()
            .filter(g -> g.getStatus() == Goal.GoalStatus.ACTIVE)
            .toList();

        LocalDate from = LocalDate.now();
        LocalDate to = from.plusYears(2);
        List<InstallmentItem> pending = installmentItemRepository.findPendingByUserIdAndDueDateBetween(userId, from, to);
        BigDecimal totalPendingInstallments = pending.stream()
            .map(InstallmentItem::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        sb.append("\n### Métricas para perfil comportamental (últimos ").append(MONTHS_ANALYSIS).append(" meses)\n");
        sb.append("- Receita total: R$ ").append(formatAmount(totalIncome6)).append(" | Média mensal: R$ ").append(formatAmount(avgIncome)).append("\n");
        sb.append("- Despesa total: R$ ").append(formatAmount(totalExpense6)).append(" | Média mensal: R$ ").append(formatAmount(avgExpense)).append("\n");
        sb.append("- Percentual gastos/renda: ").append(expenseIncomeRatio.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP)).append("%\n");
        sb.append("- Capacidade de poupança: sobra R$ ").append(formatAmount(savings6)).append(" no período | Taxa de poupança: ").append(savingsRate.setScale(1, RoundingMode.HALF_UP)).append("% da renda\n");
        sb.append("- Regularidade: despesa por mês (mais recente primeiro): ");
        for (int i = 0; i < monthlyExpenses.size(); i++) {
            if (i > 0) sb.append(" | ");
            sb.append("R$ ").append(formatAmount(monthlyExpenses.get(i)));
        }
        sb.append("\n");
        sb.append("- Quantidade de despesas no período: ").append(transactionCount).append(" (muitas despesas podem indicar compras por impulso)\n");
        sb.append("- Metas financeiras ativas: ").append(goals.size()).append("\n");
        sb.append("- Total em parcelamentos pendentes (endividamento): R$ ").append(formatAmount(totalPendingInstallments)).append("\n");
        if (avgIncome.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal installmentBurden = totalPendingInstallments.divide(avgIncome.multiply(BigDecimal.valueOf(MONTHS_ANALYSIS)), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            sb.append("- Parcelamentos em relação à renda (período): ").append(installmentBurden.setScale(1, RoundingMode.HALF_UP)).append("%\n");
        }
        sb.append("- (Atrasos em pagamentos não são registrados no sistema; use endividamento e parcelamentos como indicadores.)\n");

        return sb.toString();
    }

    private BehavioralProfileResponse parseGeminiResponse(String response) {
        if (response == null || response.isBlank()) return null;
        String json = response.trim();
        Matcher m = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```").matcher(json);
        if (m.find()) json = m.group(1).trim();
        if (json.startsWith("{")) {
            try {
                JsonNode root = objectMapper.readTree(json);
                BehavioralProfileResponse out = new BehavioralProfileResponse();
                out.setProfileType(getText(root, "profileType", "Desconhecido"));
                out.setRiskLevel(getText(root, "riskLevel", "Médio"));
                out.setPatterns(getStringList(root, "patterns"));
                out.setCriticalPoints(getStringList(root, "criticalPoints"));
                out.setSuggestions(getStringList(root, "suggestions"));
                return out;
            } catch (JsonProcessingException e) {
                log.debug("Parse JSON perfil comportamental: {}", e.getMessage());
                return null;
            }
        }
        return null;
    }

    private static String getText(JsonNode root, String key, String defaultVal) {
        JsonNode n = root.get(key);
        return n != null && n.isTextual() ? n.asText().trim() : defaultVal;
    }

    private static List<String> getStringList(JsonNode root, String key) {
        JsonNode arr = root.get(key);
        if (arr == null || !arr.isArray()) return List.of();
        List<String> list = new ArrayList<>();
        for (JsonNode item : arr) {
            if (item.isTextual()) list.add(item.asText().trim());
        }
        return list;
    }

    private BehavioralProfileResponse buildRuleBasedProfile(UUID userId, String context) {
        YearMonth now = YearMonth.now();
        LocalDate start = now.minusMonths(MONTHS_ANALYSIS).atDay(1);
        LocalDate end = now.atEndOfMonth();
        BigDecimal totalIncome = transactionRepository.sumIncomeByDateRange(userId, start, end);
        BigDecimal totalExpense = transactionRepository.sumExpensesByDateRange(userId, start, end);
        if (totalIncome == null) totalIncome = BigDecimal.ZERO;
        if (totalExpense == null) totalExpense = BigDecimal.ZERO;
        BigDecimal ratio = totalIncome.compareTo(BigDecimal.ZERO) > 0
            ? totalExpense.divide(totalIncome, 4, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        BigDecimal savings = totalIncome.subtract(totalExpense);
        long expenseCount = transactionRepository.findAllByUserIdAndDateRangeForReport(userId, start, end)
            .stream().filter(t -> t.getType() == Transaction.TransactionType.EXPENSE).count();

        List<Goal> goals = goalRepository.findAllByUserId(userId).stream()
            .filter(g -> g.getStatus() == Goal.GoalStatus.ACTIVE)
            .toList();
        long activeGoals = goals.size();

        LocalDate from = LocalDate.now();
        LocalDate to = from.plusYears(2);
        BigDecimal pendingInstallments = installmentItemRepository.findPendingByUserIdAndDueDateBetween(userId, from, to)
            .stream().map(InstallmentItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgIncome = totalIncome.divide(BigDecimal.valueOf(MONTHS_ANALYSIS), 2, RoundingMode.HALF_UP);
        BigDecimal installmentRatio = avgIncome.compareTo(BigDecimal.ZERO) > 0
            ? pendingInstallments.divide(avgIncome, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
            : BigDecimal.ZERO;

        String profileType = "Equilibrado";
        String riskLevel = "Médio";
        List<String> patterns = new ArrayList<>();
        List<String> criticalPoints = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        if (ratio.compareTo(new BigDecimal("0.95")) >= 0) {
            profileType = "Desorganizado";
            riskLevel = "Alto";
            criticalPoints.add("Gastos muito próximos ou acima da renda.");
            suggestions.add("Reduza despesas não essenciais e monitore orçamentos.");
        } else if (ratio.compareTo(new BigDecimal("0.90")) >= 0 && expenseCount > 30) {
            profileType = "Impulsivo";
            riskLevel = "Alto";
            patterns.add("Muitas transações de despesa e gastos elevados em relação à renda.");
            criticalPoints.add("Possível padrão de compras por impulso.");
            suggestions.add("Evite compras por impulso; defina lista antes de gastar.");
        } else if (ratio.compareTo(new BigDecimal("0.70")) <= 0 && activeGoals > 0) {
            profileType = "Planejador";
            riskLevel = "Baixo";
            patterns.add("Boa capacidade de poupança e metas definidas.");
            if (savings.compareTo(BigDecimal.ZERO) > 0) {
                patterns.add("Taxa de poupança positiva no período.");
            }
        } else if (ratio.compareTo(new BigDecimal("0.50")) <= 0) {
            profileType = "Conservador";
            riskLevel = "Baixo";
            patterns.add("Gastos bem abaixo da renda e boa capacidade de poupança.");
        } else if (ratio.compareTo(new BigDecimal("0.75")) <= 0 && savings.compareTo(BigDecimal.ZERO) > 0) {
            patterns.add("Capacidade de poupança no período (sobra positiva).");
        }

        if (installmentRatio.compareTo(new BigDecimal("30")) > 0) {
            if ("Alto".equals(riskLevel)) {
                criticalPoints.add("Parcelamentos altos em relação à renda.");
            } else {
                riskLevel = "Médio";
                criticalPoints.add("Atenção ao volume de parcelamentos.");
            }
            suggestions.add("Evite novas compras parceladas até reduzir o comprometimento.");
        }

        if (activeGoals == 0 && ratio.compareTo(new BigDecimal("0.85")) < 0) {
            suggestions.add("Considere definir metas financeiras para direcionar a sobra.");
        }

        if (patterns.isEmpty()) patterns.add("Análise baseada em dados dos últimos " + MONTHS_ANALYSIS + " meses.");
        if (criticalPoints.isEmpty()) criticalPoints.add("Nenhum ponto crítico evidente nos dados atuais.");
        if (suggestions.isEmpty()) suggestions.add("Mantenha o acompanhamento das finanças e dos orçamentos.");

        return BehavioralProfileResponse.builder()
            .profileType(profileType)
            .riskLevel(riskLevel)
            .patterns(patterns)
            .criticalPoints(criticalPoints)
            .suggestions(suggestions)
            .generatedAt(java.time.Instant.now())
            .fromAi(false)
            .build();
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
