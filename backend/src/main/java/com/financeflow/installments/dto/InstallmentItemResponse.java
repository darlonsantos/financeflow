package com.financeflow.installments.dto;

import com.financeflow.installments.domain.InstallmentItem;
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
public class InstallmentItemResponse {

    private UUID id;
    private UUID installmentGroupId;
    private Integer installmentNumber;
    private LocalDate dueDate;
    private BigDecimal amount;
    private InstallmentItem.InstallmentItemStatus status;
    private UUID transactionId;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
