package com.financeflow.budgets.service;

import com.financeflow.audit.service.AuditService;
import com.financeflow.budgets.domain.Budget;
import com.financeflow.budgets.dto.BudgetRequest;
import com.financeflow.budgets.dto.BudgetResponse;
import com.financeflow.budgets.exception.BudgetNotFoundException;
import com.financeflow.budgets.repository.BudgetRepository;
import com.financeflow.categories.domain.Category;
import com.financeflow.categories.repository.CategoryRepository;
import com.financeflow.transactions.repository.TransactionRepository;
import com.financeflow.users.domain.User;
import com.financeflow.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final AuditService auditService;

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UUID)) {
            throw new RuntimeException("Usuário não autenticado");
        }
        return (UUID) authentication.getPrincipal();
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> findAll(LocalDate month) {
        UUID userId = getCurrentUserId();
        List<Budget> budgets;

        if (month != null) {
            YearMonth ym = YearMonth.from(month);
            LocalDate startMonth = ym.atDay(1);
            LocalDate endMonth = ym.atEndOfMonth();
            budgets = budgetRepository.findAllByUserIdAndMonthRange(userId, startMonth, endMonth);
        } else {
            budgets = budgetRepository.findAllByUserId(userId);
        }

        return budgets.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BudgetResponse findById(UUID id) {
        UUID userId = getCurrentUserId();
        Budget budget = budgetRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new BudgetNotFoundException(id));
        return toResponse(budget);
    }

    @Transactional
    public BudgetResponse create(BudgetRequest request) {
        UUID userId = getCurrentUserId();
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        Category category = categoryRepository.findByIdAndUserId(request.getCategoryId(), userId)
            .orElseThrow(() -> new RuntimeException("Categoria não encontrada"));

        if (category.getType() != Category.CategoryType.EXPENSE) {
            throw new IllegalArgumentException("Orçamento só pode ser criado para categorias de despesa");
        }

        LocalDate monthStart = request.getMonth().withDayOfMonth(1);

        if (budgetRepository.existsByUser_IdAndCategory_IdAndMonthAndDeletedAtIsNull(
            userId,
            request.getCategoryId(),
            monthStart
        )) {
            throw new IllegalArgumentException("Já existe um orçamento para esta categoria neste mês.");
        }

        Budget budget = Budget.builder()
            .user(user)
            .category(category)
            .month(monthStart)
            .limitAmount(request.getLimitAmount())
            .spentAmount(BigDecimal.ZERO)
            .build();

        budget = budgetRepository.save(budget);

        auditService.logAction(userId, "CREATE", "Budget", budget.getId(), Map.of(
            "categoryId", category.getId().toString(),
            "month", monthStart.toString(),
            "limitAmount", request.getLimitAmount().toString()
        ));

        return toResponse(budget);
    }

    @Transactional
    public BudgetResponse update(UUID id, BudgetRequest request) {
        UUID userId = getCurrentUserId();
        Budget budget = budgetRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new BudgetNotFoundException(id));
        Category category = categoryRepository.findByIdAndUserId(request.getCategoryId(), userId)
            .orElseThrow(() -> new RuntimeException("Categoria não encontrada"));

        if (category.getType() != Category.CategoryType.EXPENSE) {
            throw new IllegalArgumentException("Orçamento só pode ser vinculado a categorias de despesa");
        }

        LocalDate monthStart = request.getMonth().withDayOfMonth(1);

        if (budgetRepository.existsByUser_IdAndCategory_IdAndMonthAndDeletedAtIsNullAndIdNot(
            userId,
            request.getCategoryId(),
            monthStart,
            id
        )) {
            throw new IllegalArgumentException("Já existe um orçamento para esta categoria neste mês.");
        }

        budget.setCategory(category);
        budget.setMonth(monthStart);
        budget.setLimitAmount(request.getLimitAmount());
        budget = budgetRepository.save(budget);

        auditService.logAction(userId, "UPDATE", "Budget", budget.getId(), Map.of(
            "categoryId", category.getId().toString(),
            "month", monthStart.toString(),
            "limitAmount", request.getLimitAmount().toString()
        ));

        return toResponse(budget);
    }

    @Transactional
    public void delete(UUID id) {
        UUID userId = getCurrentUserId();
        Budget budget = budgetRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new BudgetNotFoundException(id));

        budget.setDeletedAt(java.time.LocalDateTime.now());
        budgetRepository.save(budget);

        auditService.logAction(userId, "DELETE", "Budget", budget.getId(), Map.of(
            "categoryId", budget.getCategory().getId().toString(),
            "month", budget.getMonth().toString()
        ));
    }

    private BudgetResponse toResponse(Budget budget) {
        YearMonth ym = YearMonth.from(budget.getMonth());
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        BigDecimal spent = transactionRepository.sumExpensesByCategoryAndDateRange(
            budget.getUser().getId(),
            budget.getCategory().getId(),
            startDate,
            endDate
        );

        BigDecimal percentUsed = budget.getLimitAmount().compareTo(BigDecimal.ZERO) > 0
            ? spent.multiply(BigDecimal.valueOf(100)).divide(budget.getLimitAmount(), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        return BudgetResponse.builder()
            .id(budget.getId())
            .categoryId(budget.getCategory().getId())
            .categoryName(budget.getCategory().getName())
            .categoryColor(budget.getCategory().getColor())
            .month(budget.getMonth())
            .limitAmount(budget.getLimitAmount())
            .spentAmount(spent)
            .percentUsed(percentUsed)
            .createdAt(budget.getCreatedAt())
            .updatedAt(budget.getUpdatedAt())
            .build();
    }
}
