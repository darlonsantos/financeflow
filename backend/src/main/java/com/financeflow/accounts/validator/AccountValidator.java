package com.financeflow.accounts.validator;

import com.financeflow.accounts.domain.Account;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Validador de regras de negócio para contas.
 */
@Component
public class AccountValidator {
    
    /**
     * Valida se o saldo inicial é válido.
     * 
     * @param initialBalance Saldo inicial
     * @throws IllegalArgumentException se o saldo for inválido
     */
    public void validateInitialBalance(BigDecimal initialBalance) {
        if (initialBalance == null) {
            throw new IllegalArgumentException("Saldo inicial não pode ser nulo");
        }
    }
    
    /**
     * Valida se uma conta pode ser excluída.
     * Uma conta não pode ser excluída se tiver transações ativas.
     * 
     * @param account Conta a ser validada
     * @param hasActiveTransactions true se a conta tem transações ativas
     * @throws IllegalStateException se a conta tem transações ativas
     */
    public void validateCanDelete(Account account, boolean hasActiveTransactions) {
        if (hasActiveTransactions) {
            throw new IllegalStateException(
                String.format("Não é possível excluir conta '%s' com transações ativas", 
                    account.getName())
            );
        }
    }
    
    /**
     * Valida se o nome da conta é válido.
     * 
     * @param name Nome da conta
     * @throws IllegalArgumentException se o nome for inválido
     */
    public void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome da conta não pode ser vazio");
        }
        if (name.length() > 255) {
            throw new IllegalArgumentException("Nome da conta deve ter no máximo 255 caracteres");
        }
    }
}
