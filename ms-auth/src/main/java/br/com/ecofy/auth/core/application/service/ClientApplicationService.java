package br.com.ecofy.auth.core.application.service;

import br.com.ecofy.auth.core.application.exception.AuthErrorCode;
import br.com.ecofy.auth.core.application.exception.AuthException;
import br.com.ecofy.auth.core.domain.ClientApplication;
import br.com.ecofy.auth.core.domain.enums.ClientType;
import br.com.ecofy.auth.core.domain.enums.GrantType;
import br.com.ecofy.auth.core.port.in.RegisterClientApplicationUseCase;
import br.com.ecofy.auth.core.port.out.PasswordHashingPort;
import br.com.ecofy.auth.core.port.out.SaveClientApplicationPort;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// Serviço responsável por registrar Client Applications (client_id/secret) e validar grants/redirect URIs no ms-auth.
@Slf4j
@Service
public class ClientApplicationService implements RegisterClientApplicationUseCase {

    private final SaveClientApplicationPort saveClientApplicationPort;
    private final PasswordHashingPort passwordHashingPort;
    private final SecureRandom secureRandom = new SecureRandom();

    // Inicializa o serviço com as portas de persistência e hashing necessárias para criação de clients.
    public ClientApplicationService(
            SaveClientApplicationPort saveClientApplicationPort,
            PasswordHashingPort passwordHashingPort
    ) {
        this.saveClientApplicationPort = Objects.requireNonNull(
                saveClientApplicationPort,
                "saveClientApplicationPort must not be null"
        );
        this.passwordHashingPort = Objects.requireNonNull(
                passwordHashingPort,
                "passwordHashingPort must not be null"
        );
    }

    // Registra um novo client, gera client_id/secret (quando aplicável), valida grants/redirectUris e persiste no repositório.
    @Override
    public ClientApplication register(RegisterClientCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        log.debug(
                "[ClientApplicationService] - [register] -> Registrando novo client name={} type={} firstParty={}",
                command.name(),
                command.clientType(),
                command.firstParty()
        );

        String clientId = generateClientId(command.name());
        String rawSecret = requiresSecret(command.clientType())
                ? generateSecret()
                : null;

        if (rawSecret != null) {
            log.debug(
                    "[ClientApplicationService] - [register] -> Client requer segredo gerado clientId={} type={}",
                    clientId,
                    command.clientType()
            );
        } else {
            log.debug(
                    "[ClientApplicationService] - [register] -> Client não requer segredo clientId={} type={}",
                    clientId,
                    command.clientType()
            );
        }

        // Gera o hash do client secret (quando existir) para armazenar somente o valor derivado no banco.
        String secretHash = rawSecret != null
                ? passwordHashingPort.hash(rawSecret).value()
                : null;

        Set<GrantType> effectiveGrants = resolveEffectiveGrants(
                command.clientType(),
                command.grantTypes()
        );

        log.debug(
                "[ClientApplicationService] - [register] -> Grants resolvidos para clientId={} grants={}",
                clientId,
                effectiveGrants
        );

        validateGrants(command.clientType(), effectiveGrants);
        validateRedirectUrisIfNeeded(effectiveGrants, command.redirectUris());

        ClientApplication client = ClientApplication.create(
                command.name(),
                command.clientType(),
                effectiveGrants,
                command.redirectUris(),
                command.scopes(),
                command.firstParty(),
                clientId,
                secretHash
        );

        ClientApplication saved = saveClientApplicationPort.save(client);

        log.debug(
                "[ClientApplicationService] - [register] -> Client registrado com sucesso clientId={} type={} active={}",
                saved.clientId(),
                saved.clientType(),
                saved.isActive()
        );

        return saved;
    }

    // Indica se o tipo de client exige client secret (ex.: CONFIDENTIAL e MACHINE_TO_MACHINE).
    private boolean requiresSecret(ClientType clientType) {
        return clientType == ClientType.CONFIDENTIAL
                || clientType == ClientType.MACHINE_TO_MACHINE;
    }

