package br.com.ecofy.ms_notification.adapters.out.external;

import br.com.ecofy.ms_notification.core.domain.valueobject.ChannelAddress;
import br.com.ecofy.ms_notification.core.port.out.PushSenderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
public class PushProviderAdapter implements PushSenderPort {

    private static final String PROVIDER_NAME = "push-stub";

    @Override
    public SendResult sendPush(ChannelAddress to, String title, String body) {
        Objects.requireNonNull(to, "to must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(body, "body must not be null");

        String messageId = UUID.randomUUID().toString();

        log.info(
                "[PushProviderAdapter] - [sendPush] -> provider={} to={} titleLen={} bodyLen={} messageId={}",
                PROVIDER_NAME,
                safeToken(to.address()),
                title.length(),
                body.length(),
                messageId
        );

        return new SendResult(PROVIDER_NAME, messageId);
    }

    private static String safeToken(String token) {
        if (token == null || token.isBlank()) return "<empty>";
        if (token.length() <= 8) return "***";
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }
}
