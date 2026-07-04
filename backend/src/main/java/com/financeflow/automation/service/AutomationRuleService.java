package com.financeflow.automation.service;

import com.financeflow.automation.domain.AutomationRule;
import com.financeflow.automation.dto.AutomationRuleRequest;
import com.financeflow.automation.dto.AutomationRuleResponse;
import com.financeflow.automation.repository.AutomationRuleRepository;
import com.financeflow.users.domain.User;
import com.financeflow.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AutomationRuleService {

    private final AutomationRuleRepository ruleRepository;
    private final UserRepository userRepository;

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UUID)) {
            throw new RuntimeException("Usuário não autenticado");
        }
        return (UUID) auth.getPrincipal();
    }

    @Transactional(readOnly = true)
    public List<AutomationRuleResponse> findAll() {
        UUID userId = getCurrentUserId();
        return ruleRepository.findAllByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AutomationRuleResponse findById(UUID id) {
        UUID userId = getCurrentUserId();
        AutomationRule rule = ruleRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Regra não encontrada"));
        return toResponse(rule);
    }

    @Transactional
    public AutomationRuleResponse create(AutomationRuleRequest request) {
        UUID userId = getCurrentUserId();
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        AutomationRule rule = AutomationRule.builder()
                .user(user)
                .name(request.getName())
                .active(request.getActive() != null ? request.getActive() : true)
                .conditionType(request.getConditionType())
                .conditionConfig(request.getConditionConfig())
                .actionType(request.getActionType())
                .actionConfig(request.getActionConfig())
                .build();
        rule = ruleRepository.save(rule);
        return toResponse(rule);
    }

    @Transactional
    public AutomationRuleResponse update(UUID id, AutomationRuleRequest request) {
        UUID userId = getCurrentUserId();
        AutomationRule rule = ruleRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Regra não encontrada"));
        rule.setName(request.getName());
        if (request.getActive() != null) rule.setActive(request.getActive());
        rule.setConditionType(request.getConditionType());
        rule.setConditionConfig(request.getConditionConfig());
        rule.setActionType(request.getActionType());
        rule.setActionConfig(request.getActionConfig());
        rule = ruleRepository.save(rule);
        return toResponse(rule);
    }

    @Transactional
    public void delete(UUID id) {
        UUID userId = getCurrentUserId();
        AutomationRule rule = ruleRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Regra não encontrada"));
        ruleRepository.delete(rule);
    }

    private AutomationRuleResponse toResponse(AutomationRule rule) {
        return AutomationRuleResponse.builder()
                .id(rule.getId())
                .name(rule.getName())
                .active(rule.getActive())
                .conditionType(rule.getConditionType())
                .conditionConfig(rule.getConditionConfig())
                .actionType(rule.getActionType())
                .actionConfig(rule.getActionConfig())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }
}
