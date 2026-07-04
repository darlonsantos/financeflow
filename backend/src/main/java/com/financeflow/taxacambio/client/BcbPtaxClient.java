package com.financeflow.taxacambio.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Cliente para a API PTAX do Banco Central do Brasil (Olinda).
 * Documentação: https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata/
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BcbPtaxClient {

    private static final String BASE_URL = "https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata";
    // API PTAX OData espera datas em MM-dd-yyyy.
    private static final DateTimeFormatter BCB_DATE = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Cotação do dólar em uma data.
     */
    public BigDecimal fetchDolarDia(LocalDate data) {
        String url = BASE_URL + "/CotacaoDolarDia(dataCotacao=@dataCotacao)?@dataCotacao='"
            + data.format(BCB_DATE) + "'&$format=json";
        return parseCotacaoFromValue(url, data);
    }

    /**
     * Cotação do euro em uma data.
     */
    public BigDecimal fetchEuroDia(LocalDate data) {
        String url = BASE_URL + "/CotacaoMoedaDia(moeda=@moeda,dataCotacao=@dataCotacao)?@moeda='EUR'&@dataCotacao='"
            + data.format(BCB_DATE) + "'&$format=json";
        return parseCotacaoFromValue(url, data);
    }

    /**
     * Cotações do dólar em um período (ordenadas por data ascendente).
     */
    public List<BcbCotacaoItem> fetchDolarPeriodo(LocalDate dataInicial, LocalDate dataFinal) {
        String url = BASE_URL + "/CotacaoDolarPeriodo(dataInicial=@dataInicial,dataFinalCotacao=@dataFinalCotacao)?@dataInicial='"
            + dataInicial.format(BCB_DATE) + "'&@dataFinalCotacao='" + dataFinal.format(BCB_DATE) + "'&$format=json";
        return parsePeriodo(url, "USD");
    }

    /**
     * Cotações do euro em um período (ordenadas por data ascendente).
     */
    public List<BcbCotacaoItem> fetchEuroPeriodo(LocalDate dataInicial, LocalDate dataFinal) {
        String url = BASE_URL + "/CotacaoMoedaPeriodo(moeda=@moeda,dataInicial=@dataInicial,dataFinalCotacao=@dataFinalCotacao)?@moeda='EUR'&@dataInicial='"
            + dataInicial.format(BCB_DATE) + "'&@dataFinalCotacao='" + dataFinal.format(BCB_DATE) + "'&$format=json";
        return parsePeriodo(url, "EUR");
    }

    private BigDecimal parseCotacaoFromValue(String url, LocalDate data) {
        try {
            String json = restTemplate.getForObject(url, String.class);
            if (json == null) return null;
            JsonNode root = objectMapper.readTree(json);
            JsonNode value = root.get("value");
            if (value == null || !value.isArray() || value.isEmpty()) return null;
            JsonNode first = value.get(0);
            BigDecimal compra = first.has("cotacaoCompra") ? first.get("cotacaoCompra").decimalValue() : null;
            BigDecimal venda = first.has("cotacaoVenda") ? first.get("cotacaoVenda").decimalValue() : null;
            if (venda != null) return venda;
            if (compra != null) return compra;
            return null;
        } catch (Exception e) {
            log.warn("Erro ao buscar cotação BCB: {} - {}", url, e.getMessage());
            return null;
        }
    }

    private List<BcbCotacaoItem> parsePeriodo(String url, String moeda) {
        List<BcbCotacaoItem> out = new ArrayList<>();
        try {
            String json = restTemplate.getForObject(url, String.class);
            if (json == null) return out;
            JsonNode root = objectMapper.readTree(json);
            JsonNode value = root.get("value");
            if (value == null || !value.isArray()) return out;
            for (JsonNode node : value) {
                LocalDate data = null;
                if (node.has("dataHoraCotacao")) {
                    String dt = node.get("dataHoraCotacao").asText();
                    if (dt.length() >= 10) data = LocalDate.parse(dt.substring(0, 10));
                }
                if (data == null && node.has("dataCotacao")) {
                    data = LocalDate.parse(node.get("dataCotacao").asText().substring(0, 10));
                }
                BigDecimal compra = node.has("cotacaoCompra") ? node.get("cotacaoCompra").decimalValue() : null;
                BigDecimal venda = node.has("cotacaoVenda") ? node.get("cotacaoVenda").decimalValue() : null;
                BigDecimal valor = venda != null ? venda : compra;
                if (data != null && valor != null)
                    out.add(new BcbCotacaoItem(moeda, data, valor));
            }
        } catch (Exception e) {
            log.warn("Erro ao buscar período BCB: {} - {}", url, e.getMessage());
        }
        return out;
    }

    public record BcbCotacaoItem(String moeda, LocalDate dataCotacao, BigDecimal valor) {}
}
