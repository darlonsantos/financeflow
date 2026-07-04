package com.financeflow.accounts.controller;

import com.financeflow.accounts.domain.AccountShare;
import com.financeflow.accounts.dto.AccountShareRequest;
import com.financeflow.accounts.dto.AccountShareResponse;
import com.financeflow.accounts.service.AccountShareService;
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
public class AccountShareController {

    private final AccountShareService accountShareService;

    @PostMapping("/{accountId}/shares")
    public ResponseEntity<Map<String, Object>> shareAccount(
            @PathVariable UUID accountId,
            @Valid @RequestBody AccountShareRequest request) {
        AccountShareResponse share = accountShareService.shareAccount(accountId, request);
        Map<String, Object> result = new HashMap<>();
        result.put("data", share);
        result.put("message", "Conta compartilhada com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/{accountId}/shares")
    public ResponseEntity<Map<String, Object>> listShares(@PathVariable UUID accountId) {
        List<AccountShareResponse> shares = accountShareService.listShares(accountId);
        Map<String, Object> result = new HashMap<>();
        result.put("data", shares);
        result.put("message", "Compartilhamentos listados com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{accountId}/shares/{sharedWithUserId}")
    public ResponseEntity<Map<String, Object>> updatePermission(
            @PathVariable UUID accountId,
            @PathVariable UUID sharedWithUserId,
            @RequestParam AccountShare.Permission permission) {
        AccountShareResponse share = accountShareService.updatePermission(accountId, sharedWithUserId, permission);
        Map<String, Object> result = new HashMap<>();
        result.put("data", share);
        result.put("message", "Permissão atualizada com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{accountId}/shares/{sharedWithUserId}")
    public ResponseEntity<Map<String, Object>> revokeShare(
            @PathVariable UUID accountId,
            @PathVariable UUID sharedWithUserId) {
        accountShareService.revokeShare(accountId, sharedWithUserId);
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Compartilhamento removido com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }
}
