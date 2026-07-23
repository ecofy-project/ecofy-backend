package br.com.ecofy.ms_users.adapters.out.persistence.mapper;

import br.com.ecofy.ms_users.adapters.out.persistence.entity.LinkedAccountEntity;
import br.com.ecofy.ms_users.core.domain.LinkedAccount;
import br.com.ecofy.ms_users.core.domain.valueobject.UserId;

import java.util.Objects;

// Converte contas vinculadas entre entidade e domínio, falhando cedo em campos essenciais nulos.
public final class LinkedAccountMapper {

    // Cria uma instância do mapper (sem estado) para conversões entre Entity e Domain.
    public LinkedAccountMapper() {
    }

    // Converte LinkedAccountEntity (persistência) para LinkedAccount (domínio), realizando o mapeamento explícito dos campos.
    public LinkedAccount toDomain(LinkedAccountEntity e) {
        Objects.requireNonNull(e, "entity must not be null");

        return LinkedAccount.builder()
                .id(e.getId())
                .userId(UserId.of(e.getUserId()))
                .provider(e.getProvider())
                .externalAccountRef(e.getExternalAccountRef())
                .active(e.isActive())
                .linkedAt(e.getLinkedAt())
                .build();
    }

    // Converte LinkedAccount (domínio) para LinkedAccountEntity (persistência), realizando o mapeamento explícito dos campos.
    public static LinkedAccountEntity toEntity(LinkedAccount d) {
        Objects.requireNonNull(d, "domain must not be null");
        Objects.requireNonNull(d.getUserId(), "domain.userId must not be null");

        var e = new LinkedAccountEntity();
        e.setId(d.getId());
        e.setUserId(d.getUserId().value());
        e.setProvider(d.getProvider());
        e.setExternalAccountRef(d.getExternalAccountRef());
        e.setActive(d.isActive());
        e.setLinkedAt(d.getLinkedAt());
        return e;
    }

}
