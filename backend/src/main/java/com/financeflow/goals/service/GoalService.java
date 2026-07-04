package com.financeflow.goals.service;

import com.financeflow.audit.service.AuditService;
import com.financeflow.goals.domain.Goal;
import com.financeflow.goals.dto.GoalContributeRequest;
import com.financeflow.goals.dto.GoalRequest;
import com.financeflow.goals.dto.GoalResponse;
import com.financeflow.goals.exception.GoalNotFoundException;
import com.financeflow.goals.repository.GoalRepository;
import com.financeflow.users.domain.User;
import com.financeflow.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UUID)) {
            throw new RuntimeException("Usuário não autenticado");
        }
        return (UUID) authentication.getPrincipal();
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> findAll(Goal.GoalStatus status) {
        UUID userId = getCurrentUserId();
        List<Goal> goals = status != null
            ? goalRepository.findAllByUserId(userId).stream()
                .filter(g -> g.getStatus() == status)
                .collect(Collectors.toList())
            : goalRepository.findAllByUserId(userId);
        return goals.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GoalResponse findById(UUID id) {
        UUID userId = getCurrentUserId();
        Goal goal = goalRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new GoalNotFoundException(id));
        return toResponse(goal);
    }

    @Transactional
    public GoalResponse create(GoalRequest request) {
        UUID userId = getCurrentUserId();
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        Goal goal = Goal.builder()
            .user(user)
            .name(request.getName())
            .targetAmount(request.getTargetAmount())
            .currentAmount(BigDecimal.ZERO)
            .dueDate(request.getDueDate())
            .status(Goal.GoalStatus.ACTIVE)
            .build();

        goal = goalRepository.save(goal);

        auditService.logAction(userId, "CREATE", "Goal", goal.getId(), Map.of(
            "name", goal.getName(),
            "targetAmount", goal.getTargetAmount().toString(),
            "dueDate", goal.getDueDate() != null ? goal.getDueDate().toString() : "null"
        ));

        return toResponse(goal);
    }

    @Transactional
    public GoalResponse update(UUID id, GoalRequest request) {
        UUID userId = getCurrentUserId();
        Goal goal = goalRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new GoalNotFoundException(id));

        goal.setName(request.getName());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setDueDate(request.getDueDate());
        goal = goalRepository.save(goal);

        auditService.logAction(userId, "UPDATE", "Goal", goal.getId(), Map.of(
            "name", goal.getName(),
            "targetAmount", goal.getTargetAmount().toString(),
            "dueDate", goal.getDueDate() != null ? goal.getDueDate().toString() : "null"
        ));

        return toResponse(goal);
    }

    @Transactional
    public GoalResponse contribute(UUID id, GoalContributeRequest request) {
        UUID userId = getCurrentUserId();
        Goal goal = goalRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new GoalNotFoundException(id));

        if (goal.getStatus() != Goal.GoalStatus.ACTIVE) {
            throw new IllegalArgumentException("Apenas metas ativas podem receber contribuições");
        }

        BigDecimal newAmount = goal.getCurrentAmount().add(request.getAmount());
        goal.setCurrentAmount(newAmount);

        if (newAmount.compareTo(goal.getTargetAmount()) >= 0) {
            goal.setStatus(Goal.GoalStatus.COMPLETED);
        }

        goal = goalRepository.save(goal);

        auditService.logAction(userId, "CONTRIBUTE", "Goal", goal.getId(), Map.of(
            "amount", request.getAmount().toString(),
            "newCurrentAmount", newAmount.toString()
        ));

        return toResponse(goal);
    }

    @Transactional
    public GoalResponse updateStatus(UUID id, Goal.GoalStatus status) {
        UUID userId = getCurrentUserId();
        Goal goal = goalRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new GoalNotFoundException(id));

        goal.setStatus(status);
        goal = goalRepository.save(goal);

        auditService.logAction(userId, "UPDATE_STATUS", "Goal", goal.getId(), Map.of(
            "status", status.name()
        ));

        return toResponse(goal);
    }

    @Transactional
    public void delete(UUID id) {
        UUID userId = getCurrentUserId();
        Goal goal = goalRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new GoalNotFoundException(id));

        goal.setDeletedAt(java.time.LocalDateTime.now());
        goalRepository.save(goal);

        auditService.logAction(userId, "DELETE", "Goal", goal.getId(), Map.of(
            "name", goal.getName(),
            "targetAmount", goal.getTargetAmount().toString()
        ));
    }

    private GoalResponse toResponse(Goal goal) {
        BigDecimal percentComplete = goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0
            ? goal.getCurrentAmount().multiply(BigDecimal.valueOf(100))
                .divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        return GoalResponse.builder()
            .id(goal.getId())
            .name(goal.getName())
            .targetAmount(goal.getTargetAmount())
            .currentAmount(goal.getCurrentAmount())
            .dueDate(goal.getDueDate())
            .status(goal.getStatus())
            .percentComplete(percentComplete)
            .createdAt(goal.getCreatedAt())
            .updatedAt(goal.getUpdatedAt())
            .build();
    }
}
