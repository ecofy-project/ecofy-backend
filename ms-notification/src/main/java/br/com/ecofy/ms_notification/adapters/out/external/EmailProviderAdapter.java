package br.com.ecofy.ms_notification.adapters.out.external;

import br.com.ecofy.ms_notification.core.domain.valueobject.ChannelAddress;
import br.com.ecofy.ms_notification.core.port.out.EmailSenderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
public class EmailProviderAdapter implements EmailSenderPort {

    private static final String PROVIDER_NAME = "email-stub";

    // Envia e-mail via provedor (stub), validando parâmetros, gerando messageId e retornando um SendResult com rastreabilidade mínima.
    @Override
    public SendResult sendEmail(ChannelAddress to, String subject, String body) {
        Objects.requireNonNull(to, "to must not be null");
        Objects.requireNonNull(subject, "subject must not be null");
        Objects.requireNonNull(body, "body must not be null");

        String messageId = UUID.randomUUID().toString();

        log.info(
                "[EmailProviderAdapter] - [sendEmail] -> provider={} to={} subjectLen={} bodyLen={} messageId={}",
                PROVIDER_NAME,
                safeAddress(to.address()),
                subject.length(),
                body.length(),
                messageId
        );

        return new SendResult(PROVIDER_NAME, messageId);
    }

    // Mascara parcialmente o endereço para logs, reduzindo exposição de PII e mantendo utilidade de troubleshooting.
    private static String safeAddress(String address) {
        if (address == null || address.isBlank()) return "<empty>";
        int at = address.indexOf('@');
        if (at <= 1) return "***";
        String domain = address.substring(at + 1);
        return address.charAt(0) + "***@" + domain;
    }

}
