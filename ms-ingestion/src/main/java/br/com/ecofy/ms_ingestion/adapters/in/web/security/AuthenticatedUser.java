package br.com.ecofy.ms_ingestion.adapters.in.web.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import java.util.UUID;

// Resolve o proprietário da requisição exclusivamente pelo JWT autenticado.
public final class AuthenticatedUser {

    private AuthenticatedUser() {
    }

    // Extrai o identificador do proprietário e valida a claim obrigatória.
    public static UUID requireOwnerId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new InvalidBearerTokenException("Authenticated JWT is required");
        }

        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new InvalidBearerTokenException("JWT is missing the 'sub' claim");
        }

        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            throw new InvalidBearerTokenException("JWT 'sub' claim is not a valid user id");
        }
    }
}
