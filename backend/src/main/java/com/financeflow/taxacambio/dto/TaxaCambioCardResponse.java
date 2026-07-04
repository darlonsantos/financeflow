package com.financeflow.taxacambio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Dados para o card de uma moeda na tela de taxa de câmbio (valor atual, variação, sparkline). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxaCambioCardResponse {

    private String moeda;
    private String nomeMoeda;
    private BigDecimal valor;
    private BigDecimal variacaoPercentual;
    private LocalDate dataCotacao;
    /** Valores recentes para sparkline (mais antigo primeiro). */
    private List<BigDecimal> sparkline;
}
