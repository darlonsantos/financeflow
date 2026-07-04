package com.financeflow.projections.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceProjectionResponse {

    private BigDecimal currentBalance;
    private LocalDate projectionStartDate;
    private int monthsProjected;
    private List<MonthProjection> projections;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthProjection {
        private String monthLabel;
        private LocalDate monthStart;
        private BigDecimal balance;
        private BigDecimal projectedIncome;
        private BigDecimal projectedExpense;
    }
}
