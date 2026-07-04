package com.financeflow.goals.controller;

import com.financeflow.goals.domain.Goal;
import com.financeflow.goals.dto.GoalContributeRequest;
import com.financeflow.goals.dto.GoalRequest;
import com.financeflow.goals.dto.GoalResponse;
import com.financeflow.goals.service.GoalService;
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
@RequestMapping("/api/v1/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService goalService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> findAll(
            @RequestParam(required = false) Goal.GoalStatus status) {
        List<GoalResponse> goals = goalService.findAll(status);
        Map<String, Object> result = new HashMap<>();
        result.put("data", goals);
        result.put("message", "Metas listadas com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> findById(@PathVariable UUID id) {
        GoalResponse goal = goalService.findById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("data", goal);
        result.put("message", "Meta encontrada com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody GoalRequest request) {
        GoalResponse goal = goalService.create(request);
        Map<String, Object> result = new HashMap<>();
        result.put("data", goal);
        result.put("message", "Meta criada com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable UUID id,
            @Valid @RequestBody GoalRequest request) {
        GoalResponse goal = goalService.update(id, request);
        Map<String, Object> result = new HashMap<>();
        result.put("data", goal);
        result.put("message", "Meta atualizada com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/contribute")
    public ResponseEntity<Map<String, Object>> contribute(
            @PathVariable UUID id,
            @Valid @RequestBody GoalContributeRequest request) {
        GoalResponse goal = goalService.contribute(id, request);
        Map<String, Object> result = new HashMap<>();
        result.put("data", goal);
        result.put("message", "Contribuição registrada com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable UUID id,
            @RequestParam Goal.GoalStatus status) {
        GoalResponse goal = goalService.updateStatus(id, status);
        Map<String, Object> result = new HashMap<>();
        result.put("data", goal);
        result.put("message", "Status atualizado com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable UUID id) {
        goalService.delete(id);
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Meta excluída com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }
}
