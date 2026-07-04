package com.financeflow.openfinance.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class SyncHistoryResponse {
    private UUID id;
    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;
    private Integer totalImportado;
    private Integer conflitos;
    private String status;
    private String mensagemErro;
}
