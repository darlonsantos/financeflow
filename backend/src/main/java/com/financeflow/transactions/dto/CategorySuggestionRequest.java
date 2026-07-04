package com.financeflow.transactions.dto;

import com.financeflow.transactions.domain.Transaction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorySuggestionRequest {

    @NotNull(message = "Tipo é obrigatório")
    private Transaction.TransactionType type;

    @Size(max = 1000, message = "Descrição deve ter no máximo 1000 caracteres")
    private String description;
}
