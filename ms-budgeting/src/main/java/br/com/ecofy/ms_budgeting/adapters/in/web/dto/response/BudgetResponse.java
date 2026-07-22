package br.com.ecofy.ms_budgeting.adapters.in.web.dto.response;

import br.com.ecofy.ms_budgeting.core.application.result.BudgetResult;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BudgetResponse(

        UUID id,

        UUID userId,

        UUID categoryId,

        String periodType,

        LocalDate periodStart,

        LocalDate periodEnd,

        String status,

        String currency,

        String limitAmount,

        Instant createdAt,

        Instant updatedAt,

        // Versão para optimistic locking (ECO-11 §17.4 Option A): o cliente devolve este
        // valor no PUT para detectar atualização concorrente. Nullable para budget legado.
        Long version

) {
    // Construtor de compatibilidade (sem versão) — preserva chamadas existentes.
    public BudgetResponse(UUID id, UUID userId, UUID categoryId, String periodType,
                          LocalDate periodStart, LocalDate periodEnd, String status, String currency,
                          String limitAmount, Instant createdAt, Instant updatedAt) {
        this(id, userId, categoryId, periodType, periodStart, periodEnd, status, currency,
                limitAmount, createdAt, updatedAt, null);
    }

    // converte o resultado do caso de uso (BudgetResult) para o DTO de resposta da API
    public static BudgetResponse from(BudgetResult r) {
        return new BudgetResponse(
                r.id(),
                r.userId(),
                r.categoryId(),
                r.periodType().name(),
                r.periodStart(),
                r.periodEnd(),
                r.status().name(),
                r.currency(),
                r.limitAmount().toPlainString(),
                r.createdAt(),
                r.updatedAt(),
                r.version()
        );
    }

}
