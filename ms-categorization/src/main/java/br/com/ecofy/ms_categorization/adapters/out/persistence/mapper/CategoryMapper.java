package br.com.ecofy.ms_categorization.adapters.out.persistence.mapper;

import br.com.ecofy.ms_categorization.adapters.out.persistence.entity.CategoryEntity;
import br.com.ecofy.ms_categorization.core.domain.Category;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// Centraliza a conversão entre categorias de domínio e entidades persistidas.
@Component
public class CategoryMapper {

    private final Clock clock;

    public CategoryMapper() {
        this(Clock.systemUTC());
    }

    public CategoryMapper(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    // Converte a categoria em uma entidade persistível.
    public CategoryEntity toEntity(Category d) {
        Objects.requireNonNull(d, "domain must not be null");

        return CategoryEntity.builder()
                .id(d.getId())
                .name(d.getName())
                .color(d.getColor())
                .active(d.isActive())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    // Converte a entidade persistida em categoria e valida os dados obrigatórios.
    public Category toDomain(CategoryEntity e) {
        Objects.requireNonNull(e, "entity must not be null");

        return new Category(
                nonNullOrThrow(e.getId(), "entity.id must not be null"),
                nonBlankOrThrow(e.getName(), "entity.name must not be blank"),
                e.getColor(),
                e.isActive(),
                nonNullOrNow(e.getCreatedAt()),
                nonNullOrNow(e.getUpdatedAt())
        );
    }

    private UUID nonNullOrThrow(UUID v, String msg) {
        if (v == null) throw new IllegalStateException(msg);
        return v;
    }

    private String nonBlankOrThrow(String v, String msg) {
        if (v == null || v.isBlank()) throw new IllegalStateException(msg);
        return v;
    }

    private Instant nonNullOrNow(Instant v) {
        return v != null ? v : Instant.now(clock);
    }
}
