package br.com.ecofy.ms_categorization.adapters.out.messaging.dto;

import java.util.UUID;

// Define o payload v1 de CATEGORIZATION_APPLIED, com ruleId nulo quando a categorização é manual.
public record CategorizationAppliedDataV1(
        UUID transactionId,
        UUID categoryId,
        UUID ruleId,
        String mode,
        int score,
        UUID suggestionId
) {
}
