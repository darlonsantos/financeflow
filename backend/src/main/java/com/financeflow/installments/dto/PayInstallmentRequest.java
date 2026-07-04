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
public class PayInstallmentRequest {

    @NotNull(message = "ID da parcela é obrigatório")
    private UUID installmentItemId;

    /** Data efetiva do pagamento (default: hoje) */
    private LocalDate paymentDate;
}
