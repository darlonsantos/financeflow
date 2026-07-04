package com.financeflow.transfers.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {

    @NotNull(message = "Conta de origem é obrigatória")
    private UUID originAccountId;

    @NotNull(message = "Conta de destino é obrigatória")
    private UUID destinationAccountId;

    @NotNull(message = "Data da transferência é obrigatória")
    private LocalDate transferDate;

    @Size(max = 1000, message = "Motivo deve ter no máximo 1000 caracteres")
    private String description;

    @NotNull(message = "Valor é obrigatório")
    @Positive(message = "Valor deve ser positivo")
    private BigDecimal amount;
}
