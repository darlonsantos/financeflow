package com.financeflow.installments.service;

import com.financeflow.accounts.domain.Account;
import com.financeflow.accounts.repository.AccountRepository;
import com.financeflow.accounts.repository.AccountShareRepository;
import com.financeflow.categories.domain.Category;
import com.financeflow.categories.repository.CategoryRepository;
import com.financeflow.installments.domain.InstallmentGroup;
import com.financeflow.installments.domain.InstallmentHistory;
import com.financeflow.installments.domain.InstallmentItem;
import com.financeflow.installments.dto.*;
import com.financeflow.installments.exception.InstallmentNotFoundException;
import com.financeflow.installments.mapper.InstallmentMapper;
import com.financeflow.installments.repository.InstallmentGroupRepository;
import com.financeflow.installments.repository.InstallmentHistoryRepository;
import com.financeflow.installments.repository.InstallmentItemRepository;
import com.financeflow.transactions.domain.Transaction;
import com.financeflow.transactions.dto.TransactionRequest;
import com.financeflow.transactions.dto.TransactionResponse;
import com.financeflow.transactions.repository.TransactionRepository;
import com.financeflow.transactions.service.TransactionService;
import com.financeflow.users.domain.User;
import com.financeflow.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import jakarta.persistence.criteria.Predicate;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstallmentService {

    private final InstallmentGroupRepository groupRepository;
    private final InstallmentItemRepository itemRepository;
    private final InstallmentHistoryRepository historyRepository;
    private final AccountRepository accountRepository;
    private final AccountShareRepository accountShareRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final InstallmentMapper mapper;
    private final TransactionService transactionService;
    private final TransactionRepository transactionRepository;

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UUID)) {
            throw new RuntimeException("Usuário não autenticado");
        }
        return (UUID) auth.getPrincipal();
    }

    private List<UUID> getAccessibleAccountIds(UUID userId) {
        List<UUID> ids = new ArrayList<>(accountRepository.findAllByUserId(userId).stream().map(Account::getId).toList());
        accountShareRepository.findBySharedWithUserId(userId).stream()
            .map(s -> s.getAccount().getId())
            .forEach(ids::add);
        return ids;
    }

    @Transactional(readOnly = true)
    public Page<InstallmentGroupResponse> findAll(Pageable pageable) {
        return findAll(pageable, null, null, null);
    }

    @Transactional(readOnly = true)
    public Page<InstallmentGroupResponse> findAll(
        Pageable pageable,
        String search,
        InstallmentGroup.InstallmentGroupStatus status,
        InstallmentGroup.InstallmentType installmentType
    ) {
        UUID userId = getCurrentUserId();
        String normalizedSearch = (search == null || search.isBlank()) ? null : "%" + search.trim() + "%";
        Specification<InstallmentGroup> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user").get("id"), userId));
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (installmentType != null) {
                predicates.add(cb.equal(root.get("installmentType"), installmentType));
            }
            if (normalizedSearch != null) {
                var accountJoin = root.join("account");
                var categoryJoin = root.join("category");
                predicates.add(
                    cb.or(
                        cb.like(cb.coalesce(root.get("description"), ""), normalizedSearch),
                        cb.like(accountJoin.get("name"), normalizedSearch),
                        cb.like(categoryJoin.get("name"), normalizedSearch)
                    )
                );
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return groupRepository.findAll(spec, pageable)
            .map(g -> mapper.toResponseWithItems(g));
    }

    @Transactional(readOnly = true)
    public InstallmentGroupResponse findById(UUID id) {
        UUID userId = getCurrentUserId();
        InstallmentGroup group = groupRepository.findByIdAndUserIdWithItems(id, userId)
            .orElseThrow(() -> new InstallmentNotFoundException(id));
        return mapper.toResponseWithItems(group);
    }

    @Transactional
    public InstallmentGroupResponse create(InstallmentGroupRequest request) {
        UUID userId = getCurrentUserId();
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        Account account = accountRepository.findByIdAccessibleByUser(request.getAccountId(), userId)
            .orElseThrow(() -> new RuntimeException("Conta não encontrada"));
        Category category = categoryRepository.findByIdAndUserId(request.getCategoryId(), userId)
            .orElseThrow(() -> new RuntimeException("Categoria não encontrada"));

        if (request.getInstallmentType() == InstallmentGroup.InstallmentType.VARIABLE) {
            if (request.getVariableAmounts() == null || request.getVariableAmounts().size() != request.getNumberOfInstallments()) {
                throw new IllegalArgumentException("Para parcelamento variável, informe exatamente " + request.getNumberOfInstallments() + " valores.");
            }
            boolean hasInvalidAmount = request.getVariableAmounts().stream()
                .anyMatch(amount -> amount == null || amount.compareTo(BigDecimal.ZERO) <= 0);
            if (hasInvalidAmount) {
                throw new IllegalArgumentException("Valores por parcela devem ser maiores que zero.");
            }
            BigDecimal sum = request.getVariableAmounts().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            if (sum.compareTo(request.getTotalAmount()) != 0) {
                throw new IllegalArgumentException("Soma dos valores das parcelas deve ser igual ao valor total.");
            }
        }

        InstallmentGroup group = InstallmentGroup.builder()
            .user(user)
            .account(account)
            .category(category)
            .description(request.getDescription())
            .totalAmount(request.getTotalAmount())
            .installmentType(request.getInstallmentType())
            .status(InstallmentGroup.InstallmentGroupStatus.ACTIVE)
            .firstDueDate(request.getFirstDueDate())
            .numberOfInstallments(request.getNumberOfInstallments())
            .build();

        group = groupRepository.save(group);

        List<InstallmentItem> items = new ArrayList<>();
        if (request.getInstallmentType() == InstallmentGroup.InstallmentType.FIXED) {
            BigDecimal amountPerInstallment = request.getTotalAmount()
                .divide(BigDecimal.valueOf(request.getNumberOfInstallments()), 2, RoundingMode.HALF_UP);
            BigDecimal remainder = request.getTotalAmount().subtract(amountPerInstallment.multiply(BigDecimal.valueOf(request.getNumberOfInstallments())));
            LocalDate due = request.getFirstDueDate();
            for (int i = 1; i <= request.getNumberOfInstallments(); i++) {
                BigDecimal amt = amountPerInstallment;
                if (i == request.getNumberOfInstallments() && remainder.compareTo(BigDecimal.ZERO) != 0) {
                    amt = amt.add(remainder);
                }
                items.add(InstallmentItem.builder()
                    .installmentGroup(group)
                    .installmentNumber(i)
                    .dueDate(due)
                    .amount(amt)
                    .status(InstallmentItem.InstallmentItemStatus.PENDING)
                    .build());
                due = due.plusMonths(1);
            }
        } else if (request.getInstallmentType() == InstallmentGroup.InstallmentType.VARIABLE) {
            LocalDate due = request.getFirstDueDate();
            for (int i = 0; i < request.getVariableAmounts().size(); i++) {
                items.add(InstallmentItem.builder()
                    .installmentGroup(group)
                    .installmentNumber(i + 1)
                    .dueDate(due.plusMonths(i))
                    .amount(request.getVariableAmounts().get(i))
                    .status(InstallmentItem.InstallmentItemStatus.PENDING)
                    .build());
            }
        } else {
            // RECURRING: same amount every month
            BigDecimal amountPerInstallment = request.getTotalAmount()
                .divide(BigDecimal.valueOf(request.getNumberOfInstallments()), 2, RoundingMode.HALF_UP);
            LocalDate due = request.getFirstDueDate();
            for (int i = 1; i <= request.getNumberOfInstallments(); i++) {
                items.add(InstallmentItem.builder()
                    .installmentGroup(group)
                    .installmentNumber(i)
                    .dueDate(due)
                    .amount(amountPerInstallment)
                    .status(InstallmentItem.InstallmentItemStatus.PENDING)
                    .build());
                due = due.plusMonths(1);
            }
        }

        for (InstallmentItem item : items) {
            itemRepository.save(item);
        }
        group.getItems().addAll(items);

        saveHistory(group.getId(), InstallmentHistory.HistoryAction.CREATED, Map.of(
            "totalAmount", request.getTotalAmount().toString(),
            "numberOfInstallments", request.getNumberOfInstallments(),
            "installmentType", request.getInstallmentType().name()
        ));

        return mapper.toResponseWithItems(groupRepository.findByIdAndUserIdWithItems(group.getId(), userId).orElse(group));
    }

    @Transactional
    public InstallmentGroupResponse payInstallment(PayInstallmentRequest request) {
        UUID userId = getCurrentUserId();
        LocalDate paymentDate = request.getPaymentDate() != null ? request.getPaymentDate() : LocalDate.now();

        InstallmentItem item = itemRepository.findByIdAndUserId(request.getInstallmentItemId(), userId)
            .orElseThrow(() -> new InstallmentNotFoundException(request.getInstallmentItemId()));

        if (item.getStatus() != InstallmentItem.InstallmentItemStatus.PENDING) {
            throw new IllegalArgumentException("Parcela já está paga ou cancelada.");
        }

        InstallmentGroup group = item.getInstallmentGroup();
        String desc = (group.getDescription() != null ? group.getDescription() : "Parcelamento") + " - Parcela " + item.getInstallmentNumber() + "/" + group.getNumberOfInstallments();

        TransactionRequest txRequest = TransactionRequest.builder()
            .accountId(group.getAccount().getId())
            .categoryId(group.getCategory().getId())
            .amount(item.getAmount())
            .type(Transaction.TransactionType.EXPENSE)
            .date(paymentDate)
            .description(desc)
            .recurring(false)
            .build();

        TransactionResponse tx = transactionService.create(txRequest);

        item.setStatus(InstallmentItem.InstallmentItemStatus.PAID);
        Transaction transaction = transactionRepository.findById(tx.getId()).orElseThrow();
        item.setTransaction(transaction);
        item.setPaidAt(LocalDateTime.now());
        itemRepository.save(item);

        saveHistory(group.getId(), InstallmentHistory.HistoryAction.PAY_INSTALLMENT, Map.of(
            "installmentItemId", item.getId().toString(),
            "installmentNumber", item.getInstallmentNumber(),
            "amount", item.getAmount().toString(),
            "transactionId", tx.getId().toString(),
            "paymentDate", paymentDate.toString()
        ));

        long pendingCount = group.getItems().stream().filter(i -> i.getStatus() == InstallmentItem.InstallmentItemStatus.PENDING).count();
        if (pendingCount == 0) {
            group.setStatus(InstallmentGroup.InstallmentGroupStatus.PAID_OFF);
            groupRepository.save(group);
        }

        return findById(group.getId());
    }

    @Transactional
    public InstallmentGroupResponse earlySettlement(EarlySettlementRequest request) {
        UUID userId = getCurrentUserId();
        LocalDate settlementDate = request.getSettlementDate() != null ? request.getSettlementDate() : LocalDate.now();

        InstallmentGroup group = groupRepository.findByIdAndUserIdWithItems(request.getInstallmentGroupId(), userId)
            .orElseThrow(() -> new InstallmentNotFoundException(request.getInstallmentGroupId()));

        if (group.getStatus() != InstallmentGroup.InstallmentGroupStatus.ACTIVE) {
            throw new IllegalArgumentException("Parcelamento não está ativo.");
        }

        BigDecimal remaining = BigDecimal.ZERO;
        List<InstallmentItem> pending = new ArrayList<>();
        for (InstallmentItem i : group.getItems()) {
            if (i.getStatus() == InstallmentItem.InstallmentItemStatus.PENDING) {
                remaining = remaining.add(i.getAmount());
                pending.add(i);
            }
        }

        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Não há parcelas pendentes para quitar.");
        }

        String desc = (group.getDescription() != null ? group.getDescription() : "Parcelamento") + " - Quitação antecipada";

        TransactionRequest txRequest = TransactionRequest.builder()
            .accountId(group.getAccount().getId())
            .categoryId(group.getCategory().getId())
            .amount(remaining)
            .type(Transaction.TransactionType.EXPENSE)
            .date(settlementDate)
            .description(desc)
            .recurring(false)
            .build();

        TransactionResponse tx = transactionService.create(txRequest);

        for (InstallmentItem i : pending) {
            i.setStatus(InstallmentItem.InstallmentItemStatus.PAID);
            i.setPaidAt(LocalDateTime.now());
            itemRepository.save(i);
        }

        group.setStatus(InstallmentGroup.InstallmentGroupStatus.PAID_OFF);
        groupRepository.save(group);

        saveHistory(group.getId(), InstallmentHistory.HistoryAction.EARLY_SETTLEMENT, Map.of(
            "amount", remaining.toString(),
            "transactionId", tx.getId().toString(),
            "settlementDate", settlementDate.toString(),
            "installmentsSettled", pending.size()
        ));

        return findById(group.getId());
    }

    @Transactional
    public InstallmentGroupResponse renegotiate(RenegotiateRequest request) {
        UUID userId = getCurrentUserId();

        InstallmentGroup group = groupRepository.findByIdAndUserIdWithItems(request.getInstallmentGroupId(), userId)
            .orElseThrow(() -> new InstallmentNotFoundException(request.getInstallmentGroupId()));

        if (group.getStatus() != InstallmentGroup.InstallmentGroupStatus.ACTIVE) {
            throw new IllegalArgumentException("Apenas parcelamentos ativos podem ser renegociados.");
        }

        List<InstallmentItem> pending = group.getItems().stream()
            .filter(i -> i.getStatus() == InstallmentItem.InstallmentItemStatus.PENDING)
            .toList();

        if (pending.isEmpty()) {
            throw new IllegalArgumentException("Não há parcelas pendentes para renegociar.");
        }

        BigDecimal paidSoFar = group.getItems().stream()
            .filter(i -> i.getStatus() == InstallmentItem.InstallmentItemStatus.PAID)
            .map(InstallmentItem::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Renegociação: cancelar parcelas pendentes e criar novas com novo total (apenas o saldo devedor)
        BigDecimal newTotal = request.getNewTotalAmount();
        LocalDate newFirstDue = request.getNewFirstDueDate();
        int newCount = request.getNewNumberOfInstallments();
        InstallmentGroup.InstallmentType newType = request.getNewInstallmentType() != null ? request.getNewInstallmentType() : group.getInstallmentType();

        if (request.getNewVariableAmounts() != null && newType == InstallmentGroup.InstallmentType.VARIABLE) {
            if (request.getNewVariableAmounts().size() != newCount) {
                throw new IllegalArgumentException("Quantidade de valores deve ser igual ao número de parcelas.");
            }
            boolean hasInvalidAmount = request.getNewVariableAmounts().stream()
                .anyMatch(amount -> amount == null || amount.compareTo(BigDecimal.ZERO) <= 0);
            if (hasInvalidAmount) {
                throw new IllegalArgumentException("Valores por parcela devem ser maiores que zero.");
            }
            BigDecimal sum = request.getNewVariableAmounts().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            if (sum.compareTo(newTotal) != 0) {
                throw new IllegalArgumentException("Soma dos valores deve ser igual ao novo total.");
            }
        }

        for (InstallmentItem i : pending) {
            i.setStatus(InstallmentItem.InstallmentItemStatus.CANCELLED);
            itemRepository.save(i);
        }

        group.setTotalAmount(newTotal);
        group.setFirstDueDate(newFirstDue);
        group.setNumberOfInstallments(newCount);
        group.setInstallmentType(newType);
        group.getItems().removeIf(i -> i.getStatus() == InstallmentItem.InstallmentItemStatus.CANCELLED);
        groupRepository.save(group);

        List<InstallmentItem> newItems = new ArrayList<>();
        if (newType == InstallmentGroup.InstallmentType.FIXED) {
            BigDecimal amountPer = newTotal.divide(BigDecimal.valueOf(newCount), 2, RoundingMode.HALF_UP);
            BigDecimal remainder = newTotal.subtract(amountPer.multiply(BigDecimal.valueOf(newCount)));
            LocalDate due = newFirstDue;
            for (int i = 1; i <= newCount; i++) {
                BigDecimal amt = (i == newCount && remainder.compareTo(BigDecimal.ZERO) != 0) ? amountPer.add(remainder) : amountPer;
                InstallmentItem newItem = InstallmentItem.builder()
                    .installmentGroup(group)
                    .installmentNumber(i)
                    .dueDate(due)
                    .amount(amt)
                    .status(InstallmentItem.InstallmentItemStatus.PENDING)
                    .build();
                newItems.add(itemRepository.save(newItem));
                due = due.plusMonths(1);
            }
        } else if (newType == InstallmentGroup.InstallmentType.VARIABLE && request.getNewVariableAmounts() != null) {
            LocalDate due = newFirstDue;
            for (int i = 0; i < request.getNewVariableAmounts().size(); i++) {
                InstallmentItem newItem = InstallmentItem.builder()
                    .installmentGroup(group)
                    .installmentNumber(i + 1)
                    .dueDate(due.plusMonths(i))
                    .amount(request.getNewVariableAmounts().get(i))
                    .status(InstallmentItem.InstallmentItemStatus.PENDING)
                    .build();
                newItems.add(itemRepository.save(newItem));
            }
        } else {
            BigDecimal amountPer = newTotal.divide(BigDecimal.valueOf(newCount), 2, RoundingMode.HALF_UP);
            LocalDate due = newFirstDue;
            for (int i = 1; i <= newCount; i++) {
                InstallmentItem newItem = InstallmentItem.builder()
                    .installmentGroup(group)
                    .installmentNumber(i)
                    .dueDate(due)
                    .amount(amountPer)
                    .status(InstallmentItem.InstallmentItemStatus.PENDING)
                    .build();
                newItems.add(itemRepository.save(newItem));
                due = due.plusMonths(1);
            }
        }
        group.getItems().addAll(newItems);

        saveHistory(group.getId(), InstallmentHistory.HistoryAction.RENEGOTIATION, Map.of(
            "previousPaidAmount", paidSoFar.toString(),
            "newTotalAmount", newTotal.toString(),
            "newNumberOfInstallments", newCount,
            "newFirstDueDate", newFirstDue.toString(),
            "newInstallmentType", newType.name()
        ));

        return findById(group.getId());
    }

    @Transactional
    public void cancel(UUID groupId) {
        UUID userId = getCurrentUserId();
        InstallmentGroup group = groupRepository.findByIdAndUserIdWithItems(groupId, userId)
            .orElseThrow(() -> new InstallmentNotFoundException(groupId));

        if (group.getStatus() != InstallmentGroup.InstallmentGroupStatus.ACTIVE) {
            throw new IllegalArgumentException("Apenas parcelamentos ativos podem ser cancelados.");
        }

        for (InstallmentItem i : group.getItems()) {
            if (i.getStatus() == InstallmentItem.InstallmentItemStatus.PENDING) {
                i.setStatus(InstallmentItem.InstallmentItemStatus.CANCELLED);
                itemRepository.save(i);
            }
        }
        group.setStatus(InstallmentGroup.InstallmentGroupStatus.CANCELLED);
        groupRepository.save(group);

        saveHistory(group.getId(), InstallmentHistory.HistoryAction.CANCELLED, Map.of("cancelledAt", LocalDateTime.now().toString()));
    }

    private void saveHistory(UUID groupId, InstallmentHistory.HistoryAction action, Map<String, Object> details) {
        historyRepository.save(InstallmentHistory.builder()
            .installmentGroupId(groupId)
            .action(action)
            .details(details)
            .build());
    }

    /** Retorna parcelas pendentes por período (para projeções e indicadores). */
    @Transactional(readOnly = true)
    public List<InstallmentItem> findPendingByUserIdAndDueDateBetween(LocalDate from, LocalDate to) {
        UUID userId = getCurrentUserId();
        return itemRepository.findPendingByUserIdAndDueDateBetween(userId, from, to);
    }

    /**
     * Monta resumo dos parcelamentos ativos para o contexto do assistente IA.
     * Usado pelo AssistantContextBuilder para incluir parcelas nas respostas.
     */
    @Transactional(readOnly = true)
    public String buildInstallmentSummaryForContext(UUID userId) {
        List<InstallmentGroup> groups = groupRepository.findAllActiveByUserId(userId);
        if (groups.isEmpty()) {
            return "### Parcelamentos ativos\n- Nenhum parcelamento ativo no momento.\n\n";
        }
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR"));
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"));
        StringBuilder sb = new StringBuilder();
        sb.append("### Parcelamentos ativos (compras parceladas, financiamentos)\n");
        BigDecimal totalRemaining = BigDecimal.ZERO;
        for (InstallmentGroup g : groups) {
            List<InstallmentItem> items = itemRepository.findByInstallmentGroupIdOrderByInstallmentNumberAsc(g.getId());
            BigDecimal remaining = items.stream()
                .filter(i -> i.getStatus() == InstallmentItem.InstallmentItemStatus.PENDING)
                .map(InstallmentItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            long paidCount = items.stream().filter(i -> i.getStatus() == InstallmentItem.InstallmentItemStatus.PAID).count();
            long pendingCount = items.stream().filter(i -> i.getStatus() == InstallmentItem.InstallmentItemStatus.PENDING).count();
            LocalDate nextDue = items.stream()
                .filter(i -> i.getStatus() == InstallmentItem.InstallmentItemStatus.PENDING)
                .map(InstallmentItem::getDueDate)
                .min(LocalDate::compareTo)
                .orElse(null);
            totalRemaining = totalRemaining.add(remaining);
            String desc = g.getDescription() != null && !g.getDescription().isBlank() ? g.getDescription() : g.getCategory().getName();
            sb.append("- ").append(desc)
                .append(" | Restante R$ ").append(nf.format(remaining))
                .append(" | Parcelas ").append(paidCount).append("/").append(g.getNumberOfInstallments())
                .append(" (").append(pendingCount).append(" pendentes)");
            if (nextDue != null) {
                sb.append(" | Próximo vencimento ").append(nextDue.format(df));
            }
            sb.append("\n");
        }
        sb.append("- TOTAL em parcelamentos (saldo devedor): R$ ").append(nf.format(totalRemaining)).append("\n\n");
        return sb.toString();
    }
}
