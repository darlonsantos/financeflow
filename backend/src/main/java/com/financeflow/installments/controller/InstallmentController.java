package com.financeflow.installments.controller;

import com.financeflow.installments.dto.*;
import com.financeflow.installments.domain.InstallmentGroup;
import com.financeflow.installments.service.InstallmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/installments")
@RequiredArgsConstructor
public class InstallmentController {

    private final InstallmentService installmentService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) InstallmentGroup.InstallmentGroupStatus status,
            @RequestParam(required = false) InstallmentGroup.InstallmentType installmentType,
            @PageableDefault(size = 20, sort = "firstDueDate") Pageable pageable) {
        Page<InstallmentGroupResponse> page = installmentService.findAll(pageable, search, status, installmentType);
        Map<String, Object> result = new HashMap<>();
        result.put("data", page.getContent());
        result.put("pagination", Map.of(
            "page", page.getNumber(),
            "size", page.getSize(),
            "totalElements", page.getTotalElements(),
            "totalPages", page.getTotalPages()
        ));
        result.put("message", "Parcelamentos listados com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable UUID id) {
        InstallmentGroupResponse group = installmentService.findById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("data", group);
        result.put("message", "Parcelamento encontrado com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody InstallmentGroupRequest request) {
        InstallmentGroupResponse created = installmentService.create(request);
        Map<String, Object> result = new HashMap<>();
        result.put("data", created);
        result.put("message", "Parcelamento criado com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/pay")
    public ResponseEntity<Map<String, Object>> payInstallment(@Valid @RequestBody PayInstallmentRequest request) {
        InstallmentGroupResponse updated = installmentService.payInstallment(request);
        Map<String, Object> result = new HashMap<>();
        result.put("data", updated);
        result.put("message", "Parcela paga com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/early-settlement")
    public ResponseEntity<Map<String, Object>> earlySettlement(@Valid @RequestBody EarlySettlementRequest request) {
        InstallmentGroupResponse updated = installmentService.earlySettlement(request);
        Map<String, Object> result = new HashMap<>();
        result.put("data", updated);
        result.put("message", "Quitação antecipada realizada com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/renegotiate")
    public ResponseEntity<Map<String, Object>> renegotiate(@Valid @RequestBody RenegotiateRequest request) {
        InstallmentGroupResponse updated = installmentService.renegotiate(request);
        Map<String, Object> result = new HashMap<>();
        result.put("data", updated);
        result.put("message", "Renegociação realizada com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> cancel(@PathVariable UUID id) {
        installmentService.cancel(id);
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Parcelamento cancelado com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }
}
