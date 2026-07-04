package com.financeflow.notifications.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferencesResponse {

    private UUID id;
    private Boolean budgetExceededEnabled;
    private Boolean lowBalanceEnabled;
    private Boolean billsDueEnabled;
    private Boolean goalDueEnabled;
    private Boolean emailEnabled;
    private BigDecimal lowBalanceThreshold;
}
