package com.financeflow.goals.dto;

import com.financeflow.goals.domain.Goal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalResponse {

    private UUID id;
    private String name;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private LocalDate dueDate;
    private Goal.GoalStatus status;
    private BigDecimal percentComplete;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
