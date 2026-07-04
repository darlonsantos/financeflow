package com.financeflow.api.controller;

import com.financeflow.api.config.ApiVersionConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller para informações sobre versionamento da API.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiVersionController {
    
    private final ApiVersionConfig apiVersionConfig;
    
    /**
     * Retorna informações sobre versões disponíveis da API.
     */
    @GetMapping("/versions")
    public ResponseEntity<Map<String, Object>> getApiVersions() {
        Map<String, Object> response = new HashMap<>();
        response.put("currentVersion", apiVersionConfig.getCurrentVersion());
        response.put("defaultVersion", apiVersionConfig.getDefaultVersion());
        response.put("latestVersion", apiVersionConfig.getLatestVersion());
        response.put("supportedVersions", apiVersionConfig.getSupportedVersionsList());
        
        Map<String, Object> versions = new HashMap<>();
        versions.put("v1", Map.of(
            "status", "current",
            "deprecated", false,
            "sunsetDate", null
        ));
        // Preparar para v2 quando necessário
        // versions.put("v2", Map.of(...));
        
        response.put("versions", versions);
        response.put("message", "Versões da API disponíveis");
        response.put("timestamp", java.time.Instant.now().toString());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Health check específico para versionamento.
     */
    @GetMapping("/version")
    public ResponseEntity<Map<String, Object>> getCurrentVersion() {
        Map<String, Object> response = new HashMap<>();
        response.put("version", apiVersionConfig.getCurrentVersion());
        response.put("latestVersion", apiVersionConfig.getLatestVersion());
        response.put("timestamp", java.time.Instant.now().toString());
        
        return ResponseEntity.ok(response);
    }
}
