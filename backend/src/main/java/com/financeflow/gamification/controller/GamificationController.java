package com.financeflow.gamification.controller;

import com.financeflow.gamification.dto.GamificationSummaryResponse;
import com.financeflow.gamification.service.GamificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/gamification")
@RequiredArgsConstructor
public class GamificationController {

    private final GamificationService gamificationService;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        GamificationSummaryResponse summary = gamificationService.getSummary();
        Map<String, Object> result = new HashMap<>();
        result.put("data", summary);
        result.put("message", "Resumo de gamificação obtido com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }
}
