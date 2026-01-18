package br.com.ecofy.ms_categorization.adapters.in.web.advice;

import java.time.Instant;

public record ApiErrorResponse(

        String code,

        String message,

        Instant timestamp

) { }