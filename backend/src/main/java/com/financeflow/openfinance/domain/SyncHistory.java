package com.financeflow.openfinance.domain;

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

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sync_histories", indexes = {
    @Index(name = "idx_sync_histories_connection_id", columnList = "connection_id"),
    @Index(name = "idx_sync_histories_started_at", columnList = "started_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connection_id", nullable = false)
    private BankConnection connection;

    @CreationTimestamp
    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime dataInicio;

    @Column(name = "finished_at")
    private LocalDateTime dataFim;

    @Column(name = "total_imported", nullable = false)
    @Builder.Default
    private Integer totalImportado = 0;

    @Column(name = "conflicts", nullable = false)
    @Builder.Default
    private Integer conflitos = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String mensagemErro;

    public enum Status {
        SUCCESS,
        ERROR
    }
}
