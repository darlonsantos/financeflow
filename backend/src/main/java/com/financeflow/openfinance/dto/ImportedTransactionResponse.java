package com.financeflow.openfinance.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class ImportedTransactionResponse {
    private UUID id;
    private String providerTransactionId;
    private String descricao;
    private BigDecimal valor;
    private LocalDate dataTransacao;
    private String categoriaSugerida;
    private String statusConciliacao;
    private UUID transactionId;
}
