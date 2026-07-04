package com.financeflow.taxacambio.controller;

import com.financeflow.taxacambio.dto.TaxaCambioResumoResponse;
import com.financeflow.taxacambio.dto.TaxaCambioResponse;
import com.financeflow.taxacambio.service.TaxaCambioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/taxa-cambio")
@RequiredArgsConstructor
public class TaxaCambioController {

    private final TaxaCambioService taxaCambioService;

    @GetMapping("/resumo")
    public ResponseEntity<Map<String, Object>> getResumo() {
        TaxaCambioResumoResponse resumo = taxaCambioService.getResumo();
        Map<String, Object> result = new HashMap<>();
        result.put("data", resumo);
        result.put("message", "Resumo de taxas de câmbio");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/historico")
    public ResponseEntity<Map<String, Object>> getHistorico(
            @RequestParam(required = false) List<String> moedas,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        size = Math.min(Math.max(1, size), 50);
        Page<TaxaCambioResponse> historico = taxaCambioService.getHistorico(moedas, dataInicio, dataFim, PageRequest.of(page, size));
        Map<String, Object> result = new HashMap<>();
        result.put("data", historico.getContent());
        result.put("totalElements", historico.getTotalElements());
        result.put("totalPages", historico.getTotalPages());
        result.put("message", "Histórico de taxas de câmbio");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/historico/grafico")
    public ResponseEntity<Map<String, Object>> getHistoricoGrafico(
            @RequestParam(required = false, defaultValue = "USD") String moeda,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        List<TaxaCambioResponse> list = taxaCambioService.getHistoricoGrafico(moeda, dataInicio, dataFim);
        Map<String, Object> result = new HashMap<>();
        result.put("data", list);
        result.put("message", "Histórico para gráfico");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/atualizar")
    public ResponseEntity<Map<String, Object>> atualizarAgora() {
        taxaCambioService.atualizarAgora();
        TaxaCambioResumoResponse resumo = taxaCambioService.getResumo();
        Map<String, Object> result = new HashMap<>();
        result.put("data", resumo);
        result.put("message", "Taxas atualizadas com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }
}
