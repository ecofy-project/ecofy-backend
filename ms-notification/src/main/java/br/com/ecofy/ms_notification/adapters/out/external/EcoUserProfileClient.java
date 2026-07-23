package br.com.ecofy.ms_notification.adapters.out.external;

import br.com.ecofy.ms_notification.core.domain.valueobject.UserId;
import br.com.ecofy.ms_notification.core.port.out.LoadUserContactInfoPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

// Resolve contatos sintéticos sem I/O em dev e teste, nunca sendo carregado em prod ou sandbox.
@Slf4j
@Component
@Profile("!prod & !sandbox")
public class EcoUserProfileClient implements LoadUserContactInfoPort {

    @Override
    public Optional<UserContactInfo> load(UserId userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        log.debug("[EcoUserProfileClient] - [load] -> synthetic contacts (dev/test) userId={}", userId.value());
        return Optional.of(synthetic(userId));
    }

    private static UserContactInfo synthetic(UserId userId) {
        String email = "user+" + userId.value() + "@example.com";
        String phone = "+5511999999999";
        String deviceToken = "device-token-" + UUID.randomUUID();
        return new UserContactInfo(email, phone, deviceToken);
    }
}
