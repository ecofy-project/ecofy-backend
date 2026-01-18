package br.com.ecofy.ms_budgeting.adapters.out.persistence.mapper;

import br.com.ecofy.ms_budgeting.adapters.out.persistence.entity.BudgetAlertEntity;
import br.com.ecofy.ms_budgeting.core.domain.BudgetAlert;
import br.com.ecofy.ms_budgeting.core.domain.enums.AlertSeverity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class BudgetAlertMapper {

    private BudgetAlertMapper() {}

    // Converte o objeto de domínio (BudgetAlert) para a entidade JPA (BudgetAlertEntity).
    public static BudgetAlertEntity toEntity(BudgetAlert d) {
        Objects.requireNonNull(d, "domain must not be null");

        return BudgetAlertEntity.builder()
                .id(d.getId())
                .budgetId(d.getBudgetId())
                .consumptionId(d.getConsumptionId())
                .severity(d.getSeverity().name())
                .message(normalizeMessage(d.getMessage()))
                .periodStart(d.getPeriodStart())
                .periodEnd(d.getPeriodEnd())
                .createdAt(d.getCreatedAt())
                .build();
    }

    // Converte a entidade JPA (BudgetAlertEntity) para o objeto de domínio (BudgetAlert).
    public static BudgetAlert toDomain(BudgetAlertEntity e) {
        if (e == null) return null;

        return new BudgetAlert(
                requireNonNull(e.getId(), "id"),
                requireNonNull(e.getBudgetId(), "budgetId"),
                requireNonNull(e.getConsumptionId(), "consumptionId"),
                parseSeverity(e.getSeverity()),
                normalizeMessage(e.getMessage()),
                requireNonNull(e.getPeriodStart(), "periodStart"),
                requireNonNull(e.getPeriodEnd(), "periodEnd"),
                e.getCreatedAt() != null ? e.getCreatedAt() : Instant.now()
        );
    }

    // Cria um BudgetAlert novo (domínio) já com id e createdAt preenchidos.
    public static BudgetAlert newAlert(
            UUID budgetId,
            UUID consumptionId,
            AlertSeverity severity,
            String message,
            java.time.LocalDate periodStart,
            java.time.LocalDate periodEnd
    ) {
        return new BudgetAlert(
                UUID.randomUUID(),
                requireNonNull(budgetId, "budgetId"),
                requireNonNull(consumptionId, "consumptionId"),
                requireNonNull(severity, "severity"),
                normalizeMessage(message),
                requireNonNull(periodStart, "periodStart"),
                requireNonNull(periodEnd, "periodEnd"),
                Instant.now()
        );
    }

    // Converte o texto persistido de severidade para o enum AlertSeverity.
    private static AlertSeverity parseSeverity(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("severity must not be blank");
        }
        try {
            return AlertSeverity.valueOf(raw.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown severity: " + raw, ex);
        }
    }

    // Normaliza e valida a mensagem para garantir que não seja nula/blank.
    private static String normalizeMessage(String msg) {
        if (msg == null || msg.trim().isEmpty()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        return msg.trim();
    }

    // Valida que um campo obrigatório não é nulo e retorna o próprio valor.
    private static <T> T requireNonNull(T v, String field) {
        return Objects.requireNonNull(v, field + " must not be null");
    }
}
