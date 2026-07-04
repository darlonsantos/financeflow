package com.financeflow.automation.service;

import com.financeflow.accounts.domain.Account;
import com.financeflow.accounts.repository.AccountRepository;
import com.financeflow.automation.domain.AutomationRule;
import com.financeflow.automation.repository.AutomationRuleRepository;
import com.financeflow.notifications.domain.Notification;
import com.financeflow.notifications.repository.NotificationRepository;
import com.financeflow.transactions.domain.Transaction;
import com.financeflow.transactions.repository.TransactionRepository;
import com.financeflow.users.domain.User;
import com.financeflow.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutomationRuleProcessor {

    private static final String TAG_REVISAR = "revisar";
    private static final int RULE_ALERT_COOLDOWN_HOURS = 24;

    private final AutomationRuleRepository ruleRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * Avalia regras de transação (categoria + valor) e aplica ação "marcar como revisar".
     */
    @Transactional
    public void processTransactionRules(UUID userId, Transaction transaction) {
        List<AutomationRule> rules = ruleRepository.findActiveByUserIdAndConditionType(
                userId, AutomationRule.ConditionType.TRANSACTION_CATEGORY_AMOUNT.name());
        for (AutomationRule rule : rules) {
            if (!AutomationRule.ActionType.MARK_REVIEW.name().equals(rule.getActionType())) continue;
            if (!evaluateTransactionCondition(rule.getConditionConfig(), transaction)) continue;
            addReviewTagIfNeeded(transaction);
        }
    }

    /**
     * Avalia regras de saldo de conta e cria notificação urgente quando abaixo do limite.
     */
    @Transactional
    public void processAccountBalanceRules(UUID userId) {
        List<AutomationRule> rules = ruleRepository.findActiveByUserIdAndConditionType(
                userId, AutomationRule.ConditionType.ACCOUNT_BALANCE.name());
        if (rules.isEmpty()) return;

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        java.time.LocalDateTime cooldownSince = java.time.LocalDateTime.now().minusHours(RULE_ALERT_COOLDOWN_HOURS);
        List<Account> accounts = accountRepository.findAllByUserId(userId);

        for (AutomationRule rule : rules) {
            if (!AutomationRule.ActionType.URGENT_ALERT.name().equals(rule.getActionType())) continue;
            Map<String, Object> config = rule.getConditionConfig();
            if (config == null) continue;
            Object accountIdObj = config.get("accountId");
            Object amountObj = config.get("amount");
            String operator = config.get("operator") != null ? config.get("operator").toString() : "LT";
            if (accountIdObj == null || amountObj == null) continue;

            UUID accountId = accountIdObj instanceof UUID ? (UUID) accountIdObj : UUID.fromString(accountIdObj.toString());
            BigDecimal threshold = toBigDecimal(amountObj);
            Optional<Account> accountOpt = accounts.stream().filter(a -> a.getId().equals(accountId)).findFirst();
            if (accountOpt.isEmpty()) continue;

            Account account = accountOpt.get();
            if (account.getDeletedAt() != null) continue;
            boolean match = false;
            int cmp = account.getBalance().compareTo(threshold);
            if ("LT".equalsIgnoreCase(operator)) match = cmp < 0;
            else if ("LTE".equalsIgnoreCase(operator)) match = cmp <= 0;
            if (!match) continue;

            if (notificationRepository.existsRecentNotificationWithEntity(
                    userId, Notification.NotificationType.RULE_ALERT.name(), rule.getId(), cooldownSince)) continue;

            String title = "Alerta de regra: " + rule.getName();
            String message = String.format("A conta '%s' está com saldo R$ %s (abaixo do limite R$ %s configurado na regra).",
                    account.getName(), account.getBalance(), threshold);
            Notification notification = Notification.builder()
                    .user(user)
                    .type(Notification.NotificationType.RULE_ALERT.name())
                    .title(title)
                    .message(message)
                    .read(false)
                    .entityType("AutomationRule")
                    .entityId(rule.getId())
                    .metadata(Map.of("accountId", account.getId().toString(), "balance", account.getBalance().toString(), "threshold", threshold.toString()))
                    .build();
            notificationRepository.save(notification);
            log.debug("Rule alert notification created: ruleId={}, accountId={}", rule.getId(), account.getId());
        }
    }

    private boolean evaluateTransactionCondition(Map<String, Object> config, Transaction transaction) {
        if (config == null) return false;
        Object categoryIdObj = config.get("categoryId");
        Object amountObj = config.get("amount");
        String operator = config.get("operator") != null ? config.get("operator").toString() : "GT";
        if (categoryIdObj == null || amountObj == null) return false;

        UUID ruleCategoryId = categoryIdObj instanceof UUID ? (UUID) categoryIdObj : UUID.fromString(categoryIdObj.toString());
        if (!transaction.getCategory().getId().equals(ruleCategoryId)) return false;
        if (transaction.getType() != Transaction.TransactionType.EXPENSE) return false;

        BigDecimal threshold = toBigDecimal(amountObj);
        int cmp = transaction.getAmount().compareTo(threshold);
        return switch (operator.toUpperCase()) {
            case "GT" -> cmp > 0;
            case "GTE" -> cmp >= 0;
            case "LT" -> cmp < 0;
            case "LTE" -> cmp <= 0;
            case "EQ" -> cmp == 0;
            default -> cmp > 0;
        };
    }

    private void addReviewTagIfNeeded(Transaction transaction) {
        List<String> tags = transaction.getTags() != null ? new ArrayList<>(transaction.getTags()) : new ArrayList<>();
        if (tags.stream().anyMatch(t -> TAG_REVISAR.equalsIgnoreCase(t))) return;
        tags.add(TAG_REVISAR);
        transaction.setTags(tags);
        transactionRepository.save(transaction);
    }

    private static BigDecimal toBigDecimal(Object o) {
        if (o instanceof BigDecimal) return (BigDecimal) o;
        if (o instanceof Number) return BigDecimal.valueOf(((Number) o).doubleValue());
        return new BigDecimal(o.toString());
    }
}
