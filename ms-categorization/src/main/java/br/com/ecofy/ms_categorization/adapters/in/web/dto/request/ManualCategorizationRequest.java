package br.com.ecofy.ms_categorization.adapters.in.web.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ManualCategorizationRequest(

        @NotNull
        UUID transactionId,

        @NotNull
        UUID categoryId,

        String rationale

) { }
