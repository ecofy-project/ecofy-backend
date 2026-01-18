package br.com.ecofy.ms_categorization.core.application.result;

import java.util.UUID;

public record CategorizationResult(

        UUID transactionId,

        boolean categorized,

        UUID categoryId,

        UUID suggestionId,

        String decision,

        int score

) { }