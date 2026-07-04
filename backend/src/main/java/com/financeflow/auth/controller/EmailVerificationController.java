package com.financeflow.auth.controller;

import com.financeflow.auth.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/email-verification")
@RequiredArgsConstructor
public class EmailVerificationController {
    
    private final EmailVerificationService emailVerificationService;
    
    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UUID)) {
            throw new RuntimeException("Usuário não autenticado");
        }
        return (UUID) authentication.getPrincipal();
    }
    
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyEmail(@RequestParam String token) {
        try {
            emailVerificationService.verifyEmail(token);
            Map<String, Object> result = new HashMap<>();
            result.put("message", "E-mail verificado com sucesso");
            result.put("timestamp", java.time.Instant.now().toString());
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", Map.of(
                "code", "EMAIL_VERIFICATION_ERROR",
                "message", e.getMessage()
            ));
            error.put("timestamp", java.time.Instant.now().toString());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    @PostMapping("/resend")
    public ResponseEntity<Map<String, Object>> resendVerificationEmail(@RequestParam(required = false) String email) {
        try {
            UUID userId;
            if (email != null && !email.isEmpty()) {
                // Endpoint público - buscar usuário por email
                userId = emailVerificationService.getUserIdByEmail(email);
            } else {
                // Endpoint autenticado - usar usuário atual
                userId = getCurrentUserId();
            }
            emailVerificationService.resendVerificationEmail(userId);
            Map<String, Object> result = new HashMap<>();
            result.put("message", "E-mail de verificação reenviado com sucesso");
            result.put("timestamp", java.time.Instant.now().toString());
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", Map.of(
                "code", "EMAIL_VERIFICATION_RESEND_ERROR",
                "message", e.getMessage()
            ));
            error.put("timestamp", java.time.Instant.now().toString());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}
