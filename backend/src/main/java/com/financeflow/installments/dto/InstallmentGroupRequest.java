package com.financeflow.installments.dto;

import com.financeflow.installments.domain.InstallmentGroup;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
public class InstallmentGroupRequest {

    @NotNull(message = "Conta é obrigatória")
    private UUID accountId;

    @NotNull(message = "Categoria é obrigatória")
    private UUID categoryId;

    @Size(max = 500)
    private String description;

    @NotNull(message = "Valor total é obrigatório")
    @Positive(message = "Valor total deve ser positivo")
    private BigDecimal totalAmount;

    @NotNull(message = "Tipo de parcelamento é obrigatório")
    private InstallmentGroup.InstallmentType installmentType;

    @NotNull(message = "Data da primeira parcela é obrigatória")
    @FutureOrPresent(message = "Data da primeira parcela não pode ser no passado")
    private LocalDate firstDueDate;

    @NotNull(message = "Número de parcelas é obrigatório")
    @Min(value = 1, message = "Número de parcelas deve ser pelo menos 1")
    @Max(value = 360, message = "Número de parcelas deve ser no máximo 360")
    private Integer numberOfInstallments;

    /**
     * Para VARIABLE: lista de valores por parcela (ordem = parcela 1, 2, 3...).
     * Soma deve ser igual a totalAmount. Tamanho deve ser numberOfInstallments.
     */
    private List<BigDecimal> variableAmounts;
}
