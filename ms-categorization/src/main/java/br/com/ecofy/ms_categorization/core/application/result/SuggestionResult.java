package br.com.ecofy.ms_categorization.core.application.result;

import br.com.ecofy.ms_categorization.core.domain.enums.SuggestionStatus;

import java.util.UUID;

public record SuggestionResult(

        UUID id,

        UUID transactionId,

        UUID categoryId,

        UUID ruleId,

        SuggestionStatus status,

        int score,

        String rationale

) { }
