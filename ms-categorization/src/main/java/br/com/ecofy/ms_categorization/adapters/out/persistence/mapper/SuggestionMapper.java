package br.com.ecofy.ms_categorization.adapters.out.persistence.mapper;

import br.com.ecofy.ms_categorization.adapters.out.persistence.entity.CategorizationSuggestionEntity;
import br.com.ecofy.ms_categorization.core.domain.CategorizationSuggestion;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// Centraliza a conversão entre sugestões de domínio e entidades persistidas.
@Component
public class SuggestionMapper {

    private final Clock clock;

    public SuggestionMapper() {
        this(Clock.systemUTC());
    }

    public SuggestionMapper(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    // Converte a sugestão de categorização em uma entidade persistível.
    public CategorizationSuggestionEntity toEntity(CategorizationSuggestion d) {
        Objects.requireNonNull(d, "domain must not be null");

        return CategorizationSuggestionEntity.builder()
                .id(d.getId())
                .transactionId(d.getTransactionId())
                .categoryId(d.getCategoryId())
                .ruleId(d.getRuleId())
                .status(d.getStatus())
                .score(d.getScore())
                .rationale(d.getRationale())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    // Converte a entidade persistida em uma sugestão de categorização.
    public CategorizationSuggestion toDomain(CategorizationSuggestionEntity e) {
        Objects.requireNonNull(e, "entity must not be null");

        return new CategorizationSuggestion(
                nonNullOrThrow(e.getId(), "entity.id must not be null"),
                nonNullOrThrow(e.getTransactionId(), "entity.transactionId must not be null"),
                e.getCategoryId(),
                e.getRuleId(),
                nonNullOrThrow(e.getStatus(), "entity.status must not be null"),
                e.getScore(),
                e.getRationale(),
                nonNullOrNow(e.getCreatedAt()),
                nonNullOrNow(e.getUpdatedAt())
        );
    }

    private UUID nonNullOrThrow(UUID v, String msg) {
        if (v == null) throw new IllegalStateException(msg);
        return v;
    }

    private <T> T nonNullOrThrow(T v, String msg) {
        if (v == null) throw new IllegalStateException(msg);
        return v;
    }

    private Instant nonNullOrNow(Instant v) {
        return v != null ? v : Instant.now(clock);
    }
}
