package com.financeflow.currency.controller;

import com.financeflow.currency.dto.CurrencyRateRequest;
import com.financeflow.currency.dto.CurrencyRateResponse;
import com.financeflow.currency.dto.CurrencyResponse;
import com.financeflow.currency.service.CurrencyConversionService;
import com.financeflow.currency.service.CurrencyRateService;
import com.financeflow.currency.service.CurrencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CurrencyController {

    private final CurrencyService currencyService;
    private final CurrencyRateService currencyRateService;
    private final CurrencyConversionService conversionService;

    @GetMapping("/currencies")
    public ResponseEntity<Map<String, Object>> listCurrencies() {
        List<CurrencyResponse> list = currencyService.findAll();
        Map<String, Object> result = new HashMap<>();
        result.put("data", list);
        result.put("message", "Moedas listadas com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/currencies/{code}")
    public ResponseEntity<Map<String, Object>> getCurrency(@PathVariable String code) {
        CurrencyResponse currency = currencyService.findByCode(code);
        if (currency == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", Map.of("code", "NOT_FOUND", "message", "Moeda não encontrada"));
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("data", currency);
        result.put("message", "Moeda encontrada");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/currency-rates")
    public ResponseEntity<Map<String, Object>> listRates(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        List<CurrencyRateResponse> list = currencyRateService.findAllRates(from, to);
        Map<String, Object> result = new HashMap<>();
        result.put("data", list);
        result.put("message", "Taxas listadas com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/currency-rates/latest")
    public ResponseEntity<Map<String, Object>> getLatestRate(
            @RequestParam String from,
            @RequestParam String to) {
        CurrencyRateResponse rate = currencyRateService.getLatestRate(from, to);
        if (rate == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", Map.of("code", "NOT_FOUND", "message", "Taxa não configurada para o par de moedas"));
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("data", rate);
        result.put("message", "Taxa encontrada");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/currency-rates")
    public ResponseEntity<Map<String, Object>> createOrUpdateRate(@Valid @RequestBody CurrencyRateRequest request) {
        CurrencyRateResponse rate = currencyRateService.createOrUpdateRate(request);
        Map<String, Object> result = new HashMap<>();
        result.put("data", rate);
        result.put("message", "Taxa registrada com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @DeleteMapping("/currency-rates")
    public ResponseEntity<Map<String, Object>> deleteRates(
            @RequestParam String from,
            @RequestParam String to) {
        currencyRateService.deleteRates(from, to);
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Taxas removidas com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/currency-rates/convert")
    public ResponseEntity<Map<String, Object>> convert(
            @RequestParam BigDecimal amount,
            @RequestParam String from,
            @RequestParam String to) {
        java.util.Optional<BigDecimal> converted = conversionService.convert(amount, from, to);
        Map<String, Object> result = new HashMap<>();
        if (converted.isEmpty()) {
            result.put("error", Map.of("code", "CONVERSION_UNAVAILABLE", "message", "Taxa não disponível para o par de moedas"));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
        result.put("data", Map.of(
            "fromCurrency", from,
            "toCurrency", to,
            "originalAmount", amount,
            "convertedAmount", converted.get()
        ));
        result.put("message", "Conversão realizada");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }
}
