package br.com.ecofy.auth.adapters.in.web.dto.request;

import br.com.ecofy.auth.core.domain.enums.ClientType;
import br.com.ecofy.auth.core.domain.enums.GrantType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record ClientApplicationRequest(

        @NotBlank
        String name,

        @NotNull
        ClientType clientType,

        Set<GrantType> grantTypes,

        Set<String> redirectUris,

        Set<String> scopes,

        Boolean firstParty

) {}
