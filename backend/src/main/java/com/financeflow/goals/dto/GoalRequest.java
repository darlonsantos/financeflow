package com.financeflow.goals.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
    private String name;

    @jakarta.validation.constraints.NotNull(message = "Valor alvo é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor alvo deve ser maior que zero")
    private BigDecimal targetAmount;

    private LocalDate dueDate;
}
