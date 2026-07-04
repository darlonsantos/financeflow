package com.financeflow.config.exception;

/**
 * Lançada quando o usuário não está autenticado ao acessar recurso protegido.
 * O GlobalExceptionHandler retorna 401 Unauthorized (não 400 Bad Request).
 */
public class UnauthenticatedException extends RuntimeException {

    public UnauthenticatedException(String message) {
        super(message);
    }
}
