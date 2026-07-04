package com.financeflow.openfinance.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class CreditCardSummaryResponse {
    private String providerAccountId;
    private String nomeConta;
    private String moeda;
    private BigDecimal totalFatura;
    private BigDecimal totalFaturaMesCorrente;
    private BigDecimal pagamentoMinimo;
    private LocalDate vencimentoFatura;
    private LocalDate fechamentoFatura;
    private BigDecimal limiteDisponivel;
    private BigDecimal limiteTotal;
}
