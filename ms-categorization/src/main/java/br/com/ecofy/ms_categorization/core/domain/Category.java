package br.com.ecofy.ms_categorization.core.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// Representa uma categoria disponível para classificar transações.
public final class Category {

    private final UUID id;
    private final String name;
    private final String color;
    private final boolean active;
    private final Instant createdAt;
    private final Instant updatedAt;

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

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

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

    @Override
    public int hashCode() {
        return Objects.hash(id, name, color, active, createdAt, updatedAt);
    }

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
