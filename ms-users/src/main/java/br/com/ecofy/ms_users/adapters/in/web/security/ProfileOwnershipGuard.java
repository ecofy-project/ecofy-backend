package br.com.ecofy.ms_users.adapters.in.web.security;

import br.com.ecofy.ms_users.core.application.result.UserProfileResult;
import br.com.ecofy.ms_users.core.domain.exception.UserAccessForbiddenException;
import br.com.ecofy.ms_users.core.port.in.GetUserProfileUseCase;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// Verifica a propriedade do perfil comparando a claim do token com o externalAuthId do perfil.
@Component
@Slf4j
public class ProfileOwnershipGuard {

    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final GetUserProfileUseCase getUserProfileUseCase;
    private final MeterRegistry meterRegistry;

    public ProfileOwnershipGuard(
            AuthenticatedUserProvider authenticatedUserProvider,
            GetUserProfileUseCase getUserProfileUseCase,
            MeterRegistry meterRegistry
    ) {
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.getUserProfileUseCase = getUserProfileUseCase;
        this.meterRegistry = meterRegistry;
    }

    // Garante que o usuário autenticado é o dono do perfil e devolve o perfil já carregado.
    public UserProfileResult assertOwnsProfile(UUID profileId) {
        String authUserId = authenticatedUserProvider.requireAuthUserId();
        UserProfileResult profile = getUserProfileUseCase.getById(profileId);

        if (profile.externalAuthId() == null || !profile.externalAuthId().equals(authUserId)) {
            meterRegistry.counter("ecofy.users.ownership_denied", "operation", "profile").increment();
            // Não loga o authUserId nem o dono real: apenas o fato, para auditoria.
            log.warn("[ProfileOwnershipGuard] -> Acesso negado por ownership profileId={}", profileId);
            throw new UserAccessForbiddenException("Access to this user profile is not allowed");
        }
        return profile;
    }

    // Recupera o authUserId do usuário autenticado para os endpoints próprios e de criação.
    public String currentAuthUserId() {
        return authenticatedUserProvider.requireAuthUserId();
    }
}
