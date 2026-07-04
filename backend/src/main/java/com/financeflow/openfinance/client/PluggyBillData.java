package com.financeflow.openfinance.client;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PluggyBillData(
    String id,
    LocalDate dueDate,
    BigDecimal totalAmount,
    String currencyCode,
    BigDecimal minimumPaymentAmount
) {}
