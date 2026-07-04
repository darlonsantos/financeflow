package com.financeflow.transactions.service;

import com.financeflow.automation.service.AutomationRuleProcessor;
import com.financeflow.accounts.calculator.AccountBalanceCalculator;
import com.financeflow.accounts.domain.Account;
import com.financeflow.accounts.domain.AccountShare;
import com.financeflow.accounts.repository.AccountRepository;
import com.financeflow.accounts.repository.AccountShareRepository;
import com.financeflow.audit.service.AuditService;
import com.financeflow.categories.domain.Category;
import com.financeflow.gamification.service.GamificationService;
import com.financeflow.categories.repository.CategoryRepository;
import com.financeflow.transactions.domain.Transaction;
import com.financeflow.transactions.dto.TransactionRequest;
import com.financeflow.transactions.dto.TransactionResponse;
import com.financeflow.transactions.exception.TransactionNotFoundException;
import com.financeflow.transactions.mapper.TransactionMapper;
import com.financeflow.transactions.repository.TransactionRepository;
import com.financeflow.transactions.validator.TransactionValidator;
import com.financeflow.users.domain.User;
import com.financeflow.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.hibernate.StaleObjectStateException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService implements ITransactionService {
    
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final AccountShareRepository accountShareRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionMapper transactionMapper;
    private final TransactionValidator transactionValidator;
    private final AccountBalanceCalculator balanceCalculator;
    private final AuditService auditService;
    private final GamificationService gamificationService;
    private final AutomationRuleProcessor automationRuleProcessor;
    
    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UUID)) {
            throw new RuntimeException("Usuário não autenticado");
        }
        return (UUID) authentication.getPrincipal();
    }
    
    @Transactional(readOnly = true)
    public Page<TransactionResponse> findAll(Pageable pageable) {
        UUID userId = getCurrentUserId();
        List<UUID> accessibleAccountIds = getAccessibleAccountIds(userId);
        if (accessibleAccountIds.isEmpty()) {
            return org.springframework.data.domain.Page.empty(pageable);
        }
        return transactionRepository.findAllByAccountIdIn(accessibleAccountIds, pageable)
            .map(transactionMapper::toResponse);
    }

    private List<UUID> getAccessibleAccountIds(UUID userId) {
        List<UUID> ids = new ArrayList<>(accountRepository.findAllByUserId(userId).stream().map(Account::getId).toList());
        accountShareRepository.findBySharedWithUserId(userId).stream()
            .map(s -> s.getAccount().getId())
            .forEach(ids::add);
        return ids;
    }

    private boolean canEditTransaction(Transaction t, UUID userId) {
        if (t.getUser().getId().equals(userId)) return true;
        return accountShareRepository.findByAccountIdAndSharedWithUserId(t.getAccount().getId(), userId)
            .map(s -> s.getPermission() == AccountShare.Permission.EDIT)
            .orElse(false);
    }
    
    @Transactional(readOnly = true)
    public Page<TransactionResponse> findByAccount(UUID accountId, Pageable pageable) {
        UUID userId = getCurrentUserId();
        if (accountRepository.findByIdAccessibleByUser(accountId, userId).isEmpty()) {
            throw new RuntimeException("Conta não encontrada");
        }
        return transactionRepository.findAllByAccountId(accountId, pageable)
            .map(transactionMapper::toResponse);
    }
    
    @Transactional(readOnly = true)
    public Page<TransactionResponse> findByCategory(UUID categoryId, Pageable pageable) {
        UUID userId = getCurrentUserId();
        // Verificar se a categoria pertence ao usuário
        if (!categoryRepository.existsByIdAndUserId(categoryId, userId)) {
            throw new RuntimeException("Categoria não encontrada");
        }
        return transactionRepository.findAllByUserIdAndCategoryId(userId, categoryId, pageable)
            .map(transactionMapper::toResponse);
    }
    
    @Transactional(readOnly = true)
    public Page<TransactionResponse> findByType(Transaction.TransactionType type, Pageable pageable) {
        UUID userId = getCurrentUserId();
        return transactionRepository.findAllByUserIdAndType(userId, type, pageable)
            .map(transactionMapper::toResponse);
    }
    
    @Transactional(readOnly = true)
    public Page<TransactionResponse> findByDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        UUID userId = getCurrentUserId();
        return transactionRepository.findAllByUserIdAndDateBetween(userId, startDate, endDate, pageable)
            .map(transactionMapper::toResponse);
    }
    
    @Transactional(readOnly = true)
    public List<TransactionResponse> findAllForReport(UUID accountId, UUID categoryId, Transaction.TransactionType type, LocalDate dateFrom, LocalDate dateTo) {
        UUID userId = getCurrentUserId();
        
        // Validar se a conta pertence ao usuário (se fornecida)
        if (accountId != null && !accountRepository.existsByIdAndUserId(accountId, userId)) {
            throw new RuntimeException("Conta não encontrada");
        }
        
        // Validar se a categoria pertence ao usuário (se fornecida)
        if (categoryId != null && !categoryRepository.existsByIdAndUserId(categoryId, userId)) {
            throw new RuntimeException("Categoria não encontrada");
        }
        
        List<Transaction> transactions;
        
        // Construir query baseada nos filtros fornecidos
        if (dateFrom != null && dateTo != null) {
            transactions = transactionRepository.findAllByUserIdAndDateRangeForReport(userId, dateFrom, dateTo);
            // Aplicar filtros adicionais em memória se necessário
            if (accountId != null) {
                transactions = transactions.stream()
                    .filter(t -> t.getAccount().getId().equals(accountId))
                    .toList();
            }
            if (categoryId != null) {
                transactions = transactions.stream()
                    .filter(t -> t.getCategory().getId().equals(categoryId))
                    .toList();
            }
            if (type != null) {
                transactions = transactions.stream()
                    .filter(t -> t.getType() == type)
                    .toList();
            }
        } else if (accountId != null) {
            transactions = transactionRepository.findAllByUserIdAndAccountIdForReport(userId, accountId);
            if (categoryId != null) {
                transactions = transactions.stream()
                    .filter(t -> t.getCategory().getId().equals(categoryId))
                    .toList();
            }
            if (type != null) {
                transactions = transactions.stream()
                    .filter(t -> t.getType() == type)
                    .toList();
            }
        } else if (categoryId != null) {
            transactions = transactionRepository.findAllByUserIdAndCategoryIdForReport(userId, categoryId);
            if (type != null) {
                transactions = transactions.stream()
                    .filter(t -> t.getType() == type)
                    .toList();
            }
        } else if (type != null) {
            transactions = transactionRepository.findAllByUserIdAndTypeForReport(userId, type);
        } else {
            transactions = transactionRepository.findAllByUserIdForReport(userId);
        }
        
        return transactionMapper.toResponseList(transactions);
    }
    
    @Transactional(readOnly = true)
    public TransactionResponse findById(UUID id) {
        UUID userId = getCurrentUserId();
        Transaction transaction = transactionRepository.findById(id)
            .filter(t -> t.getDeletedAt() == null)
            .filter(t -> t.getUser().getId().equals(userId)
                || accountShareRepository.existsByAccountIdAndSharedWithUserId(t.getAccount().getId(), userId))
            .orElseThrow(() -> new TransactionNotFoundException(id));
        return transactionMapper.toResponse(transaction);
    }
    
    @Transactional
    public TransactionResponse create(TransactionRequest request) {
        UUID userId = getCurrentUserId();
        
        // Adicionar contexto ao MDC
        MDC.put("operation", "createTransaction");
        MDC.put("accountId", request.getAccountId().toString());
        MDC.put("categoryId", request.getCategoryId().toString());
        MDC.put("amount", request.getAmount().toString());
        MDC.put("type", request.getType().name());
        
        log.info("Creating transaction", Map.of(
            "accountId", request.getAccountId(),
            "categoryId", request.getCategoryId(),
            "amount", request.getAmount(),
            "type", request.getType()
        ));
        
        try {
            // Idempotência para sincronização offline: se clientId foi enviado e já existe, retornar a existente
            if (request.getClientId() != null && !request.getClientId().isBlank()) {
                Optional<Transaction> existing = transactionRepository.findByClientIdAndUserId(request.getClientId(), userId);
                if (existing.isPresent()) {
                    log.info("Transaction already exists for clientId (idempotent), returning existing", Map.of("clientId", request.getClientId()));
                    return transactionMapper.toResponse(existing.get());
                }
            }

            Account account = accountRepository.findByIdAccessibleByUser(request.getAccountId(), userId)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada"));
            boolean isSharedEdit = !account.getUser().getId().equals(userId)
                && accountShareRepository.findByAccountIdAndSharedWithUserId(request.getAccountId(), userId)
                    .map(s -> s.getPermission() == AccountShare.Permission.EDIT)
                    .orElse(false);
            if (!account.getUser().getId().equals(userId) && !isSharedEdit) {
                throw new RuntimeException("Você não tem permissão para criar transações nesta conta");
            }
            User transactionUser = account.getUser();
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
            
            Category category = categoryRepository.findByIdAndUserId(request.getCategoryId(), userId)
                .orElseThrow(() -> new RuntimeException("Categoria não encontrada"));
            
            // Validar transação usando validator
            transactionValidator.validate(request, category);
            transactionValidator.validateAmount(request.getAmount());
            
            Transaction transaction = Transaction.builder()
                .user(transactionUser)
                .account(account)
                .category(category)
                .amount(request.getAmount())
                .type(request.getType())
                .date(request.getDate())
                .dueDate(request.getDueDate())
                .description(request.getDescription())
                .tags(request.getTags())
                .recurring(request.getRecurring())
                .recurringPattern(request.getRecurringPattern())
                .clientId(request.getClientId())
                .build();
            
            transaction = transactionRepository.save(transaction);
            
            // Se for uma transação recorrente, criar transações futuras
            if (transaction.getRecurring() && transaction.getRecurringPattern() != null) {
                createRecurringTransactions(transaction);
            } else if (transaction.getRecurring() && transaction.getRecurringPattern() == null) {
                // Se for recorrente mas não tiver padrão definido, usar padrão mensal
                transaction.setRecurringPattern("MONTHLY");
                transaction = transactionRepository.save(transaction);
                createRecurringTransactions(transaction);
            }
            
            // Atualizar saldo da conta usando calculator
            BigDecimal balanceChange = balanceCalculator.calculateBalanceChange(
                transaction.getAmount(), 
                transaction.getType(), 
                true
            );
            balanceCalculator.updateBalance(account, balanceChange);
            accountRepository.save(account);
            
            MDC.put("transactionId", transaction.getId().toString());
            log.info("Transaction created successfully", Map.of(
                "transactionId", transaction.getId(),
                "accountId", account.getId(),
                "newBalance", account.getBalance()
            ));
            
            // Auditoria
            auditService.logAction(
                userId,
                "CREATE",
                "Transaction",
                transaction.getId(),
                Map.of(
                    "amount", transaction.getAmount().toString(),
                    "type", transaction.getType().name(),
                    "accountId", account.getId().toString(),
                    "categoryId", category.getId().toString(),
                    "date", transaction.getDate().toString()
                )
            );

            // Atualizar streak de gamificação
            gamificationService.updateStreakAfterTransaction(userId);

            // Regras de automação: ex.: se categoria = X e valor > Y, marcar como revisar
            try {
                automationRuleProcessor.processTransactionRules(userId, transaction);
            } catch (Exception e) {
                log.warn("Automation rules processing failed for transaction {}", transaction.getId(), e);
            }
            
            return transactionMapper.toResponse(transaction);
        } catch (Exception e) {
            log.error("Error creating transaction", Map.of(
                "error", e.getMessage(),
                "accountId", request.getAccountId()
            ), e);
            throw e;
        } finally {
            // Limpar contexto específico da operação
            MDC.remove("operation");
            MDC.remove("accountId");
            MDC.remove("categoryId");
            MDC.remove("amount");
            MDC.remove("type");
            MDC.remove("transactionId");
        }
    }
    
    @Transactional
    public TransactionResponse update(UUID id, TransactionRequest request) {
        UUID userId = getCurrentUserId();
        
        MDC.put("operation", "updateTransaction");
        MDC.put("transactionId", id.toString());
        
        log.info("Updating transaction", Map.of(
            "transactionId", id,
            "accountId", request.getAccountId()
        ));
        
        try {
            Transaction transaction = transactionRepository.findById(id)
                .filter(t -> t.getDeletedAt() == null)
                .filter(t -> canEditTransaction(t, userId))
                .orElseThrow(() -> new TransactionNotFoundException(id));
            
            UUID oldAccountId = transaction.getAccount().getId();
            BigDecimal oldAmount = transaction.getAmount();
            Transaction.TransactionType oldType = transaction.getType();
            
            Account oldAccount = accountRepository.findByIdAccessibleByUser(oldAccountId, userId)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada"));
            
            BigDecimal oldBalanceChange = balanceCalculator.calculateBalanceChange(oldAmount, oldType, false);
            balanceCalculator.updateBalance(oldAccount, oldBalanceChange);
            accountRepository.save(oldAccount);
            
            Account newAccount = oldAccount;
            if (!request.getAccountId().equals(oldAccountId)) {
                newAccount = accountRepository.findByIdAccessibleByUser(request.getAccountId(), userId)
                    .orElseThrow(() -> new RuntimeException("Conta não encontrada"));
            }
            
            Category category = categoryRepository.findByIdAndUserId(request.getCategoryId(), userId)
                .orElseThrow(() -> new RuntimeException("Categoria não encontrada"));
            
            // Validar transação usando validator
            transactionValidator.validate(request, category);
            transactionValidator.validateAmount(request.getAmount());
            
            transaction.setAccount(newAccount);
            transaction.setCategory(category);
            transaction.setAmount(request.getAmount());
            transaction.setType(request.getType());
            transaction.setDate(request.getDate());
            transaction.setDueDate(request.getDueDate());
            transaction.setDescription(request.getDescription());
            transaction.setTags(request.getTags());
            transaction.setRecurring(request.getRecurring());
            transaction.setRecurringPattern(request.getRecurringPattern());
            
            transaction = transactionRepository.save(transaction);
            
            // Atualizar saldo da nova conta usando calculator
            BigDecimal newBalanceChange = balanceCalculator.calculateBalanceChange(
                transaction.getAmount(), 
                transaction.getType(), 
                true
            );
            balanceCalculator.updateBalance(newAccount, newBalanceChange);
            accountRepository.save(newAccount);
            
            log.info("Transaction updated successfully", Map.of(
                "transactionId", id,
                "newBalance", newAccount.getBalance()
            ));
            
            // Auditoria
            auditService.logAction(
                userId,
                "UPDATE",
                "Transaction",
                id,
                Map.of(
                    "oldAmount", oldAmount.toString(),
                    "newAmount", transaction.getAmount().toString(),
                    "oldType", oldType.name(),
                    "newType", transaction.getType().name(),
                    "oldAccountId", oldAccountId.toString(),
                    "newAccountId", newAccount.getId().toString()
                )
            );

            // Regras de automação
            try {
                automationRuleProcessor.processTransactionRules(userId, transaction);
            } catch (Exception e) {
                log.warn("Automation rules processing failed for transaction {}", id, e);
            }
            
            return transactionMapper.toResponse(transaction);
        } catch (Exception e) {
            log.error("Error updating transaction", Map.of(
                "transactionId", id,
                "error", e.getMessage()
            ), e);
            throw e;
        } finally {
            MDC.remove("operation");
            MDC.remove("transactionId");
        }
    }
    
    @Transactional
    public void delete(UUID id) {
        UUID userId = getCurrentUserId();
        
        MDC.put("operation", "deleteTransaction");
        MDC.put("transactionId", id.toString());
        
        log.info("Deleting transaction", Map.of("transactionId", id));
        
        try {
            Transaction transaction = transactionRepository.findById(id)
                .filter(t -> t.getDeletedAt() == null)
                .filter(t -> canEditTransaction(t, userId))
                .orElseThrow(() -> new TransactionNotFoundException(id));
            
            UUID accountId = transaction.getAccount().getId();
            Account account = accountRepository.findByIdAccessibleByUser(accountId, userId)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada"));
            
            // Reverter o saldo da conta usando calculator
            BigDecimal balanceChange = balanceCalculator.calculateBalanceChange(
                transaction.getAmount(), 
                transaction.getType(), 
                false
            );
            balanceCalculator.updateBalance(account, balanceChange);
            accountRepository.save(account);
            
            transaction.setDeletedAt(java.time.LocalDateTime.now());
            transactionRepository.save(transaction);
            
            log.info("Transaction deleted successfully", Map.of(
                "transactionId", id,
                "accountId", accountId
            ));
            
            // Auditoria
            auditService.logAction(
                userId,
                "DELETE",
                "Transaction",
                id,
                Map.of(
                    "amount", transaction.getAmount().toString(),
                    "type", transaction.getType().name(),
                    "accountId", accountId.toString()
                )
            );
        } catch (Exception e) {
            log.error("Error deleting transaction", Map.of(
                "transactionId", id,
                "error", e.getMessage()
            ), e);
            throw e;
        } finally {
            MDC.remove("operation");
            MDC.remove("transactionId");
        }
    }
    
    @Transactional
    public void deleteBatch(List<UUID> ids) {
        UUID userId = getCurrentUserId();
        
        MDC.put("operation", "deleteBatchTransactions");
        MDC.put("transactionCount", String.valueOf(ids.size()));
        
        log.info("Deleting batch of transactions", Map.of("count", ids.size()));
        
        if (ids == null || ids.isEmpty()) {
            log.warn("Empty list of transaction IDs provided for batch delete");
            return;
        }
        
        // Remover duplicatas
        List<UUID> uniqueIds = ids.stream().distinct().toList();
        if (uniqueIds.size() != ids.size()) {
            log.warn("Duplicate IDs found and removed", Map.of(
                "original", ids.size(),
                "unique", uniqueIds.size()
            ));
        }
        
        try {
            // Buscar todas as transações de uma vez com suas contas carregadas
            List<Transaction> transactions = transactionRepository.findAllByIdsAndUserId(uniqueIds, userId);
            
            if (transactions.isEmpty()) {
                log.warn("No transactions found for the provided IDs");
                return;
            }
            
            if (transactions.size() != uniqueIds.size()) {
                log.warn("Some transactions were not found or don't belong to user", Map.of(
                    "requested", uniqueIds.size(),
                    "found", transactions.size()
                ));
            }
            
            // Filtrar transações já deletadas
            List<Transaction> activeTransactions = transactions.stream()
                .filter(t -> t.getDeletedAt() == null)
                .toList();
            
            if (activeTransactions.isEmpty()) {
                log.warn("All transactions are already deleted");
                return;
            }
            
            // Processar cada transação individualmente para garantir que falhas não comprometam o lote
            int successCount = 0;
            int failureCount = 0;
            List<UUID> failedIds = new java.util.ArrayList<>();
            LocalDateTime now = java.time.LocalDateTime.now();
            
            // Agrupar por conta para otimizar atualizações de saldo
            Map<UUID, List<Transaction>> transactionsByAccount = activeTransactions.stream()
                .collect(java.util.stream.Collectors.groupingBy(t -> t.getAccount().getId()));
            
            // Processar cada conta
            for (Map.Entry<UUID, List<Transaction>> accountEntry : transactionsByAccount.entrySet()) {
                UUID accountId = accountEntry.getKey();
                List<Transaction> accountTransactions = accountEntry.getValue();
                
                try {
                    // Recarregar conta com versão mais recente (otimistic locking)
                    Account account = accountRepository.findByIdAndUserId(accountId, userId)
                        .orElseThrow(() -> new RuntimeException("Conta não encontrada: " + accountId));
                    
                    // Calcular mudança total de saldo para esta conta
                    BigDecimal totalBalanceChange = accountTransactions.stream()
                        .map(t -> balanceCalculator.calculateBalanceChange(
                            t.getAmount(),
                            t.getType(),
                            false // false = reverter (deletar)
                        ))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    // Aplicar mudança de saldo
                    balanceCalculator.updateBalance(account, totalBalanceChange);
                    accountRepository.save(account);
                    
                    log.debug("Updated account balance", Map.of(
                        "accountId", accountId,
                        "balanceChange", totalBalanceChange,
                        "newBalance", account.getBalance(),
                        "transactionsCount", accountTransactions.size()
                    ));
                    
                    // Marcar transações como deletadas
                    for (Transaction transaction : accountTransactions) {
                        try {
                            // Verificar novamente se não foi deletada por outra operação
                            Transaction freshTransaction = transactionRepository.findByIdAndUserId(
                                transaction.getId(), userId
                            ).orElse(null);
                            
                            if (freshTransaction != null && freshTransaction.getDeletedAt() == null) {
                                freshTransaction.setDeletedAt(now);
                                transactionRepository.save(freshTransaction);
                                
                                // Auditoria
                                auditService.logAction(
                                    userId,
                                    "DELETE",
                                    "Transaction",
                                    freshTransaction.getId(),
                                    Map.of(
                                        "amount", freshTransaction.getAmount().toString(),
                                        "type", freshTransaction.getType().name(),
                                        "accountId", accountId.toString(),
                                        "batchDelete", true
                                    )
                                );
                                
                                successCount++;
                            } else {
                                log.debug("Transaction already deleted, skipping", Map.of(
                                    "transactionId", transaction.getId()
                                ));
                            }
                        } catch (Exception e) {
                            failureCount++;
                            failedIds.add(transaction.getId());
                            log.error("Error deleting transaction in batch", Map.of(
                                "transactionId", transaction.getId(),
                                "accountId", accountId,
                                "error", e.getMessage()
                            ), e);
                            // Continuar com próxima transação
                        }
                    }
                } catch (StaleObjectStateException e) {
                    // Conflito de optimistic locking - tentar novamente para cada transação individualmente
                    log.warn("Optimistic locking conflict for account, processing transactions individually", Map.of(
                        "accountId", accountId
                    ));
                    
                    for (Transaction transaction : accountTransactions) {
                        try {
                            deleteTransactionWithRetry(transaction.getId(), userId, 3);
                            successCount++;
                        } catch (Exception retryException) {
                            failureCount++;
                            failedIds.add(transaction.getId());
                            log.error("Error deleting transaction after retry", Map.of(
                                "transactionId", transaction.getId(),
                                "error", retryException.getMessage()
                            ), retryException);
                        }
                    }
                } catch (Exception e) {
                    // Falha ao atualizar conta - processar transações individualmente
                    log.error("Error updating account balance, processing transactions individually", Map.of(
                        "accountId", accountId,
                        "error", e.getMessage()
                    ), e);
                    
                    for (Transaction transaction : accountTransactions) {
                        try {
                            deleteTransactionWithRetry(transaction.getId(), userId, 3);
                            successCount++;
                        } catch (Exception retryException) {
                            failureCount++;
                            failedIds.add(transaction.getId());
                            log.error("Error deleting transaction individually", Map.of(
                                "transactionId", transaction.getId(),
                                "error", retryException.getMessage()
                            ), retryException);
                        }
                    }
                }
            }
            
            log.info("Batch delete completed", Map.of(
                "successCount", successCount,
                "failureCount", failureCount,
                "totalRequested", uniqueIds.size(),
                "failedIds", failedIds
            ));
            
            if (failureCount > 0) {
                throw new RuntimeException(String.format(
                    "Batch delete completed with %d failures out of %d transactions. Failed IDs: %s",
                    failureCount, uniqueIds.size(), failedIds
                ));
            }
        } catch (Exception e) {
            log.error("Error deleting batch of transactions", Map.of(
                "count", ids.size(),
                "error", e.getMessage()
            ), e);
            throw e;
        } finally {
            MDC.remove("operation");
            MDC.remove("transactionCount");
        }
    }
    
    /**
     * Deleta uma transação com retry para lidar com optimistic locking
     */
    private void deleteTransactionWithRetry(UUID transactionId, UUID userId, int maxRetries) {
        int attempts = 0;
        while (attempts < maxRetries) {
            try {
                Transaction transaction = transactionRepository.findByIdAndUserId(transactionId, userId)
                    .orElseThrow(() -> new TransactionNotFoundException(transactionId));
                
                if (transaction.getDeletedAt() != null) {
                    return; // Já deletada
                }
                
                UUID accountId = transaction.getAccount().getId();
                Account account = accountRepository.findByIdAndUserId(accountId, userId)
                    .orElseThrow(() -> new RuntimeException("Conta não encontrada"));
                
                BigDecimal balanceChange = balanceCalculator.calculateBalanceChange(
                    transaction.getAmount(),
                    transaction.getType(),
                    false
                );
                
                balanceCalculator.updateBalance(account, balanceChange);
                accountRepository.save(account);
                
                transaction.setDeletedAt(java.time.LocalDateTime.now());
                transactionRepository.save(transaction);
                
                auditService.logAction(
                    userId,
                    "DELETE",
                    "Transaction",
                    transactionId,
                    Map.of(
                        "amount", transaction.getAmount().toString(),
                        "type", transaction.getType().name(),
                        "accountId", accountId.toString(),
                        "batchDelete", true,
                        "retryAttempt", attempts + 1
                    )
                );
                
                return; // Sucesso
            } catch (StaleObjectStateException e) {
                attempts++;
                if (attempts >= maxRetries) {
                    throw new RuntimeException("Failed to delete transaction after " + maxRetries + " retries", e);
                }
                // Aguardar um pouco antes de tentar novamente
                try {
                    Thread.sleep(50 * attempts); // Backoff exponencial simples
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry", ie);
                }
            }
        }
    }
    
    @Transactional
    public void recalculateAccountBalance(UUID accountId) {
        UUID userId = getCurrentUserId();
        Account account = accountRepository.findByIdAndUserId(accountId, userId)
            .orElseThrow(() -> new RuntimeException("Conta não encontrada"));
        
        // Buscar todas as transações da conta
        List<Transaction> transactions = transactionRepository.findAllByAccountId(accountId);
        
        // Recalcular saldo usando calculator
        BigDecimal newBalance = balanceCalculator.recalculateBalance(account, transactions);
        account.setBalance(newBalance);
        accountRepository.save(account);
    }
    
    /**
     * Cria transações futuras baseadas no padrão de recorrência
     * Cria uma transação para cada mês futuro (até 12 meses)
     */
    private void createRecurringTransactions(Transaction originalTransaction) {
        try {
            // Garantir que temos acesso às relações antes de usar
            Account account = originalTransaction.getAccount();
            Category category = originalTransaction.getCategory();
            User user = originalTransaction.getUser();
            
            if (account == null || category == null || user == null) {
                throw new RuntimeException("Relações não carregadas para transação: " + originalTransaction.getId());
            }
            
            LocalDate originalDate = originalTransaction.getDate();
            
            // Calcular a próxima data baseada no padrão
            LocalDate nextDate = calculateNextDate(originalDate, originalTransaction.getRecurringPattern());
            
            // Criar transações para os próximos 12 meses (uma por mês)
            int createdCount = 0;
            int maxIterations = 12; // Limite de segurança para evitar loop infinito
            int iteration = 0;
            
            // Continuar criando enquanto a data estiver no futuro e não ultrapassar 12 meses
            LocalDate endDate = originalDate.plusMonths(12);
            
            while ((nextDate.isBefore(endDate) || nextDate.isEqual(endDate)) && iteration < maxIterations) {
                // Verificar se já existe uma transação para esta data específica
                // (evitar duplicatas se o método for chamado múltiplas vezes)
                long existingCount = transactionRepository.countMatchingTransactions(
                    user.getId(),
                    account.getId(),
                    category.getId(),
                    originalTransaction.getAmount(),
                    originalTransaction.getType(),
                    nextDate,
                    nextDate,
                    originalTransaction.getDescription()
                );
                
                if (existingCount == 0) {
                    // Criar nova transação para a data futura
                    Transaction recurringTransaction = Transaction.builder()
                        .user(user)
                        .account(account)
                        .category(category)
                        .amount(originalTransaction.getAmount())
                        .type(originalTransaction.getType())
                        .date(nextDate)
                        .description(originalTransaction.getDescription())
                        .tags(originalTransaction.getTags())
                        .recurring(false) // As transações futuras não são marcadas como recorrentes
                        .recurringPattern(null) // Apenas a original mantém o padrão
                        .build();
                    
                    recurringTransaction = transactionRepository.save(recurringTransaction);
                    createdCount++;
                    
                    // Atualizar saldo da conta para cada transação futura
                    BigDecimal balanceChange = balanceCalculator.calculateBalanceChange(
                        recurringTransaction.getAmount(),
                        recurringTransaction.getType(),
                        true
                    );
                    balanceCalculator.updateBalance(account, balanceChange);
                }
                
                iteration++;
                
                // Calcular próxima data
                LocalDate previousDate = nextDate;
                nextDate = calculateNextDate(nextDate, originalTransaction.getRecurringPattern());
                
                // Verificação de segurança: se a data não avançou, sair do loop
                if (!nextDate.isAfter(previousDate)) {
                    log.warn("Date did not advance, stopping loop", Map.of(
                        "previousDate", previousDate,
                        "nextDate", nextDate,
                        "pattern", originalTransaction.getRecurringPattern()
                    ));
                    break;
                }
            }
            
            accountRepository.save(account);
            
            log.info("Created {} recurring transactions (one per month)", Map.of(
                "originalTransactionId", originalTransaction.getId(),
                "createdCount", createdCount,
                "pattern", originalTransaction.getRecurringPattern()
            ));
        } catch (Exception e) {
            log.error("Error creating recurring transactions", Map.of(
                "originalTransactionId", originalTransaction.getId(),
                "error", e.getMessage()
            ), e);
            throw e;
        }
    }
    
    /**
     * Calcula a próxima data baseada no padrão de recorrência
     */
    private LocalDate calculateNextDate(LocalDate currentDate, String pattern) {
        if (pattern == null) {
            return currentDate.plusMonths(1); // Padrão: mensal
        }
        
        return switch (pattern.toUpperCase()) {
            case "DAILY" -> currentDate.plusDays(1);
            case "WEEKLY" -> currentDate.plusWeeks(1);
            case "MONTHLY" -> currentDate.plusMonths(1);
            case "YEARLY" -> currentDate.plusYears(1);
            default -> currentDate.plusMonths(1); // Padrão: mensal
        };
    }
    
    /**
     * Processa todas as transações recorrentes existentes e cria suas transações futuras
     * Útil para processar transações recorrentes criadas antes da implementação desta funcionalidade
     */
    @Transactional
    public void processRecurringTransactions() {
        UUID userId = getCurrentUserId();
        
        MDC.put("operation", "processRecurringTransactions");
        log.info("Processing recurring transactions");
        
        try {
            // Buscar todas as transações recorrentes do usuário
            List<Transaction> recurringTransactions = transactionRepository.findAllRecurringByUserId(userId);
            
            log.info("Found {} recurring transactions to process", Map.of("count", recurringTransactions.size()));
            
            if (recurringTransactions.isEmpty()) {
                log.info("No recurring transactions found to process");
                return;
            }
            
            int processedCount = 0;
            int createdCount = 0;
            
            for (Transaction recurringTransaction : recurringTransactions) {
                try {
                    log.debug("Processing recurring transaction", Map.of("transactionId", recurringTransaction.getId()));
                    
                    // Recarregar a transação com todas as relações usando findByIdAndUserId
                    // Isso garante que todas as relações lazy sejam carregadas
                    Transaction transaction = transactionRepository.findByIdAndUserId(
                        recurringTransaction.getId(), userId
                    ).orElseThrow(() -> new RuntimeException("Transação não encontrada: " + recurringTransaction.getId()));
                    
                    // Garantir que as relações estão carregadas acessando-as
                    UUID accountId = transaction.getAccount().getId();
                    UUID categoryId = transaction.getCategory().getId();
                    BigDecimal amount = transaction.getAmount();
                    Transaction.TransactionType type = transaction.getType();
                    String description = transaction.getDescription();
                    LocalDate originalDate = transaction.getDate();
                    String pattern = transaction.getRecurringPattern();
                    
                    log.debug("Transaction details", Map.of(
                        "accountId", accountId,
                        "categoryId", categoryId,
                        "amount", amount,
                        "pattern", pattern != null ? pattern : "null"
                    ));
                    
                    // Se não tiver padrão definido, definir como mensal
                    if (pattern == null || pattern.isEmpty()) {
                        transaction.setRecurringPattern("MONTHLY");
                        transaction = transactionRepository.save(transaction);
                        pattern = "MONTHLY";
                        // Recarregar novamente após salvar para garantir que as relações estão disponíveis
                        transaction = transactionRepository.findByIdAndUserId(
                            transaction.getId(), userId
                        ).orElseThrow(() -> new RuntimeException("Erro ao recarregar transação após salvar"));
                    }
                    
                    // Verificar se já existem transações futuras para esta transação recorrente
                    // Verificar se há transações futuras suficientes (pelo menos 11 meses futuros)
                    LocalDate futureDate = originalDate.plusDays(1); // Começar do dia seguinte
                    LocalDate endDate = originalDate.plusMonths(12); // Até 12 meses no futuro
                    
                    // Usar uma query otimizada que conta diretamente no banco
                    long matchingTransactions = transactionRepository.countMatchingTransactions(
                        userId, accountId, categoryId, amount, type, futureDate, endDate, description
                    );
                    
                    log.debug("Found {} matching future transactions", Map.of("count", matchingTransactions));
                    
                    // Se não há transações futuras suficientes (menos de 11 meses), criar novas
                    // Criamos para 12 meses, então esperamos pelo menos 11 transações futuras
                    if (matchingTransactions < 11) {
                        log.info("Creating recurring transactions for transaction {} (one per month)", Map.of("transactionId", transaction.getId()));
                        createRecurringTransactions(transaction);
                        createdCount++;
                    } else {
                        log.debug("Recurring transactions already exist for all future months, skipping", Map.of("transactionId", transaction.getId()));
                    }
                    
                    processedCount++;
                } catch (Exception e) {
                    log.error("Error processing recurring transaction", Map.of(
                        "transactionId", recurringTransaction != null ? recurringTransaction.getId() : "unknown",
                        "error", e.getMessage(),
                        "errorClass", e.getClass().getSimpleName()
                    ), e);
                    // Continuar processando outras transações mesmo se uma falhar
                }
            }
            
            log.info("Processed {} recurring transactions, created future transactions for {}", 
                Map.of(
                    "processedCount", processedCount,
                    "createdCount", createdCount
                ));
        } catch (Exception e) {
            log.error("Error processing recurring transactions", Map.of(
                "error", e.getMessage()
            ), e);
            throw e;
        } finally {
            MDC.remove("operation");
        }
    }
}
