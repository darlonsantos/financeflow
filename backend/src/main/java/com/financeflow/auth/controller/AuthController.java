package com.financeflow.auth.controller;

import com.financeflow.auth.dto.AuthResponse;
import com.financeflow.auth.dto.LoginRequest;
import com.financeflow.auth.dto.RefreshTokenRequest;
import com.financeflow.auth.dto.RegisterRequest;
import com.financeflow.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UUID)) {
            throw new RuntimeException("Usuário não autenticado");
        }
        return (UUID) authentication.getPrincipal();
    }
    
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        try {
            AuthResponse response = authService.register(request, httpRequest);
            Map<String, Object> result = new HashMap<>();
            result.put("data", response);
            result.put("message", "Usuário criado com sucesso");
            result.put("timestamp", java.time.Instant.now().toString());
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", Map.of(
                "code", "REGISTRATION_ERROR",
                "message", e.getMessage()
            ));
            error.put("timestamp", java.time.Instant.now().toString());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        try {
            AuthResponse response = authService.login(request, httpRequest);
            Map<String, Object> result = new HashMap<>();
            result.put("data", response);
            result.put("message", "Login realizado com sucesso");
            result.put("timestamp", java.time.Instant.now().toString());
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", Map.of(
                "code", "AUTHENTICATION_ERROR",
                "message", e.getMessage()
            ));
            error.put("timestamp", java.time.Instant.now().toString());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            AuthResponse response = authService.refreshToken(request.getRefreshToken());
            Map<String, Object> result = new HashMap<>();
            result.put("data", response);
            result.put("message", "Token renovado com sucesso");
            result.put("timestamp", java.time.Instant.now().toString());
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", Map.of(
                "code", "REFRESH_TOKEN_ERROR",
                "message", e.getMessage()
            ));
            error.put("timestamp", java.time.Instant.now().toString());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            authService.logout(request.getRefreshToken());
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Logout realizado com sucesso");
            result.put("timestamp", java.time.Instant.now().toString());
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", Map.of(
                "code", "LOGOUT_ERROR",
                "message", e.getMessage()
            ));
            error.put("timestamp", java.time.Instant.now().toString());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, Object>> logoutAll() {
        try {
            UUID userId = getCurrentUserId();
            authService.logoutAll(userId);
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Logout de todos os dispositivos realizado com sucesso");
            result.put("timestamp", java.time.Instant.now().toString());
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", Map.of(
                "code", "LOGOUT_ERROR",
                "message", e.getMessage()
            ));
            error.put("timestamp", java.time.Instant.now().toString());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}
