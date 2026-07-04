package com.financeflow.currency.service;

import com.financeflow.currency.domain.CurrencyRate;
import com.financeflow.currency.dto.CurrencyRateRequest;
import com.financeflow.currency.dto.CurrencyRateResponse;
import com.financeflow.currency.repository.CurrencyRateRepository;
import com.financeflow.currency.repository.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CurrencyRateService {

    private final CurrencyRateRepository currencyRateRepository;
    private final CurrencyRepository currencyRepository;

    @Transactional(readOnly = true)
    public List<CurrencyRateResponse> findAllRates(String fromCode, String toCode) {
        List<CurrencyRate> list;
        if (fromCode != null && !fromCode.isBlank() && toCode != null && !toCode.isBlank()) {
            list = currencyRateRepository.findByFromCurrencyCodeAndToCurrencyCodeOrderByEffectiveAtDesc(
                fromCode.toUpperCase().trim(), toCode.toUpperCase().trim());
        } else {
            list = currencyRateRepository.findAll();
        }
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CurrencyRateResponse getLatestRate(String fromCode, String toCode) {
        return currencyRateRepository
            .findFirstByFromCurrencyCodeAndToCurrencyCodeAndEffectiveAtLessThanEqualOrderByEffectiveAtDesc(
                fromCode.toUpperCase().trim(), toCode.toUpperCase().trim(), LocalDateTime.now())
            .map(this::toResponse)
            .orElse(null);
    }

    @Transactional
    public CurrencyRateResponse createOrUpdateRate(CurrencyRateRequest request) {
        String from = request.getFromCurrencyCode().toUpperCase().trim();
        String to = request.getToCurrencyCode().toUpperCase().trim();
        if (from.equals(to)) {
            throw new IllegalArgumentException("Moeda de origem e destino devem ser diferentes");
        }
        if (currencyRepository.findByCode(from).isEmpty()) {
            throw new IllegalArgumentException("Moeda de origem não encontrada: " + from);
        }
        if (currencyRepository.findByCode(to).isEmpty()) {
            throw new IllegalArgumentException("Moeda de destino não encontrada: " + to);
        }
        CurrencyRate rate = CurrencyRate.builder()
            .fromCurrencyCode(from)
            .toCurrencyCode(to)
            .rate(request.getRate())
            .effectiveAt(LocalDateTime.now())
            .build();
        rate = currencyRateRepository.save(rate);
        return toResponse(rate);
    }

    @Transactional
    public void deleteRates(String fromCode, String toCode) {
        currencyRateRepository.deleteByFromCurrencyCodeAndToCurrencyCode(
            fromCode.toUpperCase().trim(), toCode.toUpperCase().trim());
    }

    private CurrencyRateResponse toResponse(CurrencyRate r) {
        return CurrencyRateResponse.builder()
            .id(r.getId())
            .fromCurrencyCode(r.getFromCurrencyCode())
            .toCurrencyCode(r.getToCurrencyCode())
            .rate(r.getRate())
            .effectiveAt(r.getEffectiveAt())
            .createdAt(r.getCreatedAt())
            .build();
    }
}
