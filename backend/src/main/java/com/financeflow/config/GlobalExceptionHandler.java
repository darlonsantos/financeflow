package com.financeflow.config;

import lombok.extern.slf4j.Slf4j;
import com.financeflow.accounts.exception.AccountNotFoundException;
import com.financeflow.budgets.exception.BudgetNotFoundException;
import com.financeflow.categories.exception.CategoryNotFoundException;
import com.financeflow.goals.exception.GoalNotFoundException;
import com.financeflow.installments.exception.InstallmentNotFoundException;
import com.financeflow.config.exception.UnauthenticatedException;
import com.financeflow.transactions.exception.TransactionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler({AccountNotFoundException.class, BudgetNotFoundException.class, CategoryNotFoundException.class, GoalNotFoundException.class, TransactionNotFoundException.class, InstallmentNotFoundException.class})
    public ResponseEntity<Map<String, Object>> handleNotFoundException(RuntimeException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", Map.of(
            "code", "NOT_FOUND",
            "message", ex.getMessage()
        ));
        error.put("timestamp", Instant.now().toString());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> error = new HashMap<>();
        Map<String, String> details = new HashMap<>();
        
        ex.getBindingResult().getAllErrors().forEach((err) -> {
            String fieldName = ((FieldError) err).getField();
            String errorMessage = err.getDefaultMessage();
            details.put(fieldName, errorMessage);
        });
        
        error.put("error", Map.of(
            "code", "VALIDATION_ERROR",
            "message", "Dados inválidos",
            "details", details
        ));
        error.put("timestamp", Instant.now().toString());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthenticatedException(UnauthenticatedException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", Map.of(
            "code", "UNAUTHENTICATED",
            "message", ex.getMessage()
        ));
        error.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", Map.of(
            "code", "INVALID_ARGUMENT",
            "message", ex.getMessage()
        ));
        error.put("timestamp", Instant.now().toString());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        String rawMessage = ex.getMostSpecificCause() != null
            ? ex.getMostSpecificCause().getMessage()
            : ex.getMessage();

        String message = "Violação de integridade dos dados";
        if (rawMessage != null &&
            (rawMessage.contains("budgets_user_id_category_id_month_key") ||
                rawMessage.contains("uk_budgets_user_category_month"))) {
            message = "Já existe um orçamento para esta categoria neste mês.";
        }

        Map<String, Object> error = new HashMap<>();
        error.put("error", Map.of(
            "code", "DATA_INTEGRITY_VIOLATION",
            "message", message
        ));
        error.put("timestamp", Instant.now().toString());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", Map.of(
            "code", "RUNTIME_ERROR",
            "message", ex.getMessage()
        ));
        error.put("timestamp", Instant.now().toString());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Erro interno do servidor", ex);
        Map<String, Object> error = new HashMap<>();
        error.put("error", Map.of(
            "code", "INTERNAL_ERROR",
            "message", "Erro interno do servidor"
        ));
        error.put("timestamp", Instant.now().toString());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
