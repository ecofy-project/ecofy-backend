package br.com.ecofy.ms_categorization.adapters.in.web.dto.response;

import java.util.UUID;

public record CategoryResponse(

        UUID id,
        String name,
        String color,
        boolean active

) { }