package com.financeflow.reports.service;

import com.financeflow.assistant.client.GeminiClient;
import com.financeflow.assistant.client.OllamaClient;
import com.financeflow.assistant.config.GeminiProperties;
import com.financeflow.assistant.config.OllamaProperties;
import com.financeflow.assistant.service.AssistantContextBuilder;
import com.financeflow.reports.dto.AIReportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Gera relatório financeiro em texto usando IA (Gemini ou Ollama) com base nos dados do usuário.
 * Prioridade: Gemini → Ollama.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIReportService {

    private static final String REPORT_PROMPT = """
        Gere um relatório financeiro em texto (2 a 4 parágrafos) com base nos dados acima. Inclua:
        1) Resumo do mês: saldo total, receitas e despesas.
        2) Pontos de atenção: orçamentos próximos do limite ou excedidos, metas em andamento.
        3) Breve sugestão ou conclusão se fizer sentido.
        Use APENAS os dados fornecidos. Formate valores em R$ no padrão brasileiro. Seja objetivo e em português brasileiro.
        """;

    private static final String FALLBACK_MESSAGE = "Relatório por IA indisponível no momento. Configure Gemini (GEMINI_API_KEY e GEMINI_ENABLED=true) ou Ollama. Você pode usar o relatório em PDF nas transações.";

    private final GeminiProperties geminiProperties;
    private final OllamaProperties ollamaProperties;
    private final AssistantContextBuilder contextBuilder;
    private final Optional<GeminiClient> geminiClient;
    private final OllamaClient ollamaClient;

    @Transactional(readOnly = true)
    public AIReportResponse generateReport() {
        UUID userId = getCurrentUserId();
        String context = contextBuilder.buildContext(userId);

        if (geminiProperties.isConfigured() && geminiClient.isPresent()) {
            try {
                String content = geminiClient.get().chat(context, REPORT_PROMPT);
                if (content != null && !content.isBlank()) {
                    return AIReportResponse.builder()
                            .title("Relatório financeiro (IA)")
                            .content(content.trim())
                            .generatedAt(Instant.now())
                            .fromAi(true)
                            .build();
                }
            } catch (Exception e) {
                log.warn("Falha ao gerar relatório por Gemini: {}", e.getMessage());
            }
        }
        if (ollamaProperties.isEnabled()) {
            try {
                String content = ollamaClient.chat(context, REPORT_PROMPT);
                if (content != null && !content.isBlank()) {
                    return AIReportResponse.builder()
                            .title("Relatório financeiro (IA)")
                            .content(content.trim())
                            .generatedAt(Instant.now())
                            .fromAi(true)
                            .build();
                }
            } catch (Exception e) {
                log.warn("Falha ao gerar relatório por Ollama: {}", e.getMessage());
            }
        }

        return AIReportResponse.builder()
                .title("Relatório financeiro")
                .content(FALLBACK_MESSAGE)
                .generatedAt(Instant.now())
                .fromAi(false)
                .build();
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UUID)) {
            throw new RuntimeException("Usuário não autenticado");
        }
        return (UUID) auth.getPrincipal();
    }
}
