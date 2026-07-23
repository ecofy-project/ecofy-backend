package br.com.ecofy.ms_notification.adapters.out.external.support;

import br.com.ecofy.ms_notification.config.NotificationProperties;
import br.com.ecofy.ms_notification.core.domain.enums.DeliveryErrorCategory;
import br.com.ecofy.ms_notification.core.domain.exception.ProviderDeliveryException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

// Executa chamadas HTTP a providers externos com timeout, classificação de erro e circuit breaker, sem logar dados sensíveis.
@Slf4j
public class HttpProviderClient {

    private final String providerName;
    private final NotificationProperties.Providers.Provider config;
    private final RestClient restClient;
    private final ProviderCircuitBreaker circuitBreaker;

    public HttpProviderClient(String providerName,
                              NotificationProperties.Providers.Provider config,
                              ProviderCircuitBreaker circuitBreaker) {
        this.providerName = providerName;
        this.config = config;
        this.circuitBreaker = circuitBreaker;

        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(config.getConnectTimeout() != null ? config.getConnectTimeout() : Duration.ofSeconds(3));
        factory.setReadTimeout(config.getReadTimeout() != null ? config.getReadTimeout() : Duration.ofSeconds(10));

        this.restClient = RestClient.builder()
                .baseUrl(config.getBaseUrl())
                .requestFactory(factory)
                .build();
    }

    // Envia o payload ao provider e devolve o messageId, falhando rápido com o circuito aberto.
    public String send(String path, Map<String, Object> body) {
        requireConfigured();

        if (!circuitBreaker.allowRequest()) {
            throw new ProviderDeliveryException(
                    "Circuit OPEN for provider=" + providerName, DeliveryErrorCategory.TEMPORARY_FAILURE, "CIRCUIT_OPEN");
        }

        try {
            Map<?, ?> response = restClient.post()
                    .uri(path == null ? "" : path)
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            circuitBreaker.recordSuccess();
            Object id = response != null ? response.get("id") : null;
            return id != null ? String.valueOf(id) : UUID.randomUUID().toString();

        } catch (RestClientResponseException httpError) {
            ProviderDeliveryException classified = classifyHttp(httpError);
            if (classified.getCategory().retryable()) {
                circuitBreaker.recordFailure();
            }
            throw classified;

        } catch (RuntimeException networkError) {
            // Timeout / conexão recusada / IO → transitório.
            circuitBreaker.recordFailure();
            throw new ProviderDeliveryException(
                    "Provider call failed (network) provider=" + providerName,
                    DeliveryErrorCategory.TEMPORARY_FAILURE, "NETWORK_ERROR", null, null, networkError);
        }
    }

    private void requireConfigured() {
        if (!config.isEnabled()) {
            // Nunca fallback silencioso para fake em prod (§6.3): canal indisponível é falha explícita.
            throw new ProviderDeliveryException(
                    "Provider disabled provider=" + providerName, DeliveryErrorCategory.PERMANENT_FAILURE, "PROVIDER_DISABLED");
        }
        if (config.getBaseUrl() == null || config.getBaseUrl().isBlank()
                || config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new ProviderDeliveryException(
                    "Provider misconfigured (missing base-url/api-key) provider=" + providerName,
                    DeliveryErrorCategory.PROVIDER_BLOCKED, "PROVIDER_MISCONFIGURED");
        }
    }

    private ProviderDeliveryException classifyHttp(RestClientResponseException ex) {
        int status = ex.getStatusCode().value();
        Duration retryAfter = parseRetryAfter(ex.getResponseHeaders() != null
                ? ex.getResponseHeaders().getFirst("Retry-After") : null);

        DeliveryErrorCategory category;
        String code;
        if (status == 429) {
            category = DeliveryErrorCategory.RATE_LIMITED;
            code = "RATE_LIMITED";
        } else if (status == 408 || status == 502 || status == 503 || status == 504) {
            category = DeliveryErrorCategory.TEMPORARY_FAILURE;
            code = "HTTP_" + status;
        } else if (status == 401 || status == 403) {
            category = DeliveryErrorCategory.PROVIDER_BLOCKED;
            code = "HTTP_" + status;
        } else if (status == 422 || status == 400) {
            category = DeliveryErrorCategory.PERMANENT_FAILURE;
            code = "HTTP_" + status;
        } else {
            category = DeliveryErrorCategory.PERMANENT_FAILURE;
            code = "HTTP_" + status;
        }
        return new ProviderDeliveryException(
                "Provider returned status=" + status + " provider=" + providerName, category, code, status, retryAfter, null);
    }

    private static Duration parseRetryAfter(String header) {
        if (header == null || header.isBlank()) return null;
        try {
            return Duration.ofSeconds(Long.parseLong(header.trim()));
        } catch (NumberFormatException ignored) {
            return null; // formato HTTP-date não suportado aqui; backoff exponencial assume.
        }
    }

    public String providerName() {
        return providerName;
    }
}
