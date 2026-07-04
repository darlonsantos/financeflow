package com.financeflow.budgets.exception;

import java.util.UUID;

public class BudgetNotFoundException extends RuntimeException {

    public BudgetNotFoundException(UUID id) {
        super("Orçamento não encontrado: " + id);
    }
}
