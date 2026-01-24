package br.com.ecofy.ms_users.adapters.out.persistence.mapper;

import br.com.ecofy.ms_users.adapters.out.persistence.entity.UserPreferenceEntity;
import br.com.ecofy.ms_users.core.domain.UserPreference;
import br.com.ecofy.ms_users.core.domain.valueobject.UserId;

import java.util.Objects;

/**
 * Mapper JPA <-> Domain para UserPreference.
 * Padrão: fail-fast em nulos essenciais e mapeamento explícito.
 */
public final class PreferenceMapper {

    // Cria uma instância do mapper (sem estado) para conversões entre Entity e Domain.
    public PreferenceMapper() {
    }

    // Converte UserPreferenceEntity (persistência) para UserPreference (domínio), realizando o mapeamento explícito dos campos.
    public UserPreference toDomain(UserPreferenceEntity e) {
        Objects.requireNonNull(e, "entity must not be null");

        return UserPreference.builder()
                .id(e.getId())
                .userId(UserId.of(e.getUserId()))
                .key(e.getKey())
                .value(e.getValue())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    // Converte UserPreference (domínio) para UserPreferenceEntity (persistência), realizando o mapeamento explícito dos campos.
    public static UserPreferenceEntity toEntity(UserPreference d) {
        Objects.requireNonNull(d, "domain must not be null");
        Objects.requireNonNull(d.getUserId(), "domain.userId must not be null");

        var e = new UserPreferenceEntity();
        e.setId(d.getId());
        e.setUserId(d.getUserId().value());
        e.setKey(d.getKey());
        e.setValue(d.getValue());
        e.setUpdatedAt(d.getUpdatedAt());
        return e;
    }

}
