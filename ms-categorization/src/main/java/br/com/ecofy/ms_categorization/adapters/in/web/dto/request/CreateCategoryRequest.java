package br.com.ecofy.ms_categorization.adapters.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateCategoryRequest(

        @NotBlank
        String name,

        String color

) { }