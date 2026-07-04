package com.financeflow.openfinance.client;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PluggyAccountData(
    String id,
    String type,
    String name,
    BigDecimal balance,
    String subtype,
    String currencyCode,
    BigDecimal minimumPayment,
    LocalDate balanceDueDate,
    LocalDate balanceCloseDate,
    BigDecimal availableCreditLimit,
    BigDecimal creditLimit
) {}
