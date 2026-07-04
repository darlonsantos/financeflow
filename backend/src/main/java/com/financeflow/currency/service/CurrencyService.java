package com.financeflow.currency.service;

import com.financeflow.currency.domain.Currency;
import com.financeflow.currency.dto.CurrencyResponse;
import com.financeflow.currency.repository.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CurrencyService {

    private final CurrencyRepository currencyRepository;

    @Transactional(readOnly = true)
    public List<CurrencyResponse> findAll() {
        return currencyRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CurrencyResponse findByCode(String code) {
        return currencyRepository.findByCode(code.toUpperCase().trim())
            .map(this::toResponse)
            .orElse(null);
    }

    private CurrencyResponse toResponse(Currency c) {
        return CurrencyResponse.builder()
            .code(c.getCode())
            .name(c.getName())
            .symbol(c.getSymbol())
            .decimalPlaces(c.getDecimalPlaces())
            .build();
    }
}
