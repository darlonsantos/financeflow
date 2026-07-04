package com.financeflow.installments.dto;

import com.financeflow.installments.domain.InstallmentGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstallmentGroupResponse {

    private UUID id;
    private UUID accountId;
    private String accountName;
    private UUID categoryId;
    private String categoryName;
    private String description;
    private BigDecimal totalAmount;
    private InstallmentGroup.InstallmentType installmentType;
    private InstallmentGroup.InstallmentGroupStatus status;
    private LocalDate firstDueDate;
    private Integer numberOfInstallments;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private Integer paidCount;
    private Integer pendingCount;
    private List<InstallmentItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String currencyCode;
}
