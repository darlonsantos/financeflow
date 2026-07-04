package com.financeflow.budgets.controller;

import com.financeflow.budgets.dto.BudgetRequest;
import com.financeflow.budgets.dto.BudgetResponse;
import com.financeflow.budgets.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> findAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {
        List<BudgetResponse> budgets = budgetService.findAll(month);
        Map<String, Object> result = new HashMap<>();
        result.put("data", budgets);
        result.put("message", "Orçamentos listados com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> findById(@PathVariable UUID id) {
        BudgetResponse budget = budgetService.findById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("data", budget);
        result.put("message", "Orçamento encontrado com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody BudgetRequest request) {
        BudgetResponse budget = budgetService.create(request);
        Map<String, Object> result = new HashMap<>();
        result.put("data", budget);
        result.put("message", "Orçamento criado com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable UUID id,
            @Valid @RequestBody BudgetRequest request) {
        BudgetResponse budget = budgetService.update(id, request);
        Map<String, Object> result = new HashMap<>();
        result.put("data", budget);
        result.put("message", "Orçamento atualizado com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable UUID id) {
        budgetService.delete(id);
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Orçamento excluído com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }
}
