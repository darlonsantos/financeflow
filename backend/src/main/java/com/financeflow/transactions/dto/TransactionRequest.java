package com.financeflow.transactions.dto;

import com.financeflow.transactions.domain.Transaction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {
    
    @NotNull(message = "Conta é obrigatória")
    private UUID accountId;
    
    @NotNull(message = "Categoria é obrigatória")
    private UUID categoryId;
    
    @NotNull(message = "Valor é obrigatório")
    @Positive(message = "Valor deve ser positivo")
    private BigDecimal amount;
    
    @NotNull(message = "Tipo é obrigatório")
    private Transaction.TransactionType type;
    
    @NotNull(message = "Data é obrigatória")
    private LocalDate date;
    
    /** Data de vencimento (opcional). Ex.: para contas a pagar. */
    private LocalDate dueDate;
    
    @Size(max = 1000, message = "Descrição deve ter no máximo 1000 caracteres")
    private String description;
    
    private List<String> tags;
    
    @Builder.Default
    private Boolean recurring = false;
    
    @Size(max = 50, message = "Padrão de recorrência deve ter no máximo 50 caracteres")
    private String recurringPattern;
    
    private String clientId;
}
