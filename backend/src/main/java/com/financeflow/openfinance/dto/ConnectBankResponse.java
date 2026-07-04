package com.financeflow.openfinance.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ConnectBankResponse {
    private UUID connectionId;
    private String linkToken;
    private String providerConnectionId;
    private String status;
}
