package br.com.ecofy.ms_categorization.adapters.in.web.dto.response;

import java.util.UUID;

public record CategorizationResponse(

        UUID transactionId,
        boolean categorized,
        UUID categoryId,
        UUID suggestionId,
        String decision,
        int score

) { }