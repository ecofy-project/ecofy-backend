package br.com.ecofy.ms_notification.adapters.out.external;

import br.com.ecofy.ms_notification.adapters.out.external.support.HttpProviderClient;
import br.com.ecofy.ms_notification.adapters.out.external.support.ProviderCircuitBreaker;
import br.com.ecofy.ms_notification.config.NotificationProperties;
import br.com.ecofy.ms_notification.core.domain.valueobject.ChannelAddress;
import br.com.ecofy.ms_notification.core.port.out.EmailSenderPort;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

// Envia e-mail pelo provider HTTP real em prod e sandbox, falhando explicitamente sem fallback para o console.
@Slf4j
@Component
@Profile("prod | sandbox")
public class HttpEmailProviderAdapter implements EmailSenderPort {

    private static final String PROVIDER_NAME = "email-http";

    private final HttpProviderClient client;
    private final String sender;

    public HttpEmailProviderAdapter(NotificationProperties props, MeterRegistry meterRegistry) {
        Objects.requireNonNull(props, "props must not be null");
        var cfg = props.getProviders().getEmail();
        var cb = new ProviderCircuitBreaker(PROVIDER_NAME,
                props.getCircuitBreaker().getFailureThreshold(),
                props.getCircuitBreaker().getWaitDurationOpenState(),
                meterRegistry);
        this.client = new HttpProviderClient(PROVIDER_NAME, cfg, cb);
        this.sender = cfg.getSender();
    }

    @Override
    public SendResult sendEmail(ChannelAddress to, String subject, String body) {
        Objects.requireNonNull(to, "to must not be null");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("from", sender);
        payload.put("to", to.address());
        payload.put("subject", subject);
        payload.put("text", body);

        String messageId = client.send("", payload);
        return new SendResult(PROVIDER_NAME, messageId);
    }
}
