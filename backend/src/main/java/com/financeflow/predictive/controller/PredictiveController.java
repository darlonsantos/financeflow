package com.financeflow.predictive.controller;

import com.financeflow.predictive.dto.PredictiveReportResponse;
import com.financeflow.predictive.service.PredictiveIntelligenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * API da inteligência financeira preditiva: análise de riscos e alertas preventivos.
 */
@RestController
@RequestMapping("/api/v1/predictive")
@RequiredArgsConstructor
public class PredictiveController {

    private final PredictiveIntelligenceService predictiveIntelligenceService;

    @GetMapping("/report")
    public ResponseEntity<Map<String, Object>> getPredictiveReport() {
        PredictiveReportResponse report = predictiveIntelligenceService.generateReport();
        Map<String, Object> body = new HashMap<>();
        body.put("data", report);
        body.put("message", "Relatório preditivo gerado com sucesso");
        body.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(body);
    }
}
