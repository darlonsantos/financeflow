package com.financeflow.openfinance.client;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PluggyTransactionData(
    String id,
    String description,
    BigDecimal amount,
    LocalDate date,
    String category
) {}
