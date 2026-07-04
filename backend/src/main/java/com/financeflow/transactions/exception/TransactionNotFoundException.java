package com.financeflow.transactions.exception;

import java.util.UUID;

public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException(String message) {
        super(message);
    }
    
    public TransactionNotFoundException(UUID transactionId) {
        super("Transação não encontrada: " + transactionId);
    }
}
