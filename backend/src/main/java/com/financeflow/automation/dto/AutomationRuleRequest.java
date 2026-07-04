package com.financeflow.automation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutomationRuleRequest {

    @NotBlank(message = "Nome da regra é obrigatório")
    private String name;

    @NotNull
    @Builder.Default
    private Boolean active = true;

    @NotBlank(message = "Tipo da condição é obrigatório")
    private String conditionType;

    @NotNull(message = "Configuração da condição é obrigatória")
    private Map<String, Object> conditionConfig;

    @NotBlank(message = "Tipo da ação é obrigatório")
    private String actionType;

    private Map<String, Object> actionConfig;
}
