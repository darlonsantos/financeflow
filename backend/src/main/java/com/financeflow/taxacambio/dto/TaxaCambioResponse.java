package com.financeflow.taxacambio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxaCambioResponse {

    private UUID id;
    private String moeda;
    private BigDecimal valor;
    private BigDecimal variacaoPercentual;
    private LocalDate dataCotacao;
    private LocalDateTime criadoEm;
}
