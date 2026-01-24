package br.com.ecofy.ms_users.adapters.out.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "idempotency_key",
        uniqueConstraints = @UniqueConstraint(name = "uk_idempotency_operation_key", columnNames = {"operation", "idem_key"}))
public class IdempotencyKeyEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "operation", nullable = false, length = 120)
    private String operation;

    @Column(name = "idem_key", nullable = false, length = 200)
    private String key;

    @Column(name = "request_hash", nullable = false, length = 80)
    private String requestHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

}
