package br.com.ecofy.auth.core.domain.event;

import br.com.ecofy.auth.core.domain.AuthUser;
import java.time.Instant;

// Representa o registro concluído de um novo usuário.
public record UserRegisteredEvent(
        AuthUser user,
        Instant occurredAt
) {

    public UserRegisteredEvent(AuthUser user) {
        this(user, Instant.now());
    }
}
