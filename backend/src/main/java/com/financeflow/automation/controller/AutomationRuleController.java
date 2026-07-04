package com.financeflow.automation.controller;

import com.financeflow.automation.dto.AutomationRuleRequest;
import com.financeflow.automation.dto.AutomationRuleResponse;
import com.financeflow.automation.service.AutomationRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/automation-rules")
@RequiredArgsConstructor
public class AutomationRuleController {

    private final AutomationRuleService ruleService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> findAll() {
        List<AutomationRuleResponse> rules = ruleService.findAll();
        return ResponseEntity.ok(Map.of(
                "data", rules,
                "message", "Regras listadas com sucesso",
                "timestamp", java.time.Instant.now().toString()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> findById(@PathVariable UUID id) {
        AutomationRuleResponse rule = ruleService.findById(id);
        return ResponseEntity.ok(Map.of(
                "data", rule,
                "message", "Regra encontrada",
                "timestamp", java.time.Instant.now().toString()
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody AutomationRuleRequest request) {
        AutomationRuleResponse rule = ruleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "data", rule,
                "message", "Regra criada com sucesso",
                "timestamp", java.time.Instant.now().toString()
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable UUID id,
            @Valid @RequestBody AutomationRuleRequest request) {
        AutomationRuleResponse rule = ruleService.update(id, request);
        return ResponseEntity.ok(Map.of(
                "data", rule,
                "message", "Regra atualizada com sucesso",
                "timestamp", java.time.Instant.now().toString()
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable UUID id) {
        ruleService.delete(id);
        return ResponseEntity.ok(Map.of(
                "message", "Regra excluída com sucesso",
                "timestamp", java.time.Instant.now().toString()
        ));
    }
}
