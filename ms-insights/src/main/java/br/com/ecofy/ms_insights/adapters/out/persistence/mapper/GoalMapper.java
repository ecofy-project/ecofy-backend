package br.com.ecofy.ms_insights.adapters.out.persistence.mapper;

import br.com.ecofy.ms_insights.adapters.out.persistence.entity.GoalEntity;
import br.com.ecofy.ms_insights.core.domain.Goal;
import br.com.ecofy.ms_insights.core.domain.enums.GoalStatus;
import br.com.ecofy.ms_insights.core.domain.valueobject.Money;
import br.com.ecofy.ms_insights.core.domain.valueobject.UserId;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// Converte metas entre os modelos de domínio e persistência.
public final class GoalMapper {

    private GoalMapper() {
    }

    // Converte a meta do domínio para persistência.
    public static GoalEntity toEntity(Goal d) {
        Objects.requireNonNull(d, "goal must not be null");
        Objects.requireNonNull(d.getUserId(), "goal.userId must not be null");
        Objects.requireNonNull(d.getUserId().value(), "goal.userId.value must not be null");

        String name = requireNonBlank(d.getName(), "goal.name");
        Money target = Objects.requireNonNull(d.getTarget(), "goal.target must not be null");
        String currency = requireNonBlank(target.currency(), "goal.target.currency");

        GoalStatus status = Objects.requireNonNull(d.getStatus(), "goal.status must not be null");

        return GoalEntity.builder()
                .id(Objects.requireNonNull(d.getId(), "goal.id must not be null"))
                .userId(d.getUserId().value())
                .name(name)
                .targetCents(target.cents())
                .currency(currency)
                .status(status.name())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    // Converte a entidade para o domínio usando o relógio UTC.
    public static Goal toDomain(GoalEntity e) {
        return toDomain(e, Clock.systemUTC());
    }

    // Converte a entidade para o domínio e normaliza timestamps ausentes.
    public static Goal toDomain(GoalEntity e, Clock clock) {
        Objects.requireNonNull(e, "entity must not be null");
        Objects.requireNonNull(clock, "clock must not be null");

        UUID id = Objects.requireNonNull(e.getId(), "entity.id must not be null");
        UUID userId = Objects.requireNonNull(e.getUserId(), "entity.userId must not be null");
        String name = requireNonBlank(e.getName(), "entity.name");

        long targetCents = e.getTargetCents();
        String currency = requireNonBlank(e.getCurrency(), "entity.currency");

        GoalStatus status = parseStatus(e.getStatus());

        Instant now = Instant.now(clock);
        Instant createdAt = e.getCreatedAt() != null ? e.getCreatedAt() : now;
        Instant updatedAt = e.getUpdatedAt() != null ? e.getUpdatedAt() : createdAt;

        return new Goal(
                id,
                new UserId(userId),
                name,
                Money.ofCents(targetCents, currency),
                status,
                createdAt,
                updatedAt
        );
    }

    // Resolve o status persistido com fallback para o estado ativo.
    private static GoalStatus parseStatus(String status) {
        String v = requireNonBlank(status, "entity.status");
        try {
            return GoalStatus.valueOf(v);
        } catch (IllegalArgumentException ex) {
            return GoalStatus.ACTIVE;
        }
    }

    // Valida e normaliza valores textuais obrigatórios.
    private static String requireNonBlank(String v, String field) {
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return v.trim();
    }
}
