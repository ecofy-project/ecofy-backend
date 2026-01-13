package br.com.ecofy.ms_users.adapters.out.persistence.entity;

import br.com.ecofy.ms_users.core.domain.enums.AccountProvider;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "linked_account",
        uniqueConstraints = @UniqueConstraint(name = "uk_linked_account_user_provider_ref", columnNames = {"user_id", "provider", "external_account_ref"}))
public class LinkedAccountEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 40)
    private AccountProvider provider;

    @Column(name = "external_account_ref", nullable = false, length = 200)
    private String externalAccountRef;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;
}