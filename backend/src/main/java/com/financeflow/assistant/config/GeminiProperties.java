package com.financeflow.assistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuração da integração com Google Gemini API.
 * API Key deve vir de variável de ambiente (nunca no frontend).
 */
@Data
@Component
@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {

    /** Habilita o uso do Gemini quando true e api-key está definida. */
    private boolean enabled = false;

    /** API Key do Google AI (variável GEMINI_API_KEY). Não expor no frontend. */
    private String apiKey = "AIzaSyCw1gR1O32MdZf4Unq_7OEqIDmThR-OLVk";

    /**
     * Modelo para produção. gemini-2.5-flash: bom custo-benefício e baixa latência (estável).
     * Alternativas: gemini-2.5-pro (mais capaz), gemini-2.5-flash-lite (mais barato), gemini-3-flash-preview.
     */
    private String model = "gemini-2.5-flash";

    /** Timeout da requisição HTTP em segundos. */
    private int timeoutSeconds = 30;

    /** Limite de tokens na resposta (controle de custo). */
    private int maxOutputTokens = 1024;

    /** Temperatura (0 = mais determinístico, 1 = mais criativo). Para contexto financeiro, baixo. */
    private double temperature = 0.2;

    /**
     * Indica se o Gemini está configurado e pode ser usado.
     */
    public boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }
}
