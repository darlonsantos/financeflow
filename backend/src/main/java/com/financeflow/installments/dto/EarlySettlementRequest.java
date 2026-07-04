package com.financeflow.installments.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarlySettlementRequest {

    @NotNull(message = "ID do grupo de parcelas é obrigatório")
    private UUID installmentGroupId;

    /** Data efetiva da quitação */
    private LocalDate settlementDate;
}
