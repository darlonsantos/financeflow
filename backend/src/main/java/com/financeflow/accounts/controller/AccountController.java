package com.financeflow.accounts.controller;

import com.financeflow.accounts.dto.AccountRequest;
import com.financeflow.accounts.dto.AccountResponse;
import com.financeflow.accounts.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {
    
    private final AccountService accountService;
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> findAll() {
        List<AccountResponse> accounts = accountService.findAll();
        Map<String, Object> result = new HashMap<>();
        result.put("data", accounts);
        result.put("message", "Contas listadas com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> findById(@PathVariable UUID id) {
        AccountResponse account = accountService.findById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("data", account);
        result.put("message", "Conta encontrada com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody AccountRequest request) {
        AccountResponse account = accountService.create(request);
        Map<String, Object> result = new HashMap<>();
        result.put("data", account);
        result.put("message", "Conta criada com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable UUID id,
            @Valid @RequestBody AccountRequest request) {
        AccountResponse account = accountService.update(id, request);
        Map<String, Object> result = new HashMap<>();
        result.put("data", account);
        result.put("message", "Conta atualizada com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable UUID id) {
        accountService.delete(id);
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Conta excluída com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/{id}/balance")
    public ResponseEntity<Map<String, Object>> getBalance(@PathVariable UUID id) {
        AccountResponse account = accountService.getBalance(id);
        Map<String, Object> result = new HashMap<>();
        result.put("data", account);
        result.put("message", "Saldo calculado com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }
}
