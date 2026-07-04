package com.financeflow.goals.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalContributeRequest {

    @NotNull(message = "Valor da contribuição é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor da contribuição deve ser maior que zero")
    private BigDecimal amount;
}
