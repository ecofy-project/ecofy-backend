package br.com.ecofy.auth.core.domain.event;

import br.com.ecofy.auth.core.domain.AuthUser;
import java.time.Instant;

// Representa a solicitação de redefinição de senha de um usuário.
public record PasswordResetRequestedEvent(
        AuthUser user,
        String resetToken,
        Instant occurredAt
) {

    public PasswordResetRequestedEvent(
            AuthUser user,
            String resetToken
    ) {
        this(user, resetToken, Instant.now());
    }
}
