package com.financeflow.openfinance.controller;

import com.financeflow.openfinance.dto.ConnectBankRequest;
import com.financeflow.openfinance.dto.ConfirmConnectionRequest;
import com.financeflow.openfinance.dto.SyncRequest;
import com.financeflow.openfinance.service.OpenFinanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/open-finance")
@RequiredArgsConstructor
public class OpenFinanceController {

    private final OpenFinanceService openFinanceService;

    @PostMapping("/connect")
    public ResponseEntity<Map<String, Object>> connect(@Valid @RequestBody ConnectBankRequest request) {
        Map<String, Object> result = new HashMap<>();
        result.put("data", openFinanceService.connect(request.getProvider()));
        result.put("message", "Conexão bancária criada com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/connections")
    public ResponseEntity<Map<String, Object>> listConnections() {
        Map<String, Object> result = new HashMap<>();
        result.put("data", openFinanceService.listConnections());
        result.put("message", "Conexões bancárias listadas com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/connections/{id}/accounts")
    public ResponseEntity<Map<String, Object>> listAccounts(@PathVariable UUID id) {
        Map<String, Object> result = new HashMap<>();
        result.put("data", openFinanceService.listAccounts(id));
        result.put("message", "Contas bancárias importadas listadas com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/connections/{id}/confirm")
    public ResponseEntity<Map<String, Object>> confirm(
            @PathVariable UUID id,
            @Valid @RequestBody ConfirmConnectionRequest request) {
        Map<String, Object> result = new HashMap<>();
        result.put("data", openFinanceService.confirmConnection(id, request.getProviderConnectionId()));
        result.put("message", "Conexão Pluggy confirmada com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/connections/{id}/transactions")
    public ResponseEntity<Map<String, Object>> listTransactions(@PathVariable UUID id) {
        Map<String, Object> result = new HashMap<>();
        result.put("data", openFinanceService.listImportedTransactions(id));
        result.put("message", "Transações importadas listadas com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/connections/{id}/credit-summary")
    public ResponseEntity<Map<String, Object>> creditSummary(@PathVariable UUID id) {
        Map<String, Object> result = new HashMap<>();
        result.put("data", openFinanceService.listCreditCardSummary(id));
        result.put("message", "Resumo de fatura de cartão listado com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/connections/{id}/history")
    public ResponseEntity<Map<String, Object>> listHistory(@PathVariable UUID id) {
        Map<String, Object> result = new HashMap<>();
        result.put("data", openFinanceService.listHistory(id));
        result.put("message", "Histórico de sincronização listado com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/connections/{id}/sync")
    public ResponseEntity<Map<String, Object>> sync(@PathVariable UUID id, @RequestBody(required = false) SyncRequest request) {
        SyncRequest body = request == null ? new SyncRequest() : request;
        Map<String, Object> result = new HashMap<>();
        result.put("data", openFinanceService.syncConnection(id, body.getDateFrom(), body.getDateTo()));
        result.put("message", "Sincronização executada com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/connections/{id}/revoke")
    public ResponseEntity<Map<String, Object>> revoke(@PathVariable UUID id) {
        openFinanceService.revokeConnection(id);
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Conexão revogada com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }
}
