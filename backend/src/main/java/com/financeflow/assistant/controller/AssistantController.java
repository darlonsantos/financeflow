package com.financeflow.assistant.controller;

import com.financeflow.assistant.dto.AssistantChatRequest;
import com.financeflow.assistant.dto.AssistantChatResponse;
import com.financeflow.assistant.service.FinancialAssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final FinancialAssistantService financialAssistantService;

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@Valid @RequestBody AssistantChatRequest request) {
        AssistantChatResponse response = financialAssistantService.processMessage(request);
        Map<String, Object> result = new HashMap<>();
        result.put("data", response);
        result.put("message", "Resposta gerada com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }
}
