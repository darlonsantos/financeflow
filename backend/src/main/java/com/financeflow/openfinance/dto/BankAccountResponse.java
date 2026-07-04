package com.financeflow.openfinance.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class BankAccountResponse {
    private UUID id;
    private String providerAccountId;
    private String nome;
    private String banco;
    private BigDecimal saldoAtual;
    private String tipoConta;
}
