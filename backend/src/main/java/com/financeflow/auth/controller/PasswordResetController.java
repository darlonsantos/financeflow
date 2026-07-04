package com.financeflow.auth.controller;

import com.financeflow.auth.dto.PasswordResetConfirmRequest;
import com.financeflow.auth.dto.PasswordResetRequest;
import com.financeflow.auth.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/password-reset")
@RequiredArgsConstructor
public class PasswordResetController {
    
    private final PasswordResetService passwordResetService;
    
    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request,
            HttpServletRequest httpRequest) {
        try {
            passwordResetService.requestPasswordReset(request, httpRequest);
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Se o email existir, você receberá um link de recuperação");
            result.put("timestamp", java.time.Instant.now().toString());
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", Map.of(
                "code", "PASSWORD_RESET_ERROR",
                "message", e.getMessage()
            ));
            error.put("timestamp", java.time.Instant.now().toString());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    @PostMapping("/confirm")
    public ResponseEntity<Map<String, Object>> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequest request) {
        try {
            passwordResetService.confirmPasswordReset(request);
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Senha redefinida com sucesso");
            result.put("timestamp", java.time.Instant.now().toString());
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", Map.of(
                "code", "PASSWORD_RESET_CONFIRM_ERROR",
                "message", e.getMessage()
            ));
            error.put("timestamp", java.time.Instant.now().toString());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}
