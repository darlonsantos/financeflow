package com.financeflow.predictive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Resposta do módulo de inteligência financeira preditiva: cenário e alertas preventivos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictiveReportResponse {

    /** Data/hora da análise. */
    private Instant generatedAt;

    /** Resumo em texto da situação (1–2 frases). */
    private String summary;

    /** Descrição breve do cenário nos próximos meses (ex.: saldo projetado, tendências). */
    private String scenarioNextMonths;

    /** Lista de alertas preventivos ordenados por severidade. */
    private List<PredictiveAlertDto> alerts;

    /** Quantidade de meses de histórico considerados na análise. */
    private int historicalMonths;

    /** Quantidade de meses futuros considerados na projeção. */
    private int projectionMonths;
}
