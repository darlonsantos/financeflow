package com.financeflow.accounts.exception;

import java.util.UUID;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String message) {
        super(message);
    }
    
    public AccountNotFoundException(UUID accountId) {
        super("Conta não encontrada: " + accountId);
    }
}
