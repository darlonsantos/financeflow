package com.financeflow.notifications.controller;

import com.financeflow.notifications.dto.NotificationPreferencesRequest;
import com.financeflow.notifications.dto.NotificationPreferencesResponse;
import com.financeflow.notifications.dto.NotificationResponse;
import com.financeflow.notifications.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(defaultValue = "false") boolean refresh,
            @PageableDefault(size = 20) Pageable pageable) {
        if (refresh) {
            notificationService.generateNotificationsForCurrentUser();
        }
        Page<NotificationResponse> notifications = notificationService.findAllByUserId(pageable);
        Map<String, Object> result = new HashMap<>();
        result.put("data", notifications.getContent());
        result.put("pagination", Map.of(
                "page", notifications.getNumber(),
                "size", notifications.getSize(),
                "totalElements", notifications.getTotalElements(),
                "totalPages", notifications.getTotalPages()
        ));
        result.put("unreadCount", notificationService.countUnread());
        result.put("message", "Notificações listadas com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> countUnread() {
        long count = notificationService.countUnread();
        Map<String, Object> result = new HashMap<>();
        result.put("data", Map.of("unreadCount", count));
        result.put("message", "Contagem obtida com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable UUID id) {
        notificationService.markAsRead(id);
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Notificação marcada como lida");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead() {
        notificationService.markAllAsRead();
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Todas as notificações marcadas como lidas");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh() {
        notificationService.generateNotificationsForCurrentUser();
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Notificações atualizadas");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/preferences")
    public ResponseEntity<Map<String, Object>> getPreferences() {
        NotificationPreferencesResponse prefs = notificationService.getPreferences();
        Map<String, Object> result = new HashMap<>();
        result.put("data", prefs);
        result.put("message", "Preferências obtidas com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/preferences")
    public ResponseEntity<Map<String, Object>> updatePreferences(
            @Valid @RequestBody NotificationPreferencesRequest request) {
        NotificationPreferencesResponse prefs = notificationService.updatePreferences(request);
        Map<String, Object> result = new HashMap<>();
        result.put("data", prefs);
        result.put("message", "Preferências atualizadas com sucesso");
        result.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.ok(result);
    }
}
