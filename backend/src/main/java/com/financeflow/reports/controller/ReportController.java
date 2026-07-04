package com.financeflow.reports.controller;

import com.financeflow.reports.dto.AIReportResponse;
import com.financeflow.reports.service.AIReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final AIReportService aiReportService;

    @GetMapping("/ai")
    public ResponseEntity<Map<String, Object>> generateAIReport() {
        AIReportResponse report = aiReportService.generateReport();
        return ResponseEntity.ok(Map.of(
                "data", report,
                "message", "Relatório gerado com sucesso",
                "timestamp", java.time.Instant.now().toString()
        ));
    }
}
