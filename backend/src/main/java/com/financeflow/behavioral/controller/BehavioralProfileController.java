package com.financeflow.behavioral.controller;

import com.financeflow.behavioral.dto.BehavioralProfileResponse;
import com.financeflow.behavioral.service.BehavioralProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * API do perfil financeiro comportamental (classificação por IA ou regras).
 */
@RestController
@RequestMapping("/api/v1/behavioral-profile")
@RequiredArgsConstructor
public class BehavioralProfileController {

    private final BehavioralProfileService behavioralProfileService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getProfile() {
        BehavioralProfileResponse profile = behavioralProfileService.getProfile();
        Map<String, Object> body = new HashMap<>();
        body.put("data", profile);
        body.put("message", "Perfil comportamental gerado com sucesso");
        body.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(body);
    }
}
