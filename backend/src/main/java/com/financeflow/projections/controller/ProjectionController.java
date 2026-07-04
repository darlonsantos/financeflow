package com.financeflow.projections.controller;

import com.financeflow.projections.dto.BalanceProjectionResponse;
import com.financeflow.projections.service.BalanceProjectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/projections")
@RequiredArgsConstructor
public class ProjectionController {

    private final BalanceProjectionService balanceProjectionService;

    @GetMapping("/balance")
    public ResponseEntity<Map<String, Object>> getBalanceProjection(
            @RequestParam(defaultValue = "12") int months) {
        BalanceProjectionResponse projection = balanceProjectionService.projectBalance(months);
        Map<String, Object> result = new HashMap<>();
        result.put("data", projection);
        result.put("message", "Projeção de saldo calculada com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }
}
