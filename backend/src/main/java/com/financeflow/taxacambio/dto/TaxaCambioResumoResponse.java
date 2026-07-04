package com.financeflow.taxacambio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxaCambioResumoResponse {

    /** Data/hora da última atualização (mais recente data_cotacao entre as moedas). */
    private LocalDateTime ultimaAtualizacao;
    /** Cards para exibição (USD e EUR). */
    private List<TaxaCambioCardResponse> cards;
}
