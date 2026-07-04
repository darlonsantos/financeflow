package com.financeflow.installments.exception;

import java.util.UUID;

public class InstallmentNotFoundException extends RuntimeException {

    public InstallmentNotFoundException(UUID id) {
        super("Parcelamento não encontrado: " + id);
    }
}
