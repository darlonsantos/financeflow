package com.financeflow.accounts.calculator;

import com.financeflow.accounts.domain.Account;
import com.financeflow.transactions.domain.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Calculadora de saldo de contas.
 * Separa a lógica de cálculo de saldo do TransactionService.
 */
@Component
public class AccountBalanceCalculator {
    
    /**
     * Calcula a mudança de saldo baseada no tipo de transação.
     * 
     * @param amount Valor da transação
     * @param type Tipo da transação (INCOME ou EXPENSE)
     * @param isAdd true se está adicionando, false se está removendo
     * @return Mudança no saldo (positivo para receita, negativo para despesa)
     */
    public BigDecimal calculateBalanceChange(BigDecimal amount, Transaction.TransactionType type, boolean isAdd) {
        BigDecimal change;
        
        if (type == Transaction.TransactionType.INCOME) {
            change = isAdd ? amount : amount.negate();
        } else { // EXPENSE
            change = isAdd ? amount.negate() : amount;
        }
        
        return change;
    }
    
    /**
     * Atualiza o saldo da conta com base na mudança calculada.
     * 
     * @param account Conta a ser atualizada
     * @param balanceChange Mudança no saldo (pode ser positivo ou negativo)
     */
    public void updateBalance(Account account, BigDecimal balanceChange) {
        BigDecimal newBalance = account.getBalance().add(balanceChange);
        account.setBalance(newBalance);
    }
    
    /**
     * Recalcula o saldo da conta baseado em todas as transações.
     * 
     * @param account Conta para recalcular
     * @param transactions Lista de transações da conta
     * @return Novo saldo calculado
     */
    public BigDecimal recalculateBalance(Account account, java.util.List<Transaction> transactions) {
        BigDecimal balance = account.getInitialBalance();
        
        for (Transaction transaction : transactions) {
            if (transaction.getDeletedAt() == null) {
                BigDecimal change = calculateBalanceChange(
                    transaction.getAmount(), 
                    transaction.getType(), 
                    true
                );
                balance = balance.add(change);
            }
        }
        
        return balance;
    }
    
    /**
     * Calcula a diferença de saldo quando o saldo inicial é alterado.
     * 
     * @param oldInitialBalance Saldo inicial antigo
     * @param newInitialBalance Novo saldo inicial
     * @return Diferença a ser aplicada ao saldo atual
     */
    public BigDecimal calculateInitialBalanceDifference(BigDecimal oldInitialBalance, BigDecimal newInitialBalance) {
        return newInitialBalance.subtract(oldInitialBalance);
    }
}
