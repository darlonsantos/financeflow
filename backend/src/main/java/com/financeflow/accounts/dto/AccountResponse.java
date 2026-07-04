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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {
    
    private UUID id;
    private String name;
    private Account.AccountType type;
    private BigDecimal balance;
    private BigDecimal initialBalance;
    private String color;
    private String icon;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Se true, a conta foi compartilhada com o usuário atual. */
    private Boolean sharedWithMe;
    /** Permissão quando sharedWithMe é true. */
    private AccountShare.Permission sharedPermission;
    /** Nome do dono da conta quando sharedWithMe é true. */
    private String ownerName;

    /** Código da moeda da conta (ex: BRL, USD). Valores (balance, initialBalance) estão nesta moeda. */
    private String currencyCode;

    /**
     * @deprecated Use {@link com.financeflow.accounts.mapper.AccountMapper#toResponse(Account)} instead
     */
    @Deprecated
    public static AccountResponse fromEntity(Account account) {
        return AccountResponse.builder()
            .id(account.getId())
            .name(account.getName())
            .type(account.getType())
            .balance(account.getBalance())
            .initialBalance(account.getInitialBalance())
            .color(account.getColor())
            .icon(account.getIcon())
            .createdAt(account.getCreatedAt())
            .updatedAt(account.getUpdatedAt())
            .currencyCode(account.getCurrencyCode())
            .build();
    }
}
