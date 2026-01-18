package br.com.ecofy.ms_categorization.adapters.out.persistence.mapper;

import br.com.ecofy.ms_categorization.adapters.out.persistence.entity.CategorizationSuggestionEntity;
import br.com.ecofy.ms_categorization.core.domain.CategorizationSuggestion;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Component
public class SuggestionMapper {

    private final Clock clock;

    // Construtor default que usa Clock UTC.
    public SuggestionMapper() {
        this(Clock.systemUTC());
    }

    // Construtor que injeta Clock para testes e consistência temporal.
    public SuggestionMapper(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    // Converte o domínio CategorizationSuggestion para a entidade JPA CategorizationSuggestionEntity.
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

    // Converte a entidade JPA CategorizationSuggestionEntity para o domínio CategorizationSuggestion.
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

    // Garante que um UUID obrigatório não seja nulo (senão lança exceção).
    private UUID nonNullOrThrow(UUID v, String msg) {
        if (v == null) throw new IllegalStateException(msg);
        return v;
    }

    // Garante que um valor obrigatório não seja nulo (senão lança exceção).
    private <T> T nonNullOrThrow(T v, String msg) {
        if (v == null) throw new IllegalStateException(msg);
        return v;
    }

    // Retorna o Instant informado ou um Instant "agora" usando o Clock.
    private Instant nonNullOrNow(Instant v) {
        return v != null ? v : Instant.now(clock);
    }

}
