package br.com.ecofy.auth.adapters.out.external;

import br.com.ecofy.auth.adapters.in.web.correlation.CorrelationId;
import br.com.ecofy.auth.config.UsersMsProperties;
import br.com.ecofy.auth.core.domain.AuthUser;
import br.com.ecofy.auth.core.port.out.SyncUserToUsersMsPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// Sincroniza os dados de autenticação do usuário com o ms-users.
@Slf4j
@Component
@RequiredArgsConstructor
public class MsUsersClientAdapter implements SyncUserToUsersMsPort {

    private final RestClient msUsersRestClient;
    private final UsersMsProperties props;

    // Propaga os dados atualizados do usuário e o correlation ID para o ms-users.
    @Override
    public void upsertUser(AuthUser user) {
        if (!props.enabled()) {
            return;
        }

        String url = "/internal/users/{authUserId}";

        var payload = new UpsertUserFromAuthPayload(
                user.id().value().toString(),
                user.email().value(),
                user.firstName(),
                user.lastName(),
                user.firstName() + " " + user.lastName(),
                user.isEmailVerified(),
                user.status().name(),
                user.locale() != null ? user.locale() : "pt-BR"
        );

        String correlationId = CorrelationId.current();

        try {
            var spec = msUsersRestClient.put()
                    .uri(url, user.id().value().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Internal-Token", props.internalToken());

            if (correlationId != null && !correlationId.isBlank()) {
                spec = spec.header(CorrelationId.HEADER, correlationId);
            }

            spec.body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info(
                    "[ms-auth] Synced user to ms-users authUserId={}",
                    user.id().value()
            );
        } catch (Exception ex) {
            log.error(
                    "[ms-auth] Failed to sync user to ms-users authUserId={} err={}",
                    user.id().value(),
                    ex.getMessage()
            );
        }
    }

    // Representa os dados enviados ao ms-users durante a sincronização.
    private record UpsertUserFromAuthPayload(
            String authUserId,
            String email,
            String firstName,
            String lastName,
            String fullName,
            Boolean emailVerified,
            String status,
            String locale
    ) {}
}
