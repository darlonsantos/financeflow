package com.financeflow.budgets.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetResponse {

    private UUID id;
    private UUID categoryId;
    private String categoryName;
    private String categoryColor;
    private LocalDate month;
    private BigDecimal limitAmount;
    private BigDecimal spentAmount;
    private BigDecimal percentUsed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
