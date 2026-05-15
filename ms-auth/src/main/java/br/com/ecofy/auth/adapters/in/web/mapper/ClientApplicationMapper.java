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

public final class ClientApplicationMapper {

    private ClientApplicationMapper() {
        // utility class
    }

    // Converte um ClientApplication (domínio) para ClientApplicationResponse (DTO de saída).
    public static ClientApplicationResponse toResponse(ClientApplication client) {
        Objects.requireNonNull(client, "client must not be null");

        // Garante um id sempre preenchido (usa o id do domínio ou gera um novo).
        String id = client.id() != null ? client.id().toString() : UUID.randomUUID().toString();

        // Normaliza coleções para nunca retornar null (apenas sets imutáveis).
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

    // Converte uma lista de ClientApplication (domínio) para uma lista imutável de responses.
    public static List<ClientApplicationResponse> toResponseList(List<ClientApplication> clients) {
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

    // Retorna um Set<String> imutável sem nulls (ou Set vazio se a entrada for null/vazia).
    private static Set<String> safeStrings(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }

        return values.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    // Retorna um Set<GrantType> imutável sem nulls (ou Set vazio se a entrada for null/vazia).
    private static Set<GrantType> safeGrants(Set<GrantType> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }

        return values.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }
}