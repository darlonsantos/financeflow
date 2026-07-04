package com.financeflow.transfers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferListItemResponse {

    private UUID id;
    private String originAccountName;
    private String destinationAccountName;
    private LocalDate transferDate;
    private BigDecimal amount;
    private String description;
}
