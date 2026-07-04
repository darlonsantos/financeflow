package com.financeflow.accounts.dto;

import com.financeflow.accounts.domain.Account;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
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
public class AccountRequest {
    
    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
    private String name;
    
    @NotNull(message = "Tipo é obrigatório")
    private Account.AccountType type;
    
    @NotNull(message = "Saldo inicial é obrigatório")
    @PositiveOrZero(message = "Saldo inicial deve ser positivo ou zero")
    private BigDecimal initialBalance;
    
    @Size(max = 7, message = "Cor deve ter no máximo 7 caracteres")
    private String color;
    
    @Size(max = 50, message = "Ícone deve ter no máximo 50 caracteres")
    private String icon;

    @Size(min = 3, max = 3, message = "Código da moeda deve ter 3 caracteres (ex: BRL, USD)")
    private String currencyCode;
}
