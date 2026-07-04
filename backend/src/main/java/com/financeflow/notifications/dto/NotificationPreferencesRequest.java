package com.financeflow.notifications.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferencesRequest {

    private Boolean budgetExceededEnabled;
    private Boolean lowBalanceEnabled;
    private Boolean billsDueEnabled;
    private Boolean goalDueEnabled;
    private Boolean emailEnabled;

    @DecimalMin(value = "0", message = "Limite de saldo baixo deve ser >= 0")
    private BigDecimal lowBalanceThreshold;
}
