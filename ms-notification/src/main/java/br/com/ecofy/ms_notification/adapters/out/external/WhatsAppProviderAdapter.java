package br.com.ecofy.ms_notification.adapters.out.external;

import br.com.ecofy.ms_notification.core.domain.valueobject.ChannelAddress;
import br.com.ecofy.ms_notification.core.port.out.WhatsAppSenderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
public class WhatsAppProviderAdapter implements WhatsAppSenderPort {

    private static final String PROVIDER_NAME = "whatsapp-stub";

    @Override
    public SendResult sendWhatsApp(ChannelAddress to, String body) {
        Objects.requireNonNull(to, "to must not be null");
        Objects.requireNonNull(body, "body must not be null");

        String messageId = UUID.randomUUID().toString();

        log.info(
                "[WhatsAppProviderAdapter] - [sendWhatsApp] -> provider={} to={} bodyLen={} messageId={}",
                PROVIDER_NAME,
                safePhone(to.address()),
                body.length(),
                messageId
        );

        return new SendResult(PROVIDER_NAME, messageId);
    }

    private static String safePhone(String phone) {
        if (phone == null || phone.isBlank()) return "<empty>";
        String digits = phone.replaceAll("\\D+", "");
        if (digits.length() <= 6) return "***";
        String prefix = digits.substring(0, Math.min(4, digits.length()));
        String suffix = digits.substring(digits.length() - 2);
        return prefix + "****" + suffix;
    }
}
