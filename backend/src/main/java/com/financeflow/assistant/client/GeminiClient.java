package com.financeflow.assistant.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.financeflow.assistant.config.GeminiProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.List;

/**
 * Cliente HTTP para a API Google Gemini (generateContent).
 * Usado para o assistente financeiro e relatórios quando Gemini está habilitado.
 * API Key permanece apenas no backend (nunca exposta no frontend).
 */
@Component
@ConditionalOnProperty(name = "gemini.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class GeminiClient {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models";

    private static final String SYSTEM_PROMPT = """
        Você é um Agente Financeiro Inteligente especializado em análise objetiva e planejamento financeiro pessoal.

        Objetivo:
        Responder perguntas financeiras usando exclusivamente os dados fornecidos no contexto.

        Regras obrigatórias:
        - Nunca inventar valores.
        - Usar apenas números presentes no contexto.
        - Valores sempre em R$ no formato brasileiro (ex.: 1.234,56).
        - Responder sempre em português brasileiro.
        - Ser conciso e objetivo.

        Classificação da intenção:
        1. Pergunta factual:
           - Exemplos: "quanto gastei este mês", "minhas despesas"
           - Use exclusivamente RECEITAS e DESPESAS do "Resumo do mês atual".
           - Nunca dizer "dados insuficientes" se essas informações estiverem presentes.

           
        2. Pedido de sugestão ou planejamento:
           - Exemplos: "como economizar", "me ajude a poupar"
           - Analisar saldo, categorias de maior gasto, orçamentos, metas e parcelamentos.
           - Propor ações concretas baseadas nos dados.
           - Se não houver sobra, sugerir cortes nas maiores categorias.
           - Evitar novas parcelas se o saldo estiver comprometido.

        Processo interno obrigatório:
        1. Identificar o tipo de pergunta.
        2. Aplicar regras correspondentes.
        3. Validar que todos valores mencionados existem no contexto.
        4. Gerar resposta final estruturada.
        """;

    private final GeminiProperties properties;
    private RestTemplate restTemplate;

    private RestTemplate getRestTemplate() {
        if (restTemplate == null) {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(5000);
            factory.setReadTimeout(Math.max(5000, properties.getTimeoutSeconds() * 1000));
            restTemplate = new RestTemplate(factory);
        }
        return restTemplate;
    }

    /**
     * Envia contexto + pergunta ao Gemini e retorna o texto da resposta, ou null em caso de erro/timeout.
     */
    public String chat(String userContext, String userMessage) {
        if (!properties.isConfigured()) {
            log.debug("Gemini is disabled or api-key missing, skipping chat");
            return null;
        }
        String url = BASE_URL + "/" + properties.getModel() + ":generateContent?key=" + properties.getApiKey().trim();
        String fullUserContent = userContext + "\n\nPergunta do usuário: " + userMessage;

        GeminiRequest request = new GeminiRequest();
        request.setSystemInstruction(new SystemInstruction(List.of(new Part(SYSTEM_PROMPT))));
        request.setContents(List.of(
            new ContentItem(List.of(new Part(fullUserContent)))
        ));
        request.setGenerationConfig(new GenerationConfig(
            properties.getMaxOutputTokens(),
            properties.getTemperature()
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<GeminiRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<String> response = getRestTemplate().exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
            );
            String body = response != null ? response.getBody() : null;
            if (body == null || body.isBlank()) return null;

            JsonNode root = new ObjectMapper().readTree(body);
            if (root.has("error")) {
                JsonNode err = root.get("error");
                String message = err.has("message") ? err.path("message").asText("") : err.toString();
                String status = err.has("status") ? err.path("status").asText("") : "";
                log.warn("Gemini API error: {} {}", status, message);
                return null;
            }
            if (root.has("candidates") && root.get("candidates").isArray() && root.get("candidates").size() > 0) {
                JsonNode candidate = root.get("candidates").get(0);
                JsonNode content = candidate.path("content");
                if (content.has("parts") && content.get("parts").size() > 0) {
                    String text = content.get("parts").get(0).path("text").asText(null);
                    if (text != null && !text.isBlank()) return text;
                }
            }
            if (root.has("promptFeedback") && root.get("promptFeedback").has("blockReason")) {
                log.warn("Gemini block reason: {}", root.get("promptFeedback").path("blockReason").asText());
            }
            return null;
        } catch (HttpStatusCodeException e) {
            String body = e.getResponseBodyAsString();
            log.warn("Gemini API HTTP {}: {}", e.getStatusCode(), body != null && body.length() > 200 ? body.substring(0, 200) + "..." : body);
            return null;
        } catch (ResourceAccessException e) {
            log.warn("Gemini not available or timeout: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            log.warn("Gemini request failed: {}", msg);
            return null;
        }
    }

    // --- Request/Response DTOs (compatíveis com a API REST do Gemini) ---

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class GeminiRequest {
        private SystemInstruction systemInstruction;
        private List<ContentItem> contents;
        private GenerationConfig generationConfig;
    }

    @Data
    public static class SystemInstruction {
        private List<Part> parts;

        public SystemInstruction(List<Part> parts) {
            this.parts = parts;
        }
    }

    @Data
    public static class ContentItem {
        private String role = "user";
        private List<Part> parts;

        public ContentItem(List<Part> parts) {
            this.parts = parts;
        }
    }

    @Data
    public static class Part {
        private String text;

        public Part(String text) {
            this.text = text;
        }
    }

    @Data
    public static class GenerationConfig {
        private Integer maxOutputTokens;
        private Double temperature;

        public GenerationConfig(int maxOutputTokens, double temperature) {
            this.maxOutputTokens = maxOutputTokens;
            this.temperature = temperature;
        }
    }

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class GeminiResponse {
        private List<Candidate> candidates;
        private PromptFeedback promptFeedback;

        @Data
        public static class Candidate {
            private ContentContent content;
            private String finishReason;
        }

        @Data
        public static class ContentContent {
            private List<Part> parts;
        }

        @Data
        public static class PromptFeedback {
            private String blockReason;
        }
    }
}
