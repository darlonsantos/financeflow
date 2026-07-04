package com.financeflow.openfinance.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financeflow.openfinance.config.OpenFinanceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenFinanceProviderClient {

    private final OpenFinanceProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ProviderSessionResponse createConnectionSession(UUID userId) {
        String provider = properties.getProvider() == null ? "pluggy" : properties.getProvider().toLowerCase();
        if (!"pluggy".equals(provider)) {
            throw new IllegalArgumentException("Provider não suportado: " + provider);
        }
        validateCredentials();
        try {
            String apiKey = createApiKey();
            String connectToken = createConnectToken(apiKey, userId.toString());
            return new ProviderSessionResponse(
                connectToken,
                "pending_" + UUID.randomUUID(),
                null,
                null,
                LocalDateTime.now().plusMinutes(30)
            );
        } catch (Exception e) {
            log.error("Erro ao criar sessão no Pluggy", e);
            throw new IllegalStateException("Falha ao gerar Connect Token na Pluggy: " + e.getMessage(), e);
        }
    }

    public List<PluggyAccountData> fetchAccounts(String itemId) {
        validateCredentials();
        try {
            String apiKey = createApiKey();
            String url = UriComponentsBuilder
                .fromHttpUrl(properties.getBaseUrl() + "/accounts")
                .queryParam("itemId", itemId)
                .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-KEY", apiKey);
            ResponseEntity<String> response;
            try {
                response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            } catch (HttpStatusCodeException e) {
                throw new IllegalStateException("Pluggy /accounts retornou " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode results = root.get("results");
            List<PluggyAccountData> accounts = new ArrayList<>();
            if (results != null && results.isArray()) {
                for (JsonNode node : results) {
                    accounts.add(new PluggyAccountData(
                        node.path("id").asText(),
                        node.path("type").asText(null),
                        node.path("name").asText("Conta sem nome"),
                        toBigDecimal(node.path("balance")),
                        node.path("subtype").asText("CHECKING_ACCOUNT"),
                        node.path("currencyCode").asText("BRL"),
                        toBigDecimalOrNull(node.path("creditData").path("minimumPayment")),
                        toLocalDateOrNull(node.path("creditData").path("balanceDueDate").asText(null)),
                        toLocalDateOrNull(node.path("creditData").path("balanceCloseDate").asText(null)),
                        toBigDecimalOrNull(node.path("creditData").path("availableCreditLimit")),
                        toBigDecimalOrNull(node.path("creditData").path("creditLimit"))
                    ));
                }
            }
            return accounts;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao buscar contas na Pluggy: " + e.getMessage(), e);
        }
    }

    public String fetchInstitutionName(String itemId) {
        if (!isUuid(itemId)) {
            return null;
        }
        validateCredentials();
        try {
            String apiKey = createApiKey();
            String url = properties.getBaseUrl() + "/items/" + itemId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-KEY", apiKey);
            ResponseEntity<String> response;
            try {
                response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            } catch (HttpStatusCodeException e) {
                throw new IllegalStateException("Pluggy /items retornou " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            String connectorName = root.path("connector").path("name").asText(null);
            if (StringUtils.hasText(connectorName)) {
                return connectorName;
            }
            String institutionName = root.path("institution").path("name").asText(null);
            return StringUtils.hasText(institutionName) ? institutionName : null;
        } catch (Exception e) {
            log.warn("Falha ao buscar nome da instituição no Pluggy para itemId={}: {}", itemId, e.getMessage());
            return null;
        }
    }

    private boolean isUuid(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        try {
            UUID.fromString(value.trim());
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public List<PluggyTransactionData> fetchTransactions(String accountId, LocalDate from, LocalDate to) {
        validateCredentials();
        try {
            String apiKey = createApiKey();
            int page = 1;
            int totalPages = 1;
            List<PluggyTransactionData> out = new ArrayList<>();
            while (page <= totalPages) {
                String url = UriComponentsBuilder
                    .fromHttpUrl(properties.getBaseUrl() + "/transactions")
                    .queryParam("accountId", accountId)
                    .queryParam("from", from)
                    .queryParam("to", to)
                    .queryParam("pageSize", 500)
                    .queryParam("page", page)
                    .toUriString();

                HttpHeaders headers = new HttpHeaders();
                headers.set("X-API-KEY", apiKey);
                ResponseEntity<String> response;
                try {
                    response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
                } catch (HttpStatusCodeException e) {
                    throw new IllegalStateException("Pluggy /transactions retornou " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
                }

                JsonNode root = objectMapper.readTree(response.getBody());
                totalPages = root.path("totalPages").asInt(1);
                JsonNode results = root.get("results");
                if (results != null && results.isArray()) {
                    for (JsonNode node : results) {
                        out.add(new PluggyTransactionData(
                            node.path("id").asText(),
                            node.path("description").asText("Sem descrição"),
                            toBigDecimal(node.path("amount")),
                            toLocalDate(node.path("date").asText()),
                            node.path("category").asText(null)
                        ));
                    }
                }
                page++;
            }
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao buscar transações na Pluggy: " + e.getMessage(), e);
        }
    }

    public List<PluggyBillData> fetchBills(String accountId) {
        validateCredentials();
        try {
            String apiKey = createApiKey();
            int page = 1;
            int totalPages = 1;
            List<PluggyBillData> out = new ArrayList<>();
            while (page <= totalPages) {
                String url = UriComponentsBuilder
                    .fromHttpUrl(properties.getBaseUrl() + "/bills")
                    .queryParam("accountId", accountId)
                    .queryParam("pageSize", 200)
                    .queryParam("page", page)
                    .toUriString();

                HttpHeaders headers = new HttpHeaders();
                headers.set("X-API-KEY", apiKey);
                ResponseEntity<String> response;
                try {
                    response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
                } catch (HttpStatusCodeException e) {
                    // Nem todo conector expõe bills; nesse caso fazemos fallback no serviço.
                    log.warn("Pluggy /bills retornou {} para accountId={}", e.getStatusCode().value(), accountId);
                    return out;
                }

                JsonNode root = objectMapper.readTree(response.getBody());
                totalPages = root.path("totalPages").asInt(1);
                JsonNode results = root.get("results");
                if (results != null && results.isArray()) {
                    for (JsonNode node : results) {
                        out.add(new PluggyBillData(
                            node.path("id").asText(),
                            toLocalDateOrNull(node.path("dueDate").asText(null)),
                            toBigDecimalOrNull(node.path("totalAmount")),
                            node.path("totalAmountCurrencyCode").asText(null),
                            toBigDecimalOrNull(node.path("minimumPaymentAmount"))
                        ));
                    }
                }
                page++;
            }
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao buscar faturas na Pluggy: " + e.getMessage(), e);
        }
    }

    private void validateCredentials() {
        if (!StringUtils.hasText(properties.getClientId()) || !StringUtils.hasText(properties.getClientSecret())) {
            throw new IllegalStateException("Credenciais Pluggy não configuradas (open-finance.client-id/client-secret)");
        }
    }

    private String createApiKey() throws Exception {
        String url = properties.getBaseUrl() + "/auth";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> body = new HashMap<>();
        body.put("clientId", properties.getClientId());
        body.put("clientSecret", properties.getClientSecret());

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class
            );
        } catch (HttpStatusCodeException e) {
            String details = e.getResponseBodyAsString();
            throw new IllegalStateException("Pluggy /auth retornou " + e.getStatusCode().value() + ": " + details);
        }
        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode apiKeyNode = root.get("apiKey");
        if (apiKeyNode == null || apiKeyNode.asText().isBlank()) {
            throw new IllegalStateException("Resposta Pluggy sem apiKey");
        }
        return apiKeyNode.asText();
    }

    private String createConnectToken(String apiKey, String clientUserId) throws Exception {
        String url = properties.getBaseUrl() + "/connect_token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-KEY", apiKey);

        Map<String, Object> options = new HashMap<>();
        options.put("clientUserId", clientUserId);
        options.put("avoidDuplicates", true);
        Map<String, Object> payload = new HashMap<>();
        payload.put("options", options);

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(payload, headers),
                String.class
            );
        } catch (HttpStatusCodeException e) {
            String details = e.getResponseBodyAsString();
            throw new IllegalStateException("Pluggy /connect_token retornou " + e.getStatusCode().value() + ": " + details);
        }
        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode tokenNode = root.get("accessToken");
        if (tokenNode == null || tokenNode.asText().isBlank()) {
            throw new IllegalStateException("Resposta Pluggy sem connect token");
        }
        return tokenNode.asText();
    }

    private BigDecimal toBigDecimal(JsonNode node) {
        if (node == null || node.isNull()) {
            return BigDecimal.ZERO;
        }
        return node.decimalValue();
    }

    private BigDecimal toBigDecimalOrNull(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        return node.decimalValue();
    }

    private LocalDate toLocalDate(String value) {
        if (!StringUtils.hasText(value)) {
            return LocalDate.now();
        }
        try {
            return OffsetDateTime.parse(value).toLocalDate();
        } catch (Exception ignored) {
            if (value.length() >= 10) {
                return LocalDate.parse(value.substring(0, 10));
            }
            return LocalDate.now();
        }
    }

    private LocalDate toLocalDateOrNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toLocalDate();
        } catch (Exception ignored) {
            if (value.length() >= 10) {
                return LocalDate.parse(value.substring(0, 10));
            }
            return null;
        }
    }
}
