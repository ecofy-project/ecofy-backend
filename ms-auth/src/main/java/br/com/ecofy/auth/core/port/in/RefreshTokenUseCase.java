package br.com.ecofy.auth.core.port.in;

public interface RefreshTokenUseCase {

    record RefreshTokenResult(

            String accessToken,
            String refreshToken,
            long expiresInSeconds

    ) { }

    RefreshTokenResult refresh(RefreshTokenCommand command);

    record RefreshTokenCommand(

            String clientId,
            String refreshToken,
            String scope

    ) {}

}
