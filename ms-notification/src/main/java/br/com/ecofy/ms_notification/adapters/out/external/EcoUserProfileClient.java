package br.com.ecofy.ms_notification.adapters.out.external;

import br.com.ecofy.ms_notification.config.NotificationProperties;
import br.com.ecofy.ms_notification.core.domain.valueobject.UserId;
import br.com.ecofy.ms_notification.core.port.out.LoadUserContactInfoPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class EcoUserProfileClient implements LoadUserContactInfoPort {

    private final NotificationProperties.Clients.UserProfile userProfileProps;

    public EcoUserProfileClient(NotificationProperties props) {
        Objects.requireNonNull(props, "props must not be null");
        Objects.requireNonNull(props.getClients(), "props.clients must not be null");
        this.userProfileProps = Objects.requireNonNull(
                props.getClients().getUserProfile(),
                "props.clients.userProfile must not be null"
        );
    }

    // Carrega informações de contato do usuário via client de perfil, aplicando feature-flag e fallback seguro quando desabilitado ou não implementado.
    @Override
    public Optional<UserContactInfo> load(UserId userId) {
        Objects.requireNonNull(userId, "userId must not be null");

        if (!userProfileProps.isEnabled()) {
            log.debug(
                    "[EcoUserProfileClient] - [load] -> client disabled. Returning synthetic contacts userId={}",
                    userId.value()
            );
            return Optional.of(syntheticContacts(userId));
        }

        // TODO: Implementar integração real com ms-users (WebClient/Feign) e mapear resposta -> UserContactInfo
        log.warn(
                "[EcoUserProfileClient] - [load] -> client enabled but not implemented. Returning empty userId={}",
                userId.value()
        );
        return Optional.empty();
    }

    // Gera contatos sintéticos (email/telefone/deviceToken) para ambientes de dev/teste quando o client real estiver desabilitado.
    private static UserContactInfo syntheticContacts(UserId userId) {
        String email = "user+" + userId.value() + "@example.com";
        String phone = "+5511999999999";
        String deviceToken = "device-token-" + UUID.randomUUID();
        return new UserContactInfo(email, phone, deviceToken);
    }

}
