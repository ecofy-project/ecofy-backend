package br.com.ecofy.auth.core.domain.event;

import br.com.ecofy.auth.core.domain.AuthUser;
import java.time.Instant;

// Representa a confirmação do endereço de e-mail de um usuário.
public record UserEmailConfirmedEvent(
        AuthUser user,
        Instant occurredAt
) {

    public UserEmailConfirmedEvent(AuthUser user) {
        this(user, Instant.now());
    }
}
