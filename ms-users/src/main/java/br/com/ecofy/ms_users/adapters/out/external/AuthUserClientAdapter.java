package br.com.ecofy.ms_users.adapters.out.external;

import br.com.ecofy.ms_users.config.UsersProperties;
import br.com.ecofy.ms_users.core.port.out.LoadAuthUserFromAuthMsPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
public class AuthUserClientAdapter implements LoadAuthUserFromAuthMsPort {

    private static final String CLIENT_NAME = "ms-auth";
    private static final String PATH_TEMPLATE = "/api/auth/v1/users/%s";

    private final UsersProperties.ExternalAuth externalAuthProps;
    private final RestTemplate restTemplate;

    // Inicializa o adapter carregando as configurações de acesso ao ms-auth e criando o cliente HTTP (RestTemplate).
    public AuthUserClientAdapter(UsersProperties props) {
        Objects.requireNonNull(props, "props must not be null");
        this.externalAuthProps = Objects.requireNonNull(props.externalAuth(), "props.externalAuth must not be null");
        this.restTemplate = new RestTemplate();
    }

    // Consulta o ms-auth por userId e retorna o snapshot do usuário; em falhas/ausência, retorna um snapshot vazio.
    @Override
    public AuthUserSnapshot loadByUserId(UUID userId) {
        Objects.requireNonNull(userId, "userId must not be null");

        String baseUrl = requireBaseUrl();
        String url = normalizeBaseUrl(baseUrl) + PATH_TEMPLATE.formatted(userId);

        log.debug("[AuthUserClientAdapter] loadByUserId client={} url={} userId={}", CLIENT_NAME, url, userId);

        try {
            RequestEntity<Void> req = RequestEntity.get(URI.create(url)).build();
            ResponseEntity<AuthUserSnapshot> resp = restTemplate.exchange(req, AuthUserSnapshot.class);

            var status = resp.getStatusCode();
            var body = resp.getBody();

            if (status.is2xxSuccessful() && body != null) {
                log.info(
                        "[AuthUserClientAdapter] loadByUserId client={} userId={} status={} found=true hasEmail={} hasPhone={} hasFullName={}",
                        CLIENT_NAME,
                        userId,
                        status.value(),
                        hasText(body.email()),
                        hasText(body.phone()),
                        hasText(body.fullName())
                );
                return body;
            }

            log.warn(
                    "[AuthUserClientAdapter] loadByUserId client={} userId={} status={} found=false reason={}",
                    CLIENT_NAME,
                    userId,
                    status.value(),
                    (body == null ? "no_body" : "non_2xx")
            );
            return emptySnapshot(userId);

        } catch (RestClientResponseException ex) {
            int status = safeStatus(ex);

            log.warn(
                    "[AuthUserClientAdapter] loadByUserId client={} userId={} status={} reason={} body={}",
                    CLIENT_NAME,
                    userId,
                    status,
                    safeMessage(ex.getMessage()),
                    safeBody(ex)
            );
            return emptySnapshot(userId);

        } catch (Exception ex) {
            log.warn(
                    "[AuthUserClientAdapter] loadByUserId client={} userId={} reason={}",
                    CLIENT_NAME,
                    userId,
                    safeMessage(ex.getMessage()),
                    ex
            );
            return emptySnapshot(userId);
        }
    }

    // Obtém a baseUrl configurada para o ms-auth e falha caso esteja ausente/inválida.
    private String requireBaseUrl() {
        String baseUrl = externalAuthProps.baseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("usersProperties.externalAuth.baseUrl must not be blank");
        }
        return baseUrl;
    }

    // Normaliza a baseUrl removendo espaços e barra final para evitar URLs duplicadas/malformadas.
    private static String normalizeBaseUrl(String baseUrl) {
        String v = baseUrl.trim();
        return v.endsWith("/") ? v.substring(0, v.length() - 1) : v;
    }

    // Cria um AuthUserSnapshot “vazio” para representar ausência/indisponibilidade do usuário no ms-auth.
    private static AuthUserSnapshot emptySnapshot(UUID userId) {
        return new AuthUserSnapshot(userId, null, null, null, null);
    }

    // Valida se a string possui conteúdo útil (não nula e não em branco).
    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    // Extrai o status HTTP de uma exceção do RestTemplate de forma defensiva, com fallback.
    private static int safeStatus(RestClientResponseException ex) {
        try {
            return ex.getStatusCode().value(); // compatível com Spring 6.2+/Boot 4
        } catch (Exception ignored) {
            return -1;
        }
    }

    // Extrai e sanitiza o corpo de erro da resposta HTTP para logging seguro e limitado.
    private static String safeBody(RestClientResponseException ex) {
        try {
            String body = ex.getResponseBodyAsString();
            if (body == null || body.isBlank()) return "<empty>";
            String b = body.replaceAll("[\\r\\n]+", " ").trim();
            return b.length() > 240 ? b.substring(0, 240) + "..." : b;
        } catch (Exception ignored) {
            return "<unavailable>";
        }
    }

    // Sanitiza e limita mensagens de erro para logging consistente e sem quebra de linha.
    private static String safeMessage(String message) {
        if (message == null || message.isBlank()) return "<empty>";
        String m = message.replaceAll("[\\r\\n]+", " ").trim();
        return m.length() > 240 ? m.substring(0, 240) + "..." : m;
    }
}
