package com.financeflow.predictive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Representa um alerta preventivo gerado pela análise preditiva.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictiveAlertDto {

    /** Tipo do risco identificado. */
    private String riskType;

    /** Severidade: HIGH, MEDIUM, LOW. */
    private String severity;

    /** Título curto do alerta. */
    private String title;

    /** Mensagem descritiva. */
    private String message;

    /** Sugestão de ação preventiva. */
    private String suggestion;

    /** Tipo da entidade relacionada (Budget, Goal, etc.), opcional. */
    private String entityType;

    /** ID da entidade relacionada, opcional. */
    private UUID entityId;
}
