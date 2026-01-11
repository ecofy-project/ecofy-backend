package br.com.ecofy.ms_users.core.application.service;

import br.com.ecofy.ms_users.core.application.result.ContactInfoResult;
import br.com.ecofy.ms_users.core.domain.enums.PreferenceKey;
import br.com.ecofy.ms_users.core.domain.exception.UserProfileNotFoundException;
import br.com.ecofy.ms_users.core.port.in.ResolveContactInfoUseCase;
import br.com.ecofy.ms_users.core.port.out.LoadUserPreferencesPort;
import br.com.ecofy.ms_users.core.port.out.LoadUserProfilePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContactInfoResolverService implements ResolveContactInfoUseCase {

    private final LoadUserProfilePort loadUserProfilePort;
    private final LoadUserPreferencesPort loadUserPreferencesPort;

    @Override
    public ContactInfoResult resolve(UUID userId) {
        var profile = loadUserProfilePort.findById(userId).orElseThrow(() -> new UserProfileNotFoundException(userId));

        var prefs = loadUserPreferencesPort.findByUserId(userId);
        String notify = prefs.stream()
                .filter(p -> p.getKey() == PreferenceKey.NOTIFY_CHANNELS)
                .map(p -> p.getValue())
                .findFirst()
                .orElse(null);

        return new ContactInfoResult(
                userId,
                profile.getEmail() != null ? profile.getEmail().value() : null,
                profile.getPhone() != null ? profile.getPhone().value() : null,
                notify
        );
    }
}