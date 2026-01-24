package br.com.ecofy.ms_users.core.application.service;

import br.com.ecofy.ms_users.core.application.result.ContactInfoResult;
import br.com.ecofy.ms_users.core.domain.enums.PreferenceKey;
import br.com.ecofy.ms_users.core.domain.exception.UserProfileNotFoundException;
import br.com.ecofy.ms_users.core.port.in.ResolveContactInfoUseCase;
import br.com.ecofy.ms_users.core.port.out.LoadUserPreferencesPort;
import br.com.ecofy.ms_users.core.port.out.LoadUserProfilePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class ContactInfoResolverService implements ResolveContactInfoUseCase {

    private final LoadUserProfilePort loadUserProfilePort;
    private final LoadUserPreferencesPort loadUserPreferencesPort;

    // Inicializa o serviço de resolução de contato, injetando portas de leitura de perfil e preferências do usuário.
    public ContactInfoResolverService(LoadUserProfilePort loadUserProfilePort,
                                      LoadUserPreferencesPort loadUserPreferencesPort) {
        this.loadUserProfilePort = Objects.requireNonNull(loadUserProfilePort, "loadUserProfilePort must not be null");
        this.loadUserPreferencesPort =
                Objects.requireNonNull(loadUserPreferencesPort, "loadUserPreferencesPort must not be null");
    }

    // Resolve e retorna as informações de contato do usuário (email/telefone/canais), combinando dados do perfil e preferências.
    @Override
    public ContactInfoResult resolve(UUID userId) {
        Objects.requireNonNull(userId, "userId must not be null");

        var profile = loadUserProfilePort.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[ContactInfoResolverService] - [resolve] -> profile not found userId={}", userId);
                    return new UserProfileNotFoundException(userId);
                });

        var prefs = loadUserPreferencesPort.findByUserId(userId);

        String notifyChannels = prefs.stream()
                .filter(p -> p.getKey() == PreferenceKey.NOTIFY_CHANNELS)
                .map(p -> p.getValue())
                .map(ContactInfoResolverService::blankToNull)
                .findFirst()
                .orElse(null);

        String email = (profile.getEmail() != null) ? blankToNull(profile.getEmail().value()) : null;
        String phone = (profile.getPhone() != null) ? blankToNull(profile.getPhone().value()) : null;

        log.debug(
                "[ContactInfoResolverService] - [resolve] -> userId={} hasEmail={} hasPhone={} hasNotifyChannels={}",
                userId,
                email != null,
                phone != null,
                notifyChannels != null
        );

        return new ContactInfoResult(
                userId,
                email,
                phone,
                notifyChannels
        );
    }

    // Normaliza uma string opcional, retornando null quando vazia/em branco e trimando quando presente.
    private static String blankToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isBlank() ? null : t;
    }

}
