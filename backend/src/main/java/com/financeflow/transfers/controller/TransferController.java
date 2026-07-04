package com.financeflow.transfers.controller;

import com.financeflow.transfers.dto.TransferListItemResponse;
import com.financeflow.transfers.dto.TransferRequest;
import com.financeflow.transfers.dto.TransferResponse;
import com.financeflow.transfers.service.TransferService;
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
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> list() {
        List<TransferListItemResponse> transfers = transferService.list();
        Map<String, Object> result = new HashMap<>();
        result.put("data", transfers);
        result.put("message", "Transferências listadas com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody TransferRequest request) {
        TransferResponse created = transferService.create(request);
        Map<String, Object> result = new HashMap<>();
        result.put("data", created);
        result.put("message", "Transferência realizada com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable UUID id) {
        transferService.deleteTransfer(id);
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Transferência excluída com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }
}
