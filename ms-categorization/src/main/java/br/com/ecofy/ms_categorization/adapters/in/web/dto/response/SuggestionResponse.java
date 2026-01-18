package br.com.ecofy.ms_categorization.adapters.in.web.dto.response;

import br.com.ecofy.ms_categorization.core.domain.enums.SuggestionStatus;

import java.util.UUID;

public record SuggestionResponse(

        UUID id,
        UUID transactionId,
        UUID categoryId,
        UUID ruleId,
        SuggestionStatus status,
        int score,
        String rationale

) { }
