package com.financeflow.openfinance.domain;

import com.financeflow.transactions.domain.Transaction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "imported_transactions", indexes = {
    @Index(name = "idx_imported_transactions_bank_account_id", columnList = "bank_account_id"),
    @Index(name = "idx_imported_transactions_transaction_date", columnList = "transaction_date"),
    @Index(name = "idx_imported_transactions_reconciliation_status", columnList = "reconciliation_status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportedTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id", nullable = false)
    private BankAccountConnection account;

    @Column(name = "provider_transaction_id", nullable = false, length = 255)
    private String providerTransactionId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate dataTransacao;

    @Column(name = "suggested_category", length = 100)
    private String categoriaSugerida;

    @Enumerated(EnumType.STRING)
    @Column(name = "reconciliation_status", nullable = false, length = 30)
    @Builder.Default
    private StatusConciliacao statusConciliacao = StatusConciliacao.PENDENTE;

    @Column(name = "unique_hash", nullable = false, length = 255)
    private String hashUnico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime dataAtualizacao;

    public enum StatusConciliacao {
        PENDENTE,
        CONCILIADO,
        CONFLITO
    }
}