    // Resolve os grants efetivos do client usando os solicitados (se existirem) ou defaults conforme o tipo.
    private Set<GrantType> resolveEffectiveGrants(ClientType clientType, Set<GrantType> requested) {
        if (requested != null && !requested.isEmpty()) {
            return requested;
        }

        return switch (clientType) {
            case CONFIDENTIAL -> Set.of(
                    GrantType.AUTHORIZATION_CODE,
                    GrantType.REFRESH_TOKEN,
                    GrantType.CLIENT_CREDENTIALS
            );
            case PUBLIC, SPA -> Set.of(
                    GrantType.AUTHORIZATION_CODE,
                    GrantType.REFRESH_TOKEN
            );
            case MACHINE_TO_MACHINE -> Set.of(
                    GrantType.CLIENT_CREDENTIALS
            );
        };
    }

    // Valida se o conjunto de grants é compatível com o tipo do client (ex.: M2M exige CLIENT_CREDENTIALS).
    private void validateGrants(ClientType clientType, Set<GrantType> grants) {
        switch (clientType) {
            case MACHINE_TO_MACHINE -> {
                if (!grants.contains(GrantType.CLIENT_CREDENTIALS)) {
                    log.warn(
                            "[ClientApplicationService] - [validateGrants] -> M2M sem CLIENT_CREDENTIALS grants={}",
                            grants
                    );
                    throw new AuthException(
                            AuthErrorCode.CLIENT_NOT_ALLOWED_FOR_GRANT_TYPE,
                            "M2M client must support CLIENT_CREDENTIALS grant"
                    );
                }
                for (GrantType g : grants) {
                    if (g == GrantType.AUTHORIZATION_CODE || g == GrantType.PASSWORD) {
                        log.warn(
                                "[ClientApplicationService] - [validateGrants] -> Grant inválido para M2M grant={} grants={}",
                                g,
                                grants
                        );
                        throw new AuthException(
                                AuthErrorCode.CLIENT_NOT_ALLOWED_FOR_GRANT_TYPE,
                                "M2M client cannot use " + g + " grant"
                        );
                    }
                }
            }
            case PUBLIC, SPA -> {
                if (grants.contains(GrantType.CLIENT_CREDENTIALS)) {
                    log.warn(
                            "[ClientApplicationService] - [validateGrants] -> PUBLIC/SPA com CLIENT_CREDENTIALS grants={}",
                            grants
                    );
                    throw new AuthException(
                            AuthErrorCode.CLIENT_NOT_ALLOWED_FOR_GRANT_TYPE,
                            "PUBLIC/SPA clients cannot use CLIENT_CREDENTIALS grant"
                    );
                }
            }
            case CONFIDENTIAL -> {
                // CONFIDENTIAL permite os grants definidos, desde que consistentes com a configuração.
            }
        }
    }

    // Valida a obrigatoriedade de redirectUris quando o grant AUTHORIZATION_CODE estiver habilitado.
    private void validateRedirectUrisIfNeeded(Set<GrantType> grants, Set<String> redirectUris) {
        if (grants.contains(GrantType.AUTHORIZATION_CODE)) {
            if (redirectUris == null || redirectUris.isEmpty()) {
                log.warn(
                        "[ClientApplicationService] - [validateRedirectUrisIfNeeded] -> AUTHORIZATION_CODE sem redirectUris"
                );
                throw new AuthException(
                        AuthErrorCode.INVALID_REDIRECT_URI,
                        "AUTHORIZATION_CODE grant requires at least one redirectUri configured"
                );
            }
        }
    }

    // Gera um client_id com prefixo "eco_" usando bytes aleatórios codificados em Base64 URL-safe.
    private String generateClientId(String name) {
        byte[] bytes = new byte[12];
        secureRandom.nextBytes(bytes);

        String clientId = "eco_" + Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        log.debug(
                "[ClientApplicationService] - [generateClientId] -> clientId gerado name={} clientId={}",
                name,
                clientId
        );

        return clientId;
    }

    // Gera um client secret aleatório em Base64 URL-safe (o valor nunca deve ser impresso integralmente em logs).
    private String generateSecret() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);

        String secret = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        log.debug("[ClientApplicationService] - [generateSecret] -> clientSecret gerado (valor não será logado)");

        return secret;
    }
}