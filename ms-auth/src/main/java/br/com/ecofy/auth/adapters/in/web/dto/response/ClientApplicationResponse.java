package br.com.ecofy.auth.adapters.in.web.dto.response;

import br.com.ecofy.auth.core.domain.enums.ClientType;
import br.com.ecofy.auth.core.domain.enums.GrantType;

import java.time.Instant;
import java.util.Set;

public record ClientApplicationResponse(

        String id,
        String clientId,
        String name,
        ClientType clientType,
        Set<GrantType> grantTypes,
        Set<String> redirectUris,
        Set<String> scopes,
        boolean firstParty,
        boolean active,
        Instant createdAt,
        Instant updatedAt

) {}
