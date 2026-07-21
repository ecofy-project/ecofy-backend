package br.com.ecofy.auth.adapters.out.security;

import br.com.ecofy.auth.adapters.out.persistence.AuthUserJpaAdapter;
import br.com.ecofy.auth.core.domain.AuthUser;
import br.com.ecofy.auth.core.domain.valueobject.AuthUserId;
import br.com.ecofy.auth.core.port.out.CurrentUserProviderPort;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

// Resolve o usuário autenticado a partir do contexto do Spring Security.
@Component
@Slf4j
public class SpringSecurityCurrentUserAdapter implements CurrentUserProviderPort {

    private final AuthUserJpaAdapter authUserJpaAdapter;

    public SpringSecurityCurrentUserAdapter(
            AuthUserJpaAdapter authUserJpaAdapter
    ) {
        this.authUserJpaAdapter = Objects.requireNonNull(
                authUserJpaAdapter,
                "authUserJpaAdapter must not be null"
        );
    }

    // Valida o principal autenticado e carrega o usuário correspondente.
    @Override
    public AuthUser getCurrentUserOrThrow() {
        log.debug(
                "[SpringSecurityCurrentUserAdapter] - [getCurrentUserOrThrow] -> Iniciando resolução do usuário atual"
        );

        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null) {
            log.warn(
                    "[SpringSecurityCurrentUserAdapter] - [getCurrentUserOrThrow] -> Authentication nulo no SecurityContext"
            );

            throw new IllegalStateException(
                    "Authentication token is missing"
            );
        }

        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            log.warn(
                    "[SpringSecurityCurrentUserAdapter] - [getCurrentUserOrThrow] -> Principal não é JWT. Principal={}",
                    authentication.getPrincipal()
                            .getClass()
                            .getSimpleName()
            );

            throw new IllegalStateException(
                    "Authentication token is invalid"
            );
        }

        log.debug(
                "[SpringSecurityCurrentUserAdapter] - [getCurrentUserOrThrow] -> JWT recebido: sub={}, jti={}",
                jwt.getSubject(),
                jwt.getId()
        );

        UUID userId;

        try {
            userId = UUID.fromString(jwt.getSubject());
        } catch (Exception ex) {
            log.error(
                    "[SpringSecurityCurrentUserAdapter] - [getCurrentUserOrThrow] -> Falha ao converter SUB em UUID sub={}",
                    jwt.getSubject()
            );

            throw new IllegalStateException(
                    "JWT subject is not a valid user identifier"
            );
        }

        return authUserJpaAdapter.loadById(new AuthUserId(userId))
                .map(user -> {
                    log.debug(
                            "[SpringSecurityCurrentUserAdapter] - [getCurrentUserOrThrow] -> Usuário autenticado carregado id={} email={}",
                            user.id().value(),
                            user.email().value()
                    );

                    return user;
                })
                .orElseThrow(() -> {
                    log.error(
                            "[SpringSecurityCurrentUserAdapter] - [getCurrentUserOrThrow] -> Usuário não encontrado no banco para userId={}",
                            userId
                    );

                    return new IllegalStateException(
                            "Usuário autenticado não encontrado"
                    );
                });
    }
}
