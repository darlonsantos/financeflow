package com.financeflow.behavioral.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Resposta do perfil financeiro comportamental.
 * Classificação por IA ou por regras (fallback).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BehavioralProfileResponse {

    /** Classificação do perfil (ex: Conservador, Planejador, Impulsivo, Equilibrado, Arrojado, Desorganizado). */
    private String profileType;

    /** Grau de risco financeiro: Baixo, Médio, Alto. */
    private String riskLevel;

    /** Principais padrões comportamentais identificados. */
    private List<String> patterns;

    /** Pontos críticos que precisam de atenção. */
    private List<String> criticalPoints;

    /** Sugestões práticas e personalizadas de melhoria. */
    private List<String> suggestions;

    /** Data/hora da análise. */
    private Instant generatedAt;

    /** true se o resultado veio da IA (Gemini); false se fallback por regras. */
    private boolean fromAi;
}
