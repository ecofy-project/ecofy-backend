package br.com.ecofy.ms_categorization.core.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Category {

    private final UUID id;
    private final String name;
    private final String color;
    private final boolean active;
    private final Instant createdAt;
    private final Instant updatedAt;

    // Representa uma categoria de transações (ex.: Alimentação) com metadados de status e auditoria.
    public Category(
            UUID id,
            String name,
            String color,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        this.color = color;
        this.active = active;

        if (this.name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }

    // Retorna o identificador único da categoria.
    public UUID getId() {
        return id;
    }

    // Retorna o nome da categoria para exibição e regras.
    public String getName() {
        return name;
    }

    // Retorna a cor associada (opcional) para uso em UI.
    public String getColor() {
        return color;
    }

    // Indica se a categoria está ativa para uso no sistema.
    public boolean isActive() {
        return active;
    }

    // Retorna o timestamp de criação para rastreabilidade/auditoria.
    public Instant getCreatedAt() {
        return createdAt;
    }

    // Retorna o timestamp da última atualização para rastreabilidade/auditoria.
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Compara categorias por valor (campos relevantes) para consistência em coleções e testes.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category category)) return false;
        return active == category.active &&
                id.equals(category.id) &&
                name.equals(category.name) &&
                Objects.equals(color, category.color) &&
                createdAt.equals(category.createdAt) &&
                updatedAt.equals(category.updatedAt);
    }

    // Gera hash consistente com equals para uso em estruturas baseadas em hashing.
    @Override
    public int hashCode() {
        return Objects.hash(id, name, color, active, createdAt, updatedAt);
    }

    // Fornece uma representação textual completa da categoria para logs e debug.
    @Override
    public String toString() {
        return "Category[" +
                "id=" + id +
                ", name=" + name +
                ", color=" + color +
                ", active=" + active +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ']';
    }

}
