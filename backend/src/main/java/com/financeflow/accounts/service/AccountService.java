package com.financeflow.accounts.service;

import com.financeflow.accounts.calculator.AccountBalanceCalculator;
import com.financeflow.accounts.domain.Account;
import com.financeflow.accounts.domain.AccountShare;
import com.financeflow.accounts.dto.AccountRequest;
import com.financeflow.accounts.dto.AccountResponse;
import com.financeflow.accounts.exception.AccountNotFoundException;
import com.financeflow.accounts.mapper.AccountMapper;
import com.financeflow.accounts.repository.AccountRepository;
import com.financeflow.accounts.repository.AccountShareRepository;
import com.financeflow.accounts.validator.AccountValidator;
import com.financeflow.audit.service.AuditService;
import com.financeflow.users.domain.User;
import com.financeflow.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService implements IAccountService {
    
    private final AccountRepository accountRepository;
    private final AccountShareRepository accountShareRepository;
    private final UserRepository userRepository;
    private final AccountMapper accountMapper;
    private final AccountValidator accountValidator;
    private final AccountBalanceCalculator balanceCalculator;
    private final AuditService auditService;
    
    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UUID)) {
            throw new RuntimeException("Usuário não autenticado");
        }
        return (UUID) authentication.getPrincipal();
    }
    
    @Transactional(readOnly = true)
    public List<AccountResponse> findAll() {
        UUID userId = getCurrentUserId();
        List<AccountResponse> result = new ArrayList<>();
        List<Account> myAccounts = accountRepository.findAllByUserId(userId);
        for (Account a : myAccounts) {
            AccountResponse r = accountMapper.toResponse(a);
            r.setSharedWithMe(false);
            result.add(r);
        }
        List<AccountShare> sharedWithMe = accountShareRepository.findBySharedWithUserId(userId);
        for (AccountShare share : sharedWithMe) {
            Account a = share.getAccount();
            AccountResponse r = accountMapper.toResponse(a);
            r.setSharedWithMe(true);
            r.setSharedPermission(share.getPermission());
            r.setOwnerName(a.getUser().getName());
            result.add(r);
        }
        return result;
    }
    
    @Transactional(readOnly = true)
    public AccountResponse findById(UUID id) {
        UUID userId = getCurrentUserId();
        Account account = accountRepository.findByIdAccessibleByUser(id, userId)
            .orElseThrow(() -> new AccountNotFoundException(id));
        AccountResponse r = accountMapper.toResponse(account);
        if (!account.getUser().getId().equals(userId)) {
            r.setSharedWithMe(true);
            accountShareRepository.findByAccountIdAndSharedWithUserId(id, userId)
                .ifPresent(s -> r.setSharedPermission(s.getPermission()));
            r.setOwnerName(account.getUser().getName());
        } else {
            r.setSharedWithMe(false);
        }
        return r;
    }
    
    @Transactional
    public AccountResponse create(AccountRequest request) {
        UUID userId = getCurrentUserId();
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        accountValidator.validateName(request.getName());
        accountValidator.validateInitialBalance(request.getInitialBalance());
        String currencyCode = request.getCurrencyCode() != null && !request.getCurrencyCode().isBlank()
            ? request.getCurrencyCode().toUpperCase().trim()
            : "BRL";

        Account account = Account.builder()
            .user(user)
            .name(request.getName())
            .type(request.getType())
            .initialBalance(request.getInitialBalance())
            .balance(request.getInitialBalance())
            .color(request.getColor())
            .icon(request.getIcon())
            .currencyCode(currencyCode)
            .build();
        
        account = accountRepository.save(account);
        
        // Auditoria
        auditService.logAction(
            userId,
            "CREATE",
            "Account",
            account.getId(),
            Map.of(
                "name", account.getName(),
                "type", account.getType().name(),
                "initialBalance", account.getInitialBalance().toString()
            )
        );
        
        return accountMapper.toResponse(account);
    }
    
    @Transactional
    public AccountResponse update(UUID id, AccountRequest request) {
        UUID userId = getCurrentUserId();
        Account account = accountRepository.findByIdAccessibleByUser(id, userId)
            .orElseThrow(() -> new AccountNotFoundException(id));
        boolean isOwner = account.getUser().getId().equals(userId);
        if (!isOwner) {
            boolean canEdit = accountShareRepository.findByAccountIdAndSharedWithUserId(id, userId)
                .map(s -> s.getPermission() == AccountShare.Permission.EDIT)
                .orElse(false);
            if (!canEdit) {
                throw new IllegalArgumentException("Você não tem permissão para editar esta conta");
            }
        }
        
        // Validar usando validator
        accountValidator.validateName(request.getName());
        accountValidator.validateInitialBalance(request.getInitialBalance());
        
        // Recalcular saldo se o saldo inicial mudou usando calculator
        if (request.getInitialBalance().compareTo(account.getInitialBalance()) != 0) {
            java.math.BigDecimal balanceDifference = balanceCalculator.calculateInitialBalanceDifference(
                account.getInitialBalance(), 
                request.getInitialBalance()
            );
            account.setBalance(account.getBalance().add(balanceDifference));
        }
        
        account.setName(request.getName());
        account.setType(request.getType());
        account.setInitialBalance(request.getInitialBalance());
        account.setColor(request.getColor());
        account.setIcon(request.getIcon());
        if (request.getCurrencyCode() != null && !request.getCurrencyCode().isBlank()) {
            account.setCurrencyCode(request.getCurrencyCode().toUpperCase().trim());
        }
        account = accountRepository.save(account);
        
        // Auditoria (quem executou a ação)
        auditService.logAction(
            userId,
            "UPDATE",
            "Account",
            id,
            Map.of(
                "name", account.getName(),
                "type", account.getType().name(),
                "initialBalance", account.getInitialBalance().toString(),
                "bySharedUser", !isOwner
            )
        );
        
        AccountResponse r = accountMapper.toResponse(account);
        if (!isOwner) {
            r.setSharedWithMe(true);
            r.setSharedPermission(AccountShare.Permission.EDIT);
            r.setOwnerName(account.getUser().getName());
        }
        return r;
    }
    
    @Transactional
    public void delete(UUID id) {
        UUID userId = getCurrentUserId();
        Account account = accountRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new AccountNotFoundException(id));
        
        // TODO: Validar se tem transações ativas quando implementar
        // accountValidator.validateCanDelete(account, hasActiveTransactions);
        
        account.setDeletedAt(java.time.LocalDateTime.now());
        accountRepository.save(account);
        
        // Auditoria
        auditService.logAction(
            userId,
            "DELETE",
            "Account",
            id,
            Map.of(
                "name", account.getName(),
                "type", account.getType().name()
            )
        );
    }
    
    @Transactional(readOnly = true)
    public AccountResponse getBalance(UUID id) {
        UUID userId = getCurrentUserId();
        Account account = accountRepository.findByIdAccessibleByUser(id, userId)
            .orElseThrow(() -> new AccountNotFoundException(id));
        AccountResponse r = accountMapper.toResponse(account);
        if (!account.getUser().getId().equals(userId)) {
            r.setSharedWithMe(true);
            accountShareRepository.findByAccountIdAndSharedWithUserId(id, userId)
                .ifPresent(s -> r.setSharedPermission(s.getPermission()));
            r.setOwnerName(account.getUser().getName());
        }
        return r;
    }
}
