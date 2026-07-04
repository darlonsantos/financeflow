package com.financeflow.transactions.validator;

import com.financeflow.categories.domain.Category;
import com.financeflow.transactions.domain.Transaction;
import com.financeflow.transactions.dto.TransactionRequest;
import org.springframework.stereotype.Component;

/**
 * Validador de regras de negócio para transações.
 */
@Component
public class TransactionValidator {
    
    /**
     * Valida se o tipo da transação corresponde ao tipo da categoria.
     * 
     * @param transactionType Tipo da transação
     * @param category Categoria associada
     * @throws IllegalArgumentException se os tipos não correspondem
     */
    public void validateTransactionCategoryType(Transaction.TransactionType transactionType, Category category) {
        if (!transactionType.name().equals(category.getType().name())) {
            throw new IllegalArgumentException(
                String.format("Tipo da transação (%s) não corresponde ao tipo da categoria (%s)",
                    transactionType, category.getType())
            );
        }
    }
    
    /**
     * Valida uma requisição de transação completa.
     * 
     * @param request Requisição de transação
     * @param category Categoria associada
     * @throws IllegalArgumentException se a validação falhar
     */
    public void validate(TransactionRequest request, Category category) {
        validateTransactionCategoryType(request.getType(), category);
    }
    
    /**
     * Valida se o valor da transação é válido.
     * 
     * @param amount Valor da transação
     * @throws IllegalArgumentException se o valor for inválido
     */
    public void validateAmount(java.math.BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Valor da transação não pode ser nulo");
        }
        if (amount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor da transação deve ser maior que zero");
        }
    }
}
