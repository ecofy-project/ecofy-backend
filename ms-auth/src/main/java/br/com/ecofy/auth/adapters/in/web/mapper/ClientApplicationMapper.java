package br.com.ecofy.auth.adapters.in.web.mapper;

import br.com.ecofy.auth.adapters.in.web.dto.response.ClientApplicationResponse;
import br.com.ecofy.auth.core.domain.ClientApplication;
import br.com.ecofy.auth.core.domain.enums.GrantType;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// Centraliza a conversão de aplicações cliente para contratos de resposta.
public final class ClientApplicationMapper {

    private ClientApplicationMapper() {
    }

    // Converte uma aplicação cliente do domínio para o contrato de resposta.
    public static ClientApplicationResponse toResponse(ClientApplication client) {
        Objects.requireNonNull(client, "client must not be null");

        String id = client.id() != null
                ? client.id().toString()
                : UUID.randomUUID().toString();

        Set<GrantType> grants = safeGrants(client.grantTypes());
        Set<String> redirectUris = safeStrings(client.redirectUris());
        Set<String> scopes = safeStrings(client.scopes());

        return new ClientApplicationResponse(
                id,
                client.clientId(),
                client.name(),
                client.clientType(),
                grants,
                redirectUris,
                scopes,
                client.isFirstParty(),
                client.isActive(),
                client.createdAt(),
                client.updatedAt()
        );
    }

    // Converte aplicações cliente para uma lista imutável de respostas.
    public static List<ClientApplicationResponse> toResponseList(
            List<ClientApplication> clients
    ) {
        if (clients == null || clients.isEmpty()) {
            return List.of();
        }

        return Collections.unmodifiableList(
                clients.stream()
                        .filter(Objects::nonNull)
                        .map(ClientApplicationMapper::toResponse)
                        .toList()
        );
    }

    // Normaliza valores textuais como um conjunto imutável sem elementos nulos.
    private static Set<String> safeStrings(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }

        return values.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    // Normaliza permissões de concessão como um conjunto imutável sem elementos nulos.
    private static Set<GrantType> safeGrants(Set<GrantType> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }

        return values.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }
}
