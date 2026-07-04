package com.financeflow.currency.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyRateRequest {

    @NotBlank(message = "Moeda de origem é obrigatória")
    @Size(min = 3, max = 3)
    private String fromCurrencyCode;

    @NotBlank(message = "Moeda de destino é obrigatória")
    @Size(min = 3, max = 3)
    private String toCurrencyCode;

    @DecimalMin(value = "0.00000001", message = "Taxa deve ser positiva")
    private BigDecimal rate;
}
