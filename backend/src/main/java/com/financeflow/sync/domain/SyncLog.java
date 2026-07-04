package com.financeflow.sync.domain;

import com.financeflow.users.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sync_logs", indexes = {
    @Index(name = "idx_sync_logs_user_id", columnList = "user_id"),
    @Index(name = "idx_sync_logs_entity_type", columnList = "entity_type"),
    @Index(name = "idx_sync_logs_sync_status", columnList = "sync_status"),
    @Index(name = "idx_sync_logs_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;
    
    @Column(name = "entity_id", nullable = false)
    private UUID entityId;
    
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "sync_action")
    private SyncAction action;
    
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "sync_status", nullable = false, columnDefinition = "sync_status")
    private SyncStatus syncStatus;
    
    @Column(name = "client_timestamp", nullable = false)
    private LocalDateTime clientTimestamp;
    
    @Column(name = "server_timestamp")
    private LocalDateTime serverTimestamp;
    
    @Column(name = "conflict_resolution", columnDefinition = "TEXT")
    private String conflictResolution;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    public enum SyncAction {
        CREATE, UPDATE, DELETE
    }
    
    public enum SyncStatus {
        PENDING, SYNCED, CONFLICT, ERROR
    }
}
