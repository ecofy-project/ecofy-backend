package br.com.ecofy.auth.core.application.service;

import br.com.ecofy.auth.core.application.exception.AuthErrorCode;
import br.com.ecofy.auth.core.application.exception.AuthException;
import br.com.ecofy.auth.core.port.in.ValidateTokenUseCase;
import br.com.ecofy.auth.core.port.out.JwtTokenProviderPort;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// Centraliza a validação criptográfica e a extração das claims dos tokens.
@Slf4j
@Service
public class TokenValidationService implements ValidateTokenUseCase {

    private final JwtTokenProviderPort jwtTokenProviderPort;

    public TokenValidationService(
            JwtTokenProviderPort jwtTokenProviderPort
    ) {
        this.jwtTokenProviderPort = Objects.requireNonNull(
                jwtTokenProviderPort,
                "jwtTokenProviderPort must not be null"
        );
    }

    // Valida o token e converte falhas em um erro padronizado.
    @Override
    public Map<String, Object> validate(String token) {
        Objects.requireNonNull(
                token,
                "token must not be null"
        );

        String masked = maskToken(token);

        log.debug(
                "[TokenValidationService] - [validate] -> Validando token (assinatura + expiração) tokenMask={}",
                masked
        );

        final Map<String, Object> claims;

        try {
            claims = jwtTokenProviderPort
                    .verifyAndParseClaims(token);
        } catch (IllegalArgumentException ex) {
            log.warn(
                    "[TokenValidationService] - [validate] -> Token inválido (assinatura/expiração) tokenMask={}",
                    masked
            );

            throw new AuthException(
                    AuthErrorCode.INVALID_TOKEN_SIGNATURE,
                    "Invalid token"
            );
        }

        log.debug(
                "[TokenValidationService] - [validate] -> Token válido, claims extraídas tokenMask={} claimsKeys={}",
                masked,
                claims.keySet()
        );

        return claims;
    }

    private String maskToken(String token) {
        if (token == null || token.isBlank()) {
            return "***";
        }

        return token.length() > 12
                ? token.substring(0, 12) + "..."
                : "***";
    }
}
