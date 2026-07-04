package com.financeflow.currency.service;

import com.financeflow.currency.domain.CurrencyRate;
import com.financeflow.currency.repository.CurrencyRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Converte valores entre moedas usando taxas configuráveis.
 * Valores históricos (em contas/transações) não são alterados; conversão é feita apenas para exibição/agregados.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyConversionService {

    private final CurrencyRateRepository currencyRateRepository;

    /**
     * Converte um valor de uma moeda para outra usando a taxa vigente em asOf (ou a mais recente disponível).
     */
    public Optional<BigDecimal> convert(BigDecimal amount, String fromCurrencyCode, String toCurrencyCode, LocalDateTime asOf) {
        if (amount == null || fromCurrencyCode == null || toCurrencyCode == null) {
            return Optional.empty();
        }
        fromCurrencyCode = fromCurrencyCode.toUpperCase().trim();
        toCurrencyCode = toCurrencyCode.toUpperCase().trim();
        if (fromCurrencyCode.equals(toCurrencyCode)) {
            return Optional.of(amount.setScale(2, RoundingMode.HALF_UP));
        }
        LocalDateTime effective = asOf != null ? asOf : LocalDateTime.now();
        Optional<CurrencyRate> rateOpt = currencyRateRepository
            .findFirstByFromCurrencyCodeAndToCurrencyCodeAndEffectiveAtLessThanEqualOrderByEffectiveAtDesc(
                fromCurrencyCode, toCurrencyCode, effective);
        return rateOpt.map(rate -> amount.multiply(rate.getRate()).setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * Converte usando a data/hora atual.
     */
    public Optional<BigDecimal> convert(BigDecimal amount, String fromCurrencyCode, String toCurrencyCode) {
        return convert(amount, fromCurrencyCode, toCurrencyCode, LocalDateTime.now());
    }

    /**
     * Retorna a taxa de conversão (1 from = rate * to) vigente em asOf, ou empty se não houver.
     */
    public Optional<BigDecimal> getRate(String fromCurrencyCode, String toCurrencyCode, LocalDateTime asOf) {
        if (fromCurrencyCode == null || toCurrencyCode == null) return Optional.empty();
        fromCurrencyCode = fromCurrencyCode.toUpperCase().trim();
        toCurrencyCode = toCurrencyCode.toUpperCase().trim();
        if (fromCurrencyCode.equals(toCurrencyCode)) return Optional.of(BigDecimal.ONE);
        LocalDateTime effective = asOf != null ? asOf : LocalDateTime.now();
        return currencyRateRepository
            .findFirstByFromCurrencyCodeAndToCurrencyCodeAndEffectiveAtLessThanEqualOrderByEffectiveAtDesc(
                fromCurrencyCode, toCurrencyCode, effective)
            .map(CurrencyRate::getRate);
    }
}
