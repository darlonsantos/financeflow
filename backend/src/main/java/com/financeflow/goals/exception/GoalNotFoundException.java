package com.financeflow.goals.exception;

import java.util.UUID;

public class GoalNotFoundException extends RuntimeException {

    public GoalNotFoundException(UUID id) {
        super("Meta não encontrada: " + id);
    }
}
