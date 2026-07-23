package br.com.ecofy.ms_notification.adapters.out.external;

import br.com.ecofy.ms_notification.core.domain.valueobject.ChannelAddress;
import br.com.ecofy.ms_notification.core.port.out.PushSenderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

// Simula o envio de push em console para dev e teste, sem I/O externo e fora de prod e sandbox.
@Slf4j
@Component
@Profile("!prod & !sandbox")
public class PushProviderAdapter implements PushSenderPort {

    private static final String PROVIDER_NAME = "push-console";

    // Envia push via provedor (stub), validando parâmetros, gerando messageId e retornando um SendResult com rastreabilidade mínima.
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

    // Mascara parcialmente o device token para logs, reduzindo exposição de credenciais/PII e preservando depuração.
    private static String safeToken(String token) {
        if (token == null || token.isBlank()) return "<empty>";
        if (token.length() <= 8) return "***";
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }

}
