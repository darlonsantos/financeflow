package com.financeflow.accounts.service;

import com.financeflow.accounts.domain.Account;
import com.financeflow.accounts.domain.AccountShare;
import com.financeflow.accounts.dto.AccountShareRequest;
import com.financeflow.accounts.dto.AccountShareResponse;
import com.financeflow.accounts.exception.AccountNotFoundException;
import com.financeflow.accounts.repository.AccountRepository;
import com.financeflow.accounts.repository.AccountShareRepository;
import com.financeflow.audit.service.AuditService;
import com.financeflow.users.domain.User;
import com.financeflow.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountShareService {

    private final AccountShareRepository accountShareRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UUID)) {
            throw new RuntimeException("Usuário não autenticado");
        }
        return (UUID) authentication.getPrincipal();
    }

    /**
     * Verifica se o usuário atual é dono da conta ou tem compartilhamento com determinada permissão.
     */
    public boolean canAccess(UUID accountId, UUID userId, AccountShare.Permission required) {
        if (userId.equals(getCurrentUserId())) {
            return true;
        }
        return accountShareRepository.findByAccountIdAndSharedWithUserId(accountId, getCurrentUserId())
            .map(share -> required == null || share.getPermission() == AccountShare.Permission.EDIT || share.getPermission().ordinal() >= required.ordinal())
            .orElse(false);
    }

    /**
     * Retorna a permissão do usuário atual sobre a conta (null se não tiver acesso, EDIT/VIEW se for dono considera-se EDIT).
     */
    public AccountShare.Permission getPermission(UUID accountId, UUID accountOwnerId) {
        UUID current = getCurrentUserId();
        if (current.equals(accountOwnerId)) {
            return AccountShare.Permission.EDIT;
        }
        return accountShareRepository.findByAccountIdAndSharedWithUserId(accountId, current)
            .map(AccountShare::getPermission)
            .orElse(null);
    }

    @Transactional
    public AccountShareResponse shareAccount(UUID accountId, AccountShareRequest request) {
        UUID ownerId = getCurrentUserId();
        Account account = accountRepository.findByIdAndUserId(accountId, ownerId)
            .orElseThrow(() -> new AccountNotFoundException(accountId));

        User sharedWithUser = userRepository.findByEmail(request.getSharedWithEmail().trim().toLowerCase())
            .orElseThrow(() -> new IllegalArgumentException("Usuário com e-mail " + request.getSharedWithEmail() + " não encontrado"));

        if (sharedWithUser.getId().equals(ownerId)) {
            throw new IllegalArgumentException("Não é possível compartilhar a conta com você mesmo");
        }

        if (accountShareRepository.existsByAccountIdAndSharedWithUserId(accountId, sharedWithUser.getId())) {
            throw new IllegalArgumentException("Esta conta já está compartilhada com este usuário");
        }

        AccountShare share = AccountShare.builder()
            .account(account)
            .sharedWithUser(sharedWithUser)
            .permission(request.getPermission())
            .build();
        share = accountShareRepository.save(share);

        auditService.logAction(
            ownerId,
            "SHARE",
            "AccountShare",
            share.getId(),
            Map.of(
                "accountId", accountId.toString(),
                "accountName", account.getName(),
                "sharedWithUserId", sharedWithUser.getId().toString(),
                "sharedWithEmail", sharedWithUser.getEmail(),
                "permission", request.getPermission().name()
            )
        );

        return toResponse(share);
    }

    @Transactional(readOnly = true)
    public List<AccountShareResponse> listShares(UUID accountId) {
        UUID userId = getCurrentUserId();
        Account account = accountRepository.findByIdAndUserId(accountId, userId)
            .orElseThrow(() -> new AccountNotFoundException(accountId));

        return accountShareRepository.findByAccountIdWithUser(accountId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public AccountShareResponse updatePermission(UUID accountId, UUID sharedWithUserId, AccountShare.Permission permission) {
        UUID ownerId = getCurrentUserId();
        accountRepository.findByIdAndUserId(accountId, ownerId)
            .orElseThrow(() -> new AccountNotFoundException(accountId));

        AccountShare share = accountShareRepository.findByAccountIdAndSharedWithUserId(accountId, sharedWithUserId)
            .orElseThrow(() -> new IllegalArgumentException("Compartilhamento não encontrado"));

        AccountShare.Permission previous = share.getPermission();
        share.setPermission(permission);
        share = accountShareRepository.save(share);

        auditService.logAction(
            ownerId,
            "PERMISSION_CHANGE",
            "AccountShare",
            share.getId(),
            Map.of(
                "accountId", accountId.toString(),
                "sharedWithUserId", sharedWithUserId.toString(),
                "previousPermission", previous.name(),
                "newPermission", permission.name()
            )
        );

        return toResponse(share);
    }

    @Transactional
    public void revokeShare(UUID accountId, UUID sharedWithUserId) {
        UUID ownerId = getCurrentUserId();
        Account account = accountRepository.findByIdAndUserId(accountId, ownerId)
            .orElseThrow(() -> new AccountNotFoundException(accountId));

        AccountShare share = accountShareRepository.findByAccountIdAndSharedWithUserId(accountId, sharedWithUserId)
            .orElseThrow(() -> new IllegalArgumentException("Compartilhamento não encontrado"));

        accountShareRepository.delete(share);

        auditService.logAction(
            ownerId,
            "UNSHARE",
            "AccountShare",
            share.getId(),
            Map.of(
                "accountId", accountId.toString(),
                "accountName", account.getName(),
                "revokedUserId", sharedWithUserId.toString()
            )
        );
    }

    private AccountShareResponse toResponse(AccountShare share) {
        User u = share.getSharedWithUser();
        return AccountShareResponse.builder()
            .id(share.getId())
            .accountId(share.getAccount().getId())
            .sharedWithUserId(u.getId())
            .sharedWithUserName(u.getName())
            .sharedWithUserEmail(u.getEmail())
            .permission(share.getPermission())
            .createdAt(share.getCreatedAt())
            .build();
    }
}
