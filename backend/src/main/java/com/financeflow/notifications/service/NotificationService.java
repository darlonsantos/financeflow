package com.financeflow.notifications.service;

import com.financeflow.accounts.domain.Account;
import com.financeflow.accounts.repository.AccountRepository;
import com.financeflow.budgets.domain.Budget;
import com.financeflow.budgets.repository.BudgetRepository;
import com.financeflow.automation.service.AutomationRuleProcessor;
import com.financeflow.goals.domain.Goal;
import com.financeflow.goals.repository.GoalRepository;
import com.financeflow.notifications.domain.Notification;
import com.financeflow.notifications.domain.NotificationPreferences;
import com.financeflow.notifications.dto.NotificationPreferencesRequest;
import com.financeflow.notifications.dto.NotificationPreferencesResponse;
import com.financeflow.notifications.dto.NotificationResponse;
import com.financeflow.notifications.mapper.NotificationMapper;
import com.financeflow.notifications.repository.NotificationPreferencesRepository;
import com.financeflow.notifications.repository.NotificationRepository;
import com.financeflow.transactions.domain.Transaction;
import com.financeflow.transactions.repository.TransactionRepository;
import com.financeflow.config.exception.UnauthenticatedException;
import com.financeflow.email.EmailService;
import com.financeflow.predictive.dto.PredictiveAlertDto;
import com.financeflow.predictive.dto.PredictiveReportResponse;
import com.financeflow.predictive.service.PredictiveIntelligenceService;
import com.financeflow.users.domain.User;
import com.financeflow.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private static final int BILLS_DUE_DAYS = 7;
    private static final int GOAL_DUE_DAYS = 30;
    private static final int NOTIFICATION_COOLDOWN_HOURS = 24;
    private static final int PREDICTIVE_COOLDOWN_HOURS = 24;

    private final NotificationRepository notificationRepository;
    private final NotificationPreferencesRepository preferencesRepository;
    private final NotificationMapper notificationMapper;
    private final BudgetRepository budgetRepository;
    private final GoalRepository goalRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AutomationRuleProcessor automationRuleProcessor;
    private final PredictiveIntelligenceService predictiveIntelligenceService;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public Page<NotificationResponse> findAllByUserId(Pageable pageable) {
        UUID userId = getCurrentUserId();
        return notificationRepository.findAllByUserId(userId, pageable)
                .map(notificationMapper::toResponse);
    }

    public long countUnread() {
        return notificationRepository.countUnreadByUserId(getCurrentUserId());
    }

    @Transactional
    public void markAsRead(UUID id) {
        UUID userId = getCurrentUserId();
        notificationRepository.markAsRead(id, userId);
    }

    @Transactional
    public void markAllAsRead() {
        notificationRepository.markAllAsRead(getCurrentUserId());
    }

    public NotificationPreferencesResponse getPreferences() {
        UUID userId = getCurrentUserId();
        NotificationPreferences prefs = getOrCreatePreferences(userId);
        return toPreferencesResponse(prefs);
    }

    @Transactional
    public NotificationPreferencesResponse updatePreferences(NotificationPreferencesRequest request) {
        UUID userId = getCurrentUserId();
        NotificationPreferences prefs = getOrCreatePreferences(userId);

        if (request.getBudgetExceededEnabled() != null) {
            prefs.setBudgetExceededEnabled(request.getBudgetExceededEnabled());
        }
        if (request.getLowBalanceEnabled() != null) {
            prefs.setLowBalanceEnabled(request.getLowBalanceEnabled());
        }
        if (request.getBillsDueEnabled() != null) {
            prefs.setBillsDueEnabled(request.getBillsDueEnabled());
        }
        if (request.getGoalDueEnabled() != null) {
            prefs.setGoalDueEnabled(request.getGoalDueEnabled());
        }
        if (request.getEmailEnabled() != null) {
            prefs.setEmailEnabled(request.getEmailEnabled());
        }
        if (request.getLowBalanceThreshold() != null) {
            prefs.setLowBalanceThreshold(request.getLowBalanceThreshold());
        }

        prefs = preferencesRepository.save(prefs);
        return toPreferencesResponse(prefs);
    }

    /**
     * Gera notificações para o usuário atual. Chamado ao abrir app ou manualmente.
     * Inclui alertas da inteligência preditiva (alta prioridade) com cooldown.
     */
    @Transactional
    public void generateNotificationsForCurrentUser() {
        UUID userId = getCurrentUserId();
        generateNotificationsForUser(userId);

        // Alertas preventivos da inteligência preditiva (apenas para usuário atual, com cooldown)
        LocalDateTime predictiveCooldown = LocalDateTime.now().minusHours(PREDICTIVE_COOLDOWN_HOURS);
        if (!notificationRepository.existsRecentNotificationWithNullEntity(
                userId, Notification.NotificationType.PREDICTIVE_RISK.name(), predictiveCooldown)) {
            try {
                PredictiveReportResponse report = predictiveIntelligenceService.generateReport();
                if (report.getAlerts() != null) {
                    List<PredictiveAlertDto> highAlerts = report.getAlerts().stream()
                            .filter(a -> "HIGH".equals(a.getSeverity()))
                            .toList();
                    if (!highAlerts.isEmpty()) {
                        String title = highAlerts.size() == 1
                                ? highAlerts.get(0).getTitle()
                                : "Inteligência preditiva: " + highAlerts.size() + " alertas de alta prioridade";
                        String message = highAlerts.size() == 1
                                ? highAlerts.get(0).getMessage()
                                : highAlerts.stream()
                                        .map(PredictiveAlertDto::getTitle)
                                        .reduce((a, b) -> a + "; " + b)
                                        .orElse("");
                        createNotification(userId, getOrCreatePreferences(userId),
                                Notification.NotificationType.PREDICTIVE_RISK,
                                title, message, "PredictiveReport", null,
                                Map.of("alertCount", highAlerts.size(), "summary", report.getSummary() != null ? report.getSummary() : ""));
                    }
                }
            } catch (Exception e) {
                log.warn("Falha ao gerar notificação preditiva: {}", e.getMessage());
            }
        }
    }

    /**
     * Gera notificações para o usuário com base nas preferências e condições atuais.
     * Pode ser chamado ao fazer login ou periodicamente.
     */
    @Transactional
    public void generateNotificationsForUser(UUID userId) {
        NotificationPreferences prefs = getOrCreatePreferences(userId);
        LocalDateTime cooldownSince = LocalDateTime.now().minusHours(NOTIFICATION_COOLDOWN_HOURS);

        // 1. Orçamento excedido
        if (Boolean.TRUE.equals(prefs.getBudgetExceededEnabled())) {
            generateBudgetExceededNotifications(userId, prefs, prefs.getLowBalanceThreshold(), cooldownSince);
        }

        // 2. Saldo baixo
        if (Boolean.TRUE.equals(prefs.getLowBalanceEnabled())) {
            generateLowBalanceNotifications(userId, prefs, prefs.getLowBalanceThreshold(), cooldownSince);
        }

        // 3. Contas a vencer (despesas próximas)
        if (Boolean.TRUE.equals(prefs.getBillsDueEnabled())) {
            generateBillsDueNotifications(userId, prefs, cooldownSince);
        }

        // 4. Metas próximas do prazo
        if (Boolean.TRUE.equals(prefs.getGoalDueEnabled())) {
            generateGoalDueNotifications(userId, prefs, cooldownSince);
        }

        // 5. Regras de automação (ex.: saldo conta X < R$ 1000, alerta urgente)
        automationRuleProcessor.processAccountBalanceRules(userId);
    }

    private void generateBudgetExceededNotifications(UUID userId, NotificationPreferences prefs, BigDecimal threshold, LocalDateTime cooldownSince) {
        LocalDate now = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(now);
        LocalDate startMonth = currentMonth.atDay(1);
        LocalDate endMonth = currentMonth.atEndOfMonth();

        List<Budget> budgets = budgetRepository.findAllByUserIdAndMonthRange(userId, startMonth, endMonth);

        for (Budget budget : budgets) {
            if (budget.getDeletedAt() != null) continue;

            BigDecimal spent = transactionRepository.sumExpensesByCategoryAndDateRange(
                    userId, budget.getCategory().getId(), startMonth, endMonth);

            if (spent.compareTo(budget.getLimitAmount()) > 0) {
                if (!notificationRepository.existsRecentNotificationWithEntity(
                        userId, Notification.NotificationType.BUDGET_EXCEEDED.name(),
                        budget.getId(), cooldownSince)) {

                    String categoryName = budget.getCategory().getName();
                    createNotification(userId, prefs, Notification.NotificationType.BUDGET_EXCEEDED,
                            "Orçamento excedido",
                            String.format("O orçamento da categoria '%s' foi excedido. Limite: R$ %s / Gasto: R$ %s",
                                    categoryName, budget.getLimitAmount(), spent),
                            "Budget", budget.getId(),
                            Map.of("categoryName", categoryName, "limit", budget.getLimitAmount().toString(),
                                    "spent", spent.toString()));
                }
            }
        }
    }

    private void generateLowBalanceNotifications(UUID userId, NotificationPreferences prefs, BigDecimal threshold, LocalDateTime cooldownSince) {
        List<Account> accounts = accountRepository.findAllByUserId(userId);

        for (Account account : accounts) {
            if (account.getDeletedAt() != null) continue;
            if (account.getBalance().compareTo(threshold) < 0) {
                if (!notificationRepository.existsRecentNotificationWithEntity(
                        userId, Notification.NotificationType.LOW_BALANCE.name(),
                        account.getId(), cooldownSince)) {

                    createNotification(userId, prefs, Notification.NotificationType.LOW_BALANCE,
                            "Saldo baixo",
                            String.format("A conta '%s' está com saldo baixo: R$ %s (limite configurado: R$ %s)",
                                    account.getName(), account.getBalance(), threshold),
                            "Account", account.getId(),
                            Map.of("accountName", account.getName(), "balance", account.getBalance().toString()));
                }
            }
        }
    }

    private void generateBillsDueNotifications(UUID userId, NotificationPreferences prefs, LocalDateTime cooldownSince) {
        LocalDate now = LocalDate.now();
        LocalDate endDate = now.plusDays(BILLS_DUE_DAYS);

        List<Transaction> upcomingExpenses = transactionRepository.findUpcomingExpensesByUserId(userId, now, endDate);

        if (!upcomingExpenses.isEmpty() &&
                !notificationRepository.existsRecentNotificationWithNullEntity(
                        userId, Notification.NotificationType.BILLS_DUE.name(),
                        cooldownSince)) {

            BigDecimal total = upcomingExpenses.stream()
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            String firstDate = upcomingExpenses.get(0).getDate().toString();
            String lastDate = upcomingExpenses.get(upcomingExpenses.size() - 1).getDate().toString();

            createNotification(userId, prefs, Notification.NotificationType.BILLS_DUE,
                    "Despesas a vencer",
                    String.format("Você tem %d despesa(s) nos próximos %d dias (total: R$ %s). Período: %s a %s",
                            upcomingExpenses.size(), BILLS_DUE_DAYS, total, firstDate, lastDate),
                    "BillsDue", null,
                    Map.of("count", upcomingExpenses.size(), "total", total.toString()));
        }
    }

    private void generateGoalDueNotifications(UUID userId, NotificationPreferences prefs, LocalDateTime cooldownSince) {
        LocalDate now = LocalDate.now();
        LocalDate endDate = now.plusDays(GOAL_DUE_DAYS);

        List<Goal> goals = goalRepository.findAllActiveByUserIdAndDueDateBetween(userId, now, endDate);

        for (Goal goal : goals) {
            if (!notificationRepository.existsRecentNotificationWithEntity(
                    userId, Notification.NotificationType.GOAL_DUE_SOON.name(),
                    goal.getId(), cooldownSince)) {

                long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(now, goal.getDueDate());

                createNotification(userId, prefs, Notification.NotificationType.GOAL_DUE_SOON,
                        "Meta próxima do prazo",
                        String.format("A meta '%s' vence em %d dia(s). Valor atual: R$ %s / Meta: R$ %s",
                                goal.getName(), daysLeft, goal.getCurrentAmount(), goal.getTargetAmount()),
                        "Goal", goal.getId(),
                        Map.of("goalName", goal.getName(), "daysLeft", daysLeft,
                                "currentAmount", goal.getCurrentAmount().toString(),
                                "targetAmount", goal.getTargetAmount().toString()));
            }
        }
    }

    private void createNotification(UUID userId, NotificationPreferences prefs, Notification.NotificationType type,
                                    String title, String message, String entityType, UUID entityId,
                                    Map<String, Object> metadata) {
        User user = userRepository.findById(userId).orElseThrow();
        Notification notification = Notification.builder()
                .user(user)
                .type(type.name())
                .title(title)
                .message(message)
                .read(false)
                .entityType(entityType)
                .entityId(entityId)
                .metadata(metadata != null ? new HashMap<>(metadata) : null)
                .build();
        notificationRepository.save(notification);
        log.debug("Notification created: type={}, userId={}", type, userId);

        if (Boolean.TRUE.equals(prefs.getEmailEnabled())) {
            try {
                emailService.sendNotificationEmail(user.getEmail(), user.getName(), title, message);
                log.debug("Notification email sent: type={}, userId={}", type, userId);
            } catch (Exception e) {
                log.warn("Failed to send notification email: type={}, userId={}, error={}",
                        type, userId, e.getMessage());
            }
        }
    }

    private NotificationPreferences getOrCreatePreferences(UUID userId) {
        return preferencesRepository.findByUser_Id(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId).orElseThrow();
                    NotificationPreferences prefs = NotificationPreferences.builder()
                            .user(user)
                            .build();
                    return preferencesRepository.save(prefs);
                });
    }

    private NotificationPreferencesResponse toPreferencesResponse(NotificationPreferences prefs) {
        return NotificationPreferencesResponse.builder()
                .id(prefs.getId())
                .budgetExceededEnabled(prefs.getBudgetExceededEnabled())
                .lowBalanceEnabled(prefs.getLowBalanceEnabled())
                .billsDueEnabled(prefs.getBillsDueEnabled())
                .goalDueEnabled(prefs.getGoalDueEnabled())
                .emailEnabled(prefs.getEmailEnabled())
                .lowBalanceThreshold(prefs.getLowBalanceThreshold())
                .build();
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthenticatedException("Usuário não autenticado");
        }
        return UUID.fromString(auth.getName());
    }
}
