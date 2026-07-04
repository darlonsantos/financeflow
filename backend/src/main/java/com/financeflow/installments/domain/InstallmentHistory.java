package com.financeflow.installments.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "installment_history", indexes = {
    @Index(name = "idx_installment_history_group_id", columnList = "installment_group_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstallmentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "installment_group_id", nullable = false)
    private UUID installmentGroupId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "installment_history_action")
    private HistoryAction action;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> details;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum HistoryAction {
        CREATED,
        PAY_INSTALLMENT,
        EARLY_SETTLEMENT,
        RENEGOTIATION,
        CANCELLED
    }
}
