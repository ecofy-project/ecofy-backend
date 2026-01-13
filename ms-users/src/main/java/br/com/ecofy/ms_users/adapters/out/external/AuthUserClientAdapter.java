package br.com.ecofy.ms_users.adapters.out.external;

import br.com.ecofy.ms_users.config.UsersProperties;
import br.com.ecofy.ms_users.core.port.out.LoadAuthUserFromAuthMsPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
public class AuthUserClientAdapter implements LoadAuthUserFromAuthMsPort {

    private static final String CLIENT_NAME = "ms-auth";
    private static final String PATH_TEMPLATE = "/api/auth/v1/users/%s";

    private final UsersProperties.ExternalAuth externalAuthProps;
    private final RestTemplate restTemplate;

    public AuthUserClientAdapter(UsersProperties props) {
        Objects.requireNonNull(props, "props must not be null");
        this.externalAuthProps = Objects.requireNonNull(props.externalAuth(), "props.externalAuth must not be null");

        // Mantém simples com RestTemplate, mas evita criar new em campo (testabilidade).
        // Se quiser timeouts, injete RestTemplateBuilder e configure connect/read timeout.
        this.restTemplate = new RestTemplate();
    }

    @Override
    public AuthUserSnapshot loadByUserId(UUID userId) {
        Objects.requireNonNull(userId, "userId must not be null");

        String baseUrl = requireBaseUrl();
        String url = normalizeBaseUrl(baseUrl) + PATH_TEMPLATE.formatted(userId);

        log.debug("[AuthUserClientAdapter] - [loadByUserId] -> client={} url={} userId={}", CLIENT_NAME, url, userId);

        try {
            RequestEntity<Void> req = RequestEntity.get(URI.create(url)).build();
            ResponseEntity<AuthUserSnapshot> resp = restTemplate.exchange(req, AuthUserSnapshot.class);

            HttpStatusCode status = resp.getStatusCode();
            AuthUserSnapshot body = resp.getBody();

            if (status.is2xxSuccessful() && body != null) {
                log.info(
                        "[AuthUserClientAdapter] - [loadByUserId] -> client={} userId={} status={} found=true hasEmail={} hasPhone={} hasFullName={}",
                        CLIENT_NAME,
                        userId,
                        status.value(),
                        body.email() != null && !body.email().isBlank(),
                        body.phone() != null && !body.phone().isBlank(),
                        body.fullName() != null && !body.fullName().isBlank()
                );
                return body;
            }

            log.warn(
                    "[AuthUserClientAdapter] - [loadByUserId] -> client={} userId={} status={} found=false reason=no_body",
                    CLIENT_NAME,
                    userId,
                    status.value()
            );
            return emptySnapshot(userId);

        } catch (RestClientResponseException ex) {
            log.warn(
                    "[AuthUserClientAdapter] - [loadByUserId] -> client={} userId={} status={} reason={}",
                    CLIENT_NAME,
                    userId,
                    ex.getRawStatusCode(),
                    safeMessage(ex.getMessage())
            );
            return emptySnapshot(userId);

        } catch (Exception ex) {
            log.warn(
                    "[AuthUserClientAdapter] - [loadByUserId] -> client={} userId={} reason={}",
                    CLIENT_NAME,
                    userId,
                    safeMessage(ex.getMessage())
            );
            return emptySnapshot(userId);
        }
    }

    private String requireBaseUrl() {
        String baseUrl = externalAuthProps.baseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            // Falhar cedo ajuda a identificar env mal configurada.
            throw new IllegalStateException("usersProperties.externalAuth.baseUrl must not be blank");
        }
        return baseUrl;
    }

    private static String normalizeBaseUrl(String baseUrl) {
        String v = baseUrl.trim();
        return v.endsWith("/") ? v.substring(0, v.length() - 1) : v;
    }

    private static AuthUserSnapshot emptySnapshot(UUID userId) {
        return new AuthUserSnapshot(userId, null, null, null, null);
    }

    private static String safeMessage(String message) {
        if (message == null || message.isBlank()) return "<empty>";
        // Evita log gigantesco / com PII (headers etc.) em certos exceptions.
        String m = message.replaceAll("[\\r\\n]+", " ").trim();
        return m.length() > 240 ? m.substring(0, 240) + "..." : m;
    }
}
