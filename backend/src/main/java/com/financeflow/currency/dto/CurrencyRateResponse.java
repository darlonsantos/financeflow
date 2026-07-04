package com.financeflow.currency.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyRateResponse {
    private UUID id;
    private String fromCurrencyCode;
    private String toCurrencyCode;
    private BigDecimal rate;
    private LocalDateTime effectiveAt;
    private LocalDateTime createdAt;
}
