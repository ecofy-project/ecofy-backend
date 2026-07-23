package br.com.ecofy.ms_notification.adapters.out.external;

import br.com.ecofy.ms_notification.adapters.out.external.support.HttpProviderClient;
import br.com.ecofy.ms_notification.adapters.out.external.support.ProviderCircuitBreaker;
import br.com.ecofy.ms_notification.config.NotificationProperties;
import br.com.ecofy.ms_notification.core.domain.valueobject.ChannelAddress;
import br.com.ecofy.ms_notification.core.port.out.PushSenderPort;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

// Envia push pelo provider HTTP real em prod e sandbox, com timeout e circuit breaker.
@Component
@Profile("prod | sandbox")
public class HttpPushProviderAdapter implements PushSenderPort {

    private static final String PROVIDER_NAME = "push-http";

    private final HttpProviderClient client;

    public HttpPushProviderAdapter(NotificationProperties props, MeterRegistry meterRegistry) {
        Objects.requireNonNull(props, "props must not be null");
        var cfg = props.getProviders().getPush();
        var cb = new ProviderCircuitBreaker(PROVIDER_NAME,
                props.getCircuitBreaker().getFailureThreshold(),
                props.getCircuitBreaker().getWaitDurationOpenState(),
                meterRegistry);
        this.client = new HttpProviderClient(PROVIDER_NAME, cfg, cb);
    }

    @Override
    public SendResult sendPush(ChannelAddress to, String title, String body) {
        Objects.requireNonNull(to, "to must not be null");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("token", to.address());
        payload.put("title", title);
        payload.put("body", body);

        String messageId = client.send("", payload);
        return new SendResult(PROVIDER_NAME, messageId);
    }
}
