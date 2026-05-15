package br.com.ecofy.auth.core.port.in;

import br.com.ecofy.auth.core.domain.ClientApplication;
import br.com.ecofy.auth.core.domain.enums.ClientType;
import br.com.ecofy.auth.core.domain.enums.GrantType;

import java.util.Set;

public interface RegisterClientApplicationUseCase {

    ClientApplication register(RegisterClientCommand command);

    record RegisterClientCommand(

            String name,
            ClientType clientType,
            Set<GrantType> grantTypes,
            Set<String> redirectUris,
            Set<String> scopes,
            boolean firstParty

    ) {}

}
