package br.com.ecofy.ms_categorization.core.application.command;

import java.util.Objects;

public record CreateCategoryCommand(

        String name,

        String color

) {

    // Valida os dados de entrada para criação de uma categoria (nome obrigatório e não vazio).
    public CreateCategoryCommand {

        Objects.requireNonNull(name, "name must not be null");

        if (name.isBlank()) throw new IllegalArgumentException("name must not be blank");

    }

}
