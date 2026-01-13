package br.com.ecofy.ms_users.adapters.out.persistence.entity;

import br.com.ecofy.ms_users.core.domain.enums.PreferenceKey;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "user_preference",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_preference_user_key", columnNames = {"user_id", "pref_key"}))
public class UserPreferenceEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "pref_key", nullable = false, length = 60)
    private PreferenceKey key;

    @Column(name = "pref_value", nullable = false, columnDefinition = "text")
    private String value;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}