package com.financeflow.installments.dto;

import com.financeflow.installments.domain.InstallmentGroup;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.FutureOrPresent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenegotiateRequest {

    @NotNull(message = "ID do grupo de parcelas é obrigatório")
    private UUID installmentGroupId;

    @NotNull(message = "Novo valor total é obrigatório")
    @Positive(message = "Valor deve ser positivo")
    private BigDecimal newTotalAmount;

    @NotNull(message = "Nova data da primeira parcela é obrigatória")
    @FutureOrPresent(message = "Nova data da primeira parcela não pode ser no passado")
    private LocalDate newFirstDueDate;

    @NotNull(message = "Número de parcelas é obrigatório")
    @Min(value = 1, message = "Número de parcelas deve ser pelo menos 1")
    @Max(value = 360, message = "Número de parcelas deve ser no máximo 360")
    private Integer newNumberOfInstallments;

    /** Para VARIABLE: novos valores por parcela */
    private List<BigDecimal> newVariableAmounts;

    /** Novo tipo (opcional; mantém se null) */
    private InstallmentGroup.InstallmentType newInstallmentType;
}
