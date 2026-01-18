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

        Instant updatedAt

) {

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
                r.updatedAt()
        );
    }

}
