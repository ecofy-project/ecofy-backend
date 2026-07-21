package br.com.ecofy.auth.core.application.service;

import br.com.ecofy.auth.core.application.exception.AuthErrorCode;
import br.com.ecofy.auth.core.application.exception.AuthException;
import br.com.ecofy.auth.core.domain.AuthUser;
import br.com.ecofy.auth.core.port.in.GetCurrentUserProfileUseCase;
import br.com.ecofy.auth.core.port.out.CurrentUserProviderPort;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// Resolve o perfil associado ao usuário autenticado.
@Slf4j
@Service
public class CurrentUserProfileService
        implements GetCurrentUserProfileUseCase {

    private final CurrentUserProviderPort currentUserProviderPort;

    public CurrentUserProfileService(
            CurrentUserProviderPort currentUserProviderPort
    ) {
        this.currentUserProviderPort = Objects.requireNonNull(
                currentUserProviderPort,
                "currentUserProviderPort must not be null"
        );
    }

    // Converte falhas de resolução em um erro padronizado de autenticação.
    @Override
    public AuthUser getCurrentUser() {
        log.debug(
                "[CurrentUserProfileService] - [getCurrentUser] -> Buscando usuário autenticado…"
        );

        try {
            AuthUser user =
                    currentUserProviderPort.getCurrentUserOrThrow();

            log.debug(
                    "[CurrentUserProfileService] - [getCurrentUser] -> Usuário autenticado id={} email={} status={}",
                    user.id().value(),
                    user.email().value(),
                    user.status()
            );

            return user;
        } catch (RuntimeException ex) {
            throw new AuthException(
                    AuthErrorCode.CURRENT_USER_NOT_AUTHENTICATED,
                    "No authenticated user found",
                    ex
            );
        }
    }
}
