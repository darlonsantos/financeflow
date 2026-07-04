package com.financeflow.openfinance.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class BankConnectionResponse {
    private UUID id;
    private String provider;
    private String providerConnectionId;
    private String status;
    private LocalDateTime expiraEm;
    private LocalDateTime dataCriacao;
}
