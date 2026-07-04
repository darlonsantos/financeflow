package com.financeflow.installments.domain;

import com.financeflow.transactions.domain.Transaction;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "installment_items", indexes = {
    @Index(name = "idx_installment_items_group_id", columnList = "installment_group_id"),
    @Index(name = "idx_installment_items_due_date", columnList = "due_date"),
    @Index(name = "idx_installment_items_status", columnList = "status")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_installment_items_group_number", columnNames = {"installment_group_id", "installment_number"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstallmentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "installment_group_id", nullable = false)
    private InstallmentGroup installmentGroup;

    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "installment_item_status")
    @Builder.Default
    private InstallmentItemStatus status = InstallmentItemStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum InstallmentItemStatus {
        PENDING,
        PAID,
        CANCELLED
    }
}
