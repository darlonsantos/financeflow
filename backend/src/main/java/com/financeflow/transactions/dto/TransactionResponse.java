package com.financeflow.transactions.dto;

import com.financeflow.transactions.domain.Transaction;
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
public class TransactionResponse {
    
    private UUID id;
    private UUID accountId;
    private String accountName;
    private UUID categoryId;
    private String categoryName;
    private BigDecimal amount;
    private Transaction.TransactionType type;
    private LocalDate date;
    private LocalDate dueDate;
    private String description;
    private List<String> tags;
    private Boolean recurring;
    private String recurringPattern;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String clientId;
    /** Código da moeda da conta (ex: BRL, USD). O valor amount está nesta moeda. */
    private String currencyCode;
    
    /**
     * @deprecated Use {@link com.financeflow.transactions.mapper.TransactionMapper#toResponse(Transaction)} instead
     */
    @Deprecated
    public static TransactionResponse fromEntity(Transaction transaction) {
        return TransactionResponse.builder()
            .id(transaction.getId())
            .accountId(transaction.getAccount().getId())
            .accountName(transaction.getAccount().getName())
            .categoryId(transaction.getCategory().getId())
            .categoryName(transaction.getCategory().getName())
            .amount(transaction.getAmount())
            .type(transaction.getType())
            .date(transaction.getDate())
            .dueDate(transaction.getDueDate())
            .description(transaction.getDescription())
            .tags(transaction.getTags())
            .recurring(transaction.getRecurring())
            .recurringPattern(transaction.getRecurringPattern())
            .createdAt(transaction.getCreatedAt())
            .updatedAt(transaction.getUpdatedAt())
            .clientId(transaction.getClientId())
            .build();
    }
}
