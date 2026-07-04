package com.financeflow.automation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutomationRuleResponse {

    private UUID id;
    private String name;
    private Boolean active;
    private String conditionType;
    private Map<String, Object> conditionConfig;
    private String actionType;
    private Map<String, Object> actionConfig;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
