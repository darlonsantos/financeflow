package com.financeflow.categories.exception;

import java.util.UUID;

public class CategoryNotFoundException extends RuntimeException {
    public CategoryNotFoundException(String message) {
        super(message);
    }
    
    public CategoryNotFoundException(UUID categoryId) {
        super("Categoria não encontrada: " + categoryId);
    }
}
