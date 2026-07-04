package com.financeflow.assistant.client;

import com.financeflow.assistant.config.OllamaProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.List;

/**
 * Cliente HTTP para a API Ollama (chat). Usado quando as regras do assistente não cobrem a pergunta.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OllamaClient {

    private static final String SYSTEM_PROMPT = """
        Você é um assistente financeiro. Use os dados financeiros do contexto para responder. Seja conciso e objetivo. Valores em R$ no formato brasileiro (ex.: 1.234,56).
        Perguntas factuais (use os números do contexto):
        - "quanto gastei/ganhei este mês", "minhas despesas/receitas" → use DESPESAS e RECEITAS do "Resumo do mês atual" e responda com os valores (ex.: "Você gastou R$ X este mês.").
        - Não diga "não há dados suficientes" quando Receitas e Despesas do mês estiverem no contexto.
        Pedidos de sugestões e planejamento (ex.: "proponha uma solução para poupar dinheiro", "como economizar", "dicas para poupar", "me ajude a gastar menos"):
        - Use o contexto do usuário (saldo, receitas, despesas, orçamentos, metas, parcelamentos, top categorias de gastos) para propor soluções personalizadas e práticas.
        - Sugira ações concretas baseadas nos dados: ex. reduzir categorias que mais gastam, respeitar orçamentos, destinar parte da sobra para metas, evitar novas parcelas.
        - Não invente valores; use apenas os do contexto. Se não houver sobra, sugira onde cortar com base nas categorias e orçamentos listados.
        Responda sempre em português brasileiro. Para pedidos de sugestão, use no máximo um parágrafo curto por ideia e seja direto.
        """;

    private final OllamaProperties properties;
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
     * Envia contexto + pergunta ao Ollama e retorna o texto da resposta, ou null em caso de erro/timeout.
     */
    public String chat(String userContext, String userMessage) {
        if (!properties.isEnabled()) {
            log.debug("Ollama is disabled, skipping chat");
            return null;
        }
        String url = properties.getBaseUrl().replaceAll("/$", "") + "/api/chat";
        String fullUserContent = userContext + "\n\nPergunta do usuário: " + userMessage;

        OllamaChatRequest request = new OllamaChatRequest();
        request.setModel(properties.getModel());
        request.setStream(false);
        request.setMessages(List.of(
                new OllamaMessage("system", SYSTEM_PROMPT),
                new OllamaMessage("user", fullUserContent)
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<OllamaChatRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<OllamaChatResponse> response = getRestTemplate().exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    OllamaChatResponse.class
            );
            OllamaChatResponse body = response != null ? response.getBody() : null;
            if (body != null && body.getMessage() != null) {
                return body.getMessage().getContent();
            }
            return null;
        } catch (ResourceAccessException e) {
            log.warn("Ollama not available or timeout: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("404") || msg.contains("not found")) {
                log.warn("Ollama model '{}' not found. Baixe com: ollama pull {}", properties.getModel(), properties.getModel());
            } else {
                log.warn("Ollama request failed: {}", msg);
            }
            return null;
        }
    }

    @Data
    public static class OllamaChatRequest {
        private String model;
        private List<OllamaMessage> messages;
        private boolean stream = false;
    }

    @Data
    public static class OllamaMessage {
        private String role;
        private String content;

        public OllamaMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    @Data
    public static class OllamaChatResponse {
        private OllamaMessageContent message;
    }

    @Data
    public static class OllamaMessageContent {
        private String content;
    }
}
