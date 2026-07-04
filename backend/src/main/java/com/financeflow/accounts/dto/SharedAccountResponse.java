package com.financeflow.accounts.dto;

import com.financeflow.accounts.domain.Account;
import com.financeflow.accounts.domain.AccountShare;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resposta de conta quando listada para o usuário, incluindo se é compartilhada e permissão.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedAccountResponse {

    private UUID id;
    private String name;
    private Account.AccountType type;
    private BigDecimal balance;
    private BigDecimal initialBalance;
    private String color;
    private String icon;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Se true, a conta foi compartilhada com o usuário atual (não é dono). */
    private boolean sharedWithMe;
    /** Permissão quando sharedWithMe é true: VIEW ou EDIT. */
    private AccountShare.Permission sharedPermission;
    /** Nome do dono da conta quando sharedWithMe é true. */
    private String ownerName;
}
