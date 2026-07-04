package com.financeflow.installments.domain;

import com.financeflow.accounts.domain.Account;
import com.financeflow.categories.domain.Category;
import com.financeflow.users.domain.User;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "installment_groups", indexes = {
    @Index(name = "idx_installment_groups_user_id", columnList = "user_id"),
    @Index(name = "idx_installment_groups_account_id", columnList = "account_id"),
    @Index(name = "idx_installment_groups_status", columnList = "status"),
    @Index(name = "idx_installment_groups_first_due_date", columnList = "first_due_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstallmentGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(length = 500)
    private String description;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "installment_type", nullable = false, columnDefinition = "installment_group_type")
    @Builder.Default
    private InstallmentType installmentType = InstallmentType.FIXED;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "installment_group_status")
    @Builder.Default
    private InstallmentGroupStatus status = InstallmentGroupStatus.ACTIVE;

    @Column(name = "first_due_date", nullable = false)
    private LocalDate firstDueDate;

    @Column(name = "number_of_installments", nullable = false)
    private Integer numberOfInstallments;

    @OneToMany(mappedBy = "installmentGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InstallmentItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(nullable = false)
    @Builder.Default
    @Version
    private Integer version = 1;

    public enum InstallmentType {
        /** Parcelas com valor fixo (ex: 12x de R$ 100) */
        FIXED,
        /** Parcelas com valores variáveis (ex: 3x 100, 2x 150) */
        VARIABLE,
        /** Recorrente (assinatura mensal sem fim fixo; número de parcelas pode ser 0 = indefinido) */
        RECURRING
    }

    public enum InstallmentGroupStatus {
        ACTIVE,
        PAID_OFF,
        CANCELLED
    }
}
