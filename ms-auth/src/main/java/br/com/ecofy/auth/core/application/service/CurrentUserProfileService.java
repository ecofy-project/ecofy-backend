package br.com.ecofy.auth.core.application.service;

import br.com.ecofy.auth.core.application.exception.AuthErrorCode;
import br.com.ecofy.auth.core.application.exception.AuthException;
import br.com.ecofy.auth.core.domain.AuthUser;
import br.com.ecofy.auth.core.port.in.GetCurrentUserProfileUseCase;
import br.com.ecofy.auth.core.port.out.CurrentUserProviderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

// Serviço responsável por obter e retornar o perfil do usuário atualmente autenticado.
@Slf4j
@Service
public class CurrentUserProfileService implements GetCurrentUserProfileUseCase {

    private final CurrentUserProviderPort currentUserProviderPort;

    // Injeta a porta que resolve o usuário atual no contexto de segurança e garante que ela não seja nula.
    public CurrentUserProfileService(CurrentUserProviderPort currentUserProviderPort) {
        this.currentUserProviderPort =
                Objects.requireNonNull(currentUserProviderPort, "currentUserProviderPort must not be null");
    }

    // Retorna o usuário autenticado atual, convertendo falhas de resolução em AuthException padronizada.
    @Override
    public AuthUser getCurrentUser() {
        log.debug("[CurrentUserProfileService] - [getCurrentUser] -> Buscando usuário autenticado…");

        try {
            AuthUser user = currentUserProviderPort.getCurrentUserOrThrow();

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
