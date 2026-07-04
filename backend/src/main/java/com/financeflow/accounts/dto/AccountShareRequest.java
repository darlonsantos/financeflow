package com.financeflow.accounts.dto;

import com.financeflow.accounts.domain.AccountShare;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountShareRequest {

    @NotBlank(message = "E-mail do usuário é obrigatório")
    @Email(message = "E-mail inválido")
    private String sharedWithEmail;

    @NotNull(message = "Permissão é obrigatória")
    private AccountShare.Permission permission;
}
