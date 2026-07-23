package br.com.ecofy.ms_users.adapters.in.web.security;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

// Extrai o authUserId da claim configurada do JWT, negando propriedade a tokens de serviço.
@Component
public class JwtAuthenticatedUserProvider implements AuthenticatedUserProvider {

    private static final String INTERNAL_ROLE = "ROLE_INTERNAL";

    private final String ownerClaim;

    public JwtAuthenticatedUserProvider(
            @Value("${ecofy.users.security.owner-claim:sub}") String ownerClaim
    ) {
        this.ownerClaim = (ownerClaim == null || ownerClaim.isBlank()) ? "sub" : ownerClaim;
    }

    @Override
    public Optional<String> currentAuthUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth) || !jwtAuth.isAuthenticated()) {
            return Optional.empty();
        }
        String value = jwtAuth.getToken().getClaimAsString(ownerClaim);
        return (value == null || value.isBlank()) ? Optional.empty() : Optional.of(value);
    }

    @Override
    public String requireAuthUserId() {
        return currentAuthUserId().orElseThrow(() -> new NotAuthenticatedException(
                "No authenticated user: a user token with a valid owner claim is required"));
    }

    @Override
    public boolean isServiceToken() {
        return hasRole(INTERNAL_ROLE);
    }

    @Override
    public boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String expected = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(expected::equals);
    }

    // Sinaliza ausência de usuário autenticado, resultando em 401 em vez de 403.
    public static class NotAuthenticatedException extends RuntimeException {
        public NotAuthenticatedException(String message) {
            super(message);
        }
    }
}
