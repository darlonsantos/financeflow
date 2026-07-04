package com.financeflow.budgets.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
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
public class BudgetRequest {

    @NotNull(message = "Categoria é obrigatória")
    private UUID categoryId;

    @NotNull(message = "Mês é obrigatório")
    private LocalDate month;

    @NotNull(message = "Limite é obrigatório")
    @DecimalMin(value = "0.01", message = "Limite deve ser maior que zero")
    private BigDecimal limitAmount;
}
