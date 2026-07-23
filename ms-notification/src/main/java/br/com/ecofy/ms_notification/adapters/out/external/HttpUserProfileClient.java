package br.com.ecofy.ms_notification.adapters.out.external;

import br.com.ecofy.ms_notification.adapters.out.external.support.ProviderCircuitBreaker;
import br.com.ecofy.ms_notification.config.NotificationProperties;
import br.com.ecofy.ms_notification.core.domain.valueobject.UserId;
import br.com.ecofy.ms_notification.core.port.out.LoadUserContactInfoPort;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

// Resolve contatos e preferências no ms-users via HTTP interno em prod e sandbox, expondo falhas em vez de mascará-las.
@Slf4j
@Component
@Profile("prod | sandbox")
public class HttpUserProfileClient implements LoadUserContactInfoPort {

    private static final String CB_NAME = "user-profile";

    private final RestClient restClient;
    private final String serviceToken;
    private final ProviderCircuitBreaker circuitBreaker;
    private final MeterRegistry meterRegistry;

    public HttpUserProfileClient(NotificationProperties props, MeterRegistry meterRegistry) {
        Objects.requireNonNull(props, "props must not be null");
        var cfg = props.getClients().getUserProfile();
        this.serviceToken = cfg.getServiceToken();
        this.meterRegistry = meterRegistry;

        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(cfg.getConnectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(cfg.getReadTimeoutMs()));

        this.restClient = RestClient.builder()
                .baseUrl(cfg.getBaseUrl())
                .requestFactory(factory)
                .build();

        this.circuitBreaker = new ProviderCircuitBreaker(CB_NAME,
                props.getCircuitBreaker().getFailureThreshold(),
                props.getCircuitBreaker().getWaitDurationOpenState(),
                meterRegistry);
    }

    @Override
    public Optional<UserContactInfo> load(UserId userId) {
        Objects.requireNonNull(userId, "userId must not be null");

        if (!circuitBreaker.allowRequest()) {
            meterRegistry.counter("ecofy.notification.userprofile.failure.total", "reason", "circuit_open").increment();
            log.warn("[HttpUserProfileClient] - [load] -> circuit OPEN, skipping ms-users call userId={}", userId.value());
            return Optional.empty();
        }

        try {
            Map<?, ?> body = restClient.get()
                    .uri("/internal/v1/users/{userId}/contact", userId.value())
                    .header("Authorization", "Bearer " + serviceToken)
                    .retrieve()
                    .body(Map.class);

            circuitBreaker.recordSuccess();
            if (body == null) {
                return Optional.empty();
            }
            return Optional.of(new UserContactInfo(
                    str(body.get("email")), str(body.get("phoneE164")), str(body.get("deviceToken"))));

        } catch (RuntimeException ex) {
            circuitBreaker.recordFailure();
            meterRegistry.counter("ecofy.notification.userprofile.failure.total", "reason", "http_error").increment();
            log.error("[HttpUserProfileClient] - [load] -> ms-users call failed userId={} error={}",
                    userId.value(), ex.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }
}
