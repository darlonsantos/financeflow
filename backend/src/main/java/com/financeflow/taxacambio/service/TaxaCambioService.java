package com.financeflow.taxacambio.service;

import com.financeflow.currency.domain.CurrencyRate;
import com.financeflow.currency.repository.CurrencyRateRepository;
import com.financeflow.taxacambio.client.BcbPtaxClient;
import com.financeflow.taxacambio.dto.TaxaCambioCardResponse;
import com.financeflow.taxacambio.dto.TaxaCambioResumoResponse;
import com.financeflow.taxacambio.dto.TaxaCambioResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço de taxa de câmbio PTAX usando a tabela currency_rates (from=USD/EUR, to=BRL).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaxaCambioService {

    private static final String TO_BRL = "BRL";
    private static final List<String> MOEDAS = List.of("USD", "EUR");
    private static final Map<String, String> NOME_MOEDA = Map.of("USD", "Dólar Americano", "EUR", "Euro");
    private static final int SPARKLINE_DAYS = 7;
    private static final int LOOKBACK_DIAS_COTACAO = 7;

    private final BcbPtaxClient bcbClient;
    private final CurrencyRateRepository currencyRateRepository;

    /**
     * Resumo para a tela: última atualização + cards (USD, EUR) com valor, variação e sparkline.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "taxaCambioResumo", unless = "#result == null || #result.cards.isEmpty()")
    public TaxaCambioResumoResponse getResumo() {
        LocalDateTime ultima = null;
        List<TaxaCambioCardResponse> cards = new ArrayList<>();
        for (String moeda : MOEDAS) {
            Optional<CurrencyRate> latest = currencyRateRepository
                .findFirstByFromCurrencyCodeAndToCurrencyCodeOrderByEffectiveAtDesc(moeda, TO_BRL);
            if (latest.isEmpty()) continue;
            CurrencyRate r = latest.get();
            LocalDate dataCotacao = r.getEffectiveAt().toLocalDate();
            if (ultima == null || r.getCreatedAt().isAfter(ultima)) ultima = r.getCreatedAt();
            BigDecimal variacao = calcularVariacao(moeda, dataCotacao, r.getRate());
            List<BigDecimal> sparkline = getSparkline(moeda, dataCotacao, SPARKLINE_DAYS);
            cards.add(TaxaCambioCardResponse.builder()
                .moeda(moeda)
                .nomeMoeda(NOME_MOEDA.getOrDefault(moeda, moeda))
                .valor(r.getRate())
                .variacaoPercentual(variacao)
                .dataCotacao(dataCotacao)
                .sparkline(sparkline)
                .build());
        }
        return TaxaCambioResumoResponse.builder()
            .ultimaAtualizacao(ultima)
            .cards(cards)
            .build();
    }

    /**
     * Histórico paginado (mais recente primeiro). Moedas opcional (vazio = todas).
     */
    @Transactional(readOnly = true)
    public Page<TaxaCambioResponse> getHistorico(List<String> moedas, LocalDate dataInicio, LocalDate dataFim, Pageable pageable) {
        if (dataInicio == null) dataInicio = LocalDate.now().minusDays(90);
        if (dataFim == null) dataFim = LocalDate.now();
        List<String> m = (moedas == null || moedas.isEmpty()) ? MOEDAS : moedas;
        LocalDateTime start = dataInicio.atStartOfDay();
        LocalDateTime end = dataFim.plusDays(1).atStartOfDay();
        return currencyRateRepository
            .findByFromCurrencyCodeInAndToCurrencyCodeAndEffectiveAtBetweenOrderByEffectiveAtDesc(m, TO_BRL, start, end, pageable)
            .map(this::toResponse);
    }

    /**
     * Histórico para gráfico (lista simples, ordenada por data ascendente).
     */
    @Transactional(readOnly = true)
    public List<TaxaCambioResponse> getHistoricoGrafico(String moeda, LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio == null) dataInicio = LocalDate.now().minusDays(90);
        if (dataFim == null) dataFim = LocalDate.now();
        String m = (moeda == null || moeda.isBlank()) ? "USD" : moeda.toUpperCase();
        LocalDateTime start = dataInicio.atStartOfDay();
        LocalDateTime end = dataFim.plusDays(1).atStartOfDay();
        List<CurrencyRate> list = currencyRateRepository
            .findByFromCurrencyCodeAndToCurrencyCodeAndEffectiveAtBetweenOrderByEffectiveAtDesc(m, TO_BRL, start, end);
        Collections.reverse(list);
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Atualiza cotações a partir do BCB e persiste em currency_rates (from=USD/EUR, to=BRL). Limpa cache.
     * Se já existir cotação para hoje, atualiza o valor para refletir a última publicação do BCB.
     */
    @Transactional
    @CacheEvict(value = "taxaCambioResumo", allEntries = true)
    public void atualizarAgora() {
        LocalDate hoje = LocalDate.now();
        for (String moeda : MOEDAS) {
            CotacaoEncontrada cotacao = buscarCotacaoMaisRecente(moeda, hoje, LOOKBACK_DIAS_COTACAO);
            if (cotacao == null) {
                log.warn("Sem cotação disponível para {} nos últimos {} dias.", moeda, LOOKBACK_DIAS_COTACAO);
                continue;
            }
            List<CurrencyRate> existentes = currencyRateRepository
                .findByFromCurrencyCodeAndToCurrencyCodeAndEffectiveAtBetweenOrderByEffectiveAtDesc(
                    moeda,
                    TO_BRL,
                    cotacao.dataCotacao().atStartOfDay(),
                    cotacao.dataCotacao().plusDays(1).atStartOfDay());
            existentes.stream().findFirst().ifPresentOrElse(
                r -> {
                    r.setRate(cotacao.valor());
                    currencyRateRepository.save(r);
                },
                () -> salvarCotacao(moeda, cotacao.dataCotacao(), cotacao.valor()));
        }
    }

    /**
     * Job diário: busca cotação do dia (e, se BCB ainda não publicou, do último dia útil).
     */
    @Transactional
    @CacheEvict(value = "taxaCambioResumo", allEntries = true)
    public void sincronizarDiario() {
        LocalDate hoje = LocalDate.now();
        for (String moeda : MOEDAS) {
            LocalDate dataCotacao = hoje;
            BigDecimal valor = "USD".equals(moeda)
                ? bcbClient.fetchDolarDia(hoje)
                : bcbClient.fetchEuroDia(hoje);
            if (valor == null) {
                dataCotacao = hoje.minusDays(1);
                valor = "USD".equals(moeda)
                    ? bcbClient.fetchDolarDia(dataCotacao)
                    : bcbClient.fetchEuroDia(dataCotacao);
                if (valor == null) continue;
            }
            if (existeCotacaoParaData(moeda, dataCotacao)) continue;
            salvarCotacao(moeda, dataCotacao, valor);
        }
    }

    private boolean existeCotacaoParaData(String moeda, LocalDate data) {
        LocalDateTime start = data.atStartOfDay();
        LocalDateTime end = data.plusDays(1).atStartOfDay();
        return currencyRateRepository.existsByFromCurrencyCodeAndToCurrencyCodeAndEffectiveAtBetween(
            moeda, TO_BRL, start, end);
    }

    private void salvarCotacao(String moeda, LocalDate dataCotacao, BigDecimal valor) {
        CurrencyRate r = CurrencyRate.builder()
            .fromCurrencyCode(moeda)
            .toCurrencyCode(TO_BRL)
            .rate(valor)
            .effectiveAt(dataCotacao.atStartOfDay())
            .build();
        currencyRateRepository.save(Objects.requireNonNull(r));
    }

    private CotacaoEncontrada buscarCotacaoMaisRecente(String moeda, LocalDate dataBase, int lookbackDias) {
        for (int offset = 0; offset <= lookbackDias; offset++) {
            LocalDate data = dataBase.minusDays(offset);
            BigDecimal valor = "USD".equals(moeda)
                ? bcbClient.fetchDolarDia(data)
                : bcbClient.fetchEuroDia(data);
            if (valor != null) {
                return new CotacaoEncontrada(data, valor);
            }
        }
        return null;
    }

    private record CotacaoEncontrada(LocalDate dataCotacao, BigDecimal valor) {}

    private BigDecimal calcularVariacao(String moeda, LocalDate data, BigDecimal valorAtual) {
        LocalDateTime start = data.minusDays(10).atStartOfDay();
        LocalDateTime end = data.minusDays(1).plusDays(1).atStartOfDay();
        List<CurrencyRate> anteriores = currencyRateRepository
            .findByFromCurrencyCodeAndToCurrencyCodeAndEffectiveAtBetweenOrderByEffectiveAtDesc(moeda, TO_BRL, start, end);
        if (anteriores.isEmpty()) return null;
        BigDecimal vAnt = anteriores.get(0).getRate();
        if (vAnt == null || vAnt.compareTo(BigDecimal.ZERO) == 0) return null;
        return valorAtual.subtract(vAnt).divide(vAnt, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }

    private List<BigDecimal> getSparkline(String moeda, LocalDate ate, int dias) {
        LocalDate inicio = ate.minusDays(dias);
        LocalDateTime start = inicio.atStartOfDay();
        LocalDateTime end = ate.plusDays(1).atStartOfDay();
        List<CurrencyRate> list = currencyRateRepository
            .findByFromCurrencyCodeAndToCurrencyCodeAndEffectiveAtBetweenOrderByEffectiveAtDesc(moeda, TO_BRL, start, end);
        Collections.reverse(list);
        return list.stream().map(CurrencyRate::getRate).collect(Collectors.toList());
    }

    private TaxaCambioResponse toResponse(CurrencyRate r) {
        LocalDate dataCotacao = r.getEffectiveAt().toLocalDate();
        BigDecimal variacao = calcularVariacao(r.getFromCurrencyCode(), dataCotacao, r.getRate());
        return TaxaCambioResponse.builder()
            .id(r.getId())
            .moeda(r.getFromCurrencyCode())
            .valor(r.getRate())
            .variacaoPercentual(variacao)
            .dataCotacao(dataCotacao)
            .criadoEm(r.getCreatedAt())
            .build();
    }
}
