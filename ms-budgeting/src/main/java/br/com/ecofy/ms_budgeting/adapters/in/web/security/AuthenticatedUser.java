package br.com.ecofy.ms_budgeting.adapters.in.web.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import java.util.UUID;

// Resolve o dono da requisição sempre a partir de uma claim do JWT, nunca de parâmetros da requisição.
public final class AuthenticatedUser {

    private AuthenticatedUser() {
    }

    // Extrai o id do dono da claim informada, rejeitando token ausente ou claim inválida.
    public static UUID requireOwnerId(String ownerClaim) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new InvalidBearerTokenException("Authenticated JWT is required");
        }

        String value = claimValue(jwt, ownerClaim);
        if (value == null || value.isBlank()) {
            throw new InvalidBearerTokenException("JWT is missing the owner claim '" + ownerClaim + "'");
        }

        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException e) {
            // O ms-auth emite o id do usuário como UUID; qualquer outra coisa não é dono válido.
            throw new InvalidBearerTokenException("JWT owner claim '" + ownerClaim + "' is not a valid user id");
        }
    }

    private static String claimValue(Jwt jwt, String ownerClaim) {
        if ("sub".equals(ownerClaim)) {
            return jwt.getSubject();
        }
        Object claim = jwt.getClaim(ownerClaim);
        return claim != null ? String.valueOf(claim) : null;
    }
}
