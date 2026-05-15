package br.com.ecofy.auth.core.port.in;

import br.com.ecofy.auth.core.domain.JwtToken;

public interface AuthenticateUserUseCase {

    AuthenticationResult authenticate(AuthenticationCommand command);

    record AuthenticationCommand(

            String clientId,
            String clientSecret,
            String username,
            String password,
            String scope,
            String ipAddress

    ) { }

    record AuthenticationResult(

            JwtToken accessToken,
            String refreshToken,
            long expiresInSeconds,
            String tokenType

    ) { }

}
