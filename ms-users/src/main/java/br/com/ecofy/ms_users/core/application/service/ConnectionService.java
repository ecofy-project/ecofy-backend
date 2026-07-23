package br.com.ecofy.ms_users.core.application.service;

import br.com.ecofy.ms_users.core.application.command.CreateConnectionCommand;
import br.com.ecofy.ms_users.core.application.result.ConnectionResult;
import br.com.ecofy.ms_users.core.domain.Connection;
import br.com.ecofy.ms_users.core.domain.enums.AccountProvider;
import br.com.ecofy.ms_users.core.domain.enums.ConnectionType;
import br.com.ecofy.ms_users.core.domain.exception.BusinessValidationException;
import br.com.ecofy.ms_users.core.domain.exception.IdempotencyViolationException;
import br.com.ecofy.ms_users.core.domain.valueobject.UserId;
import br.com.ecofy.ms_users.core.port.in.CreateConnectionUseCase;
import br.com.ecofy.ms_users.core.port.in.ListConnectionsUseCase;
import br.com.ecofy.ms_users.core.port.out.IdempotencyOutcome;
import br.com.ecofy.ms_users.core.port.out.IdempotencyPort;
import br.com.ecofy.ms_users.core.port.out.LoadConnectionsPort;
import br.com.ecofy.ms_users.core.port.out.SaveConnectionPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

// Orquestra a criação e consulta de conexões vinculadas aos usuários.
@Slf4j
@Service
public class ConnectionService
        implements CreateConnectionUseCase, ListConnectionsUseCase {

    private static final String OP_CREATE_CONNECTION =
            "users.createConnection";

    private final SaveConnectionPort savePort;
    private final LoadConnectionsPort loadPort;
    private final IdempotencyPort idempotencyPort;
    private final Duration idempotencyTtl;

    public ConnectionService(
            SaveConnectionPort savePort,
            LoadConnectionsPort loadPort,
            IdempotencyPort idempotencyPort,
            @Value("${ecofy.users.idempotency.ttl:24h}")
            Duration idempotencyTtl
    ) {
        this.savePort = Objects.requireNonNull(
                savePort,
                "savePort must not be null"
        );
        this.loadPort = Objects.requireNonNull(
                loadPort,
                "loadPort must not be null"
        );
        this.idempotencyPort = Objects.requireNonNull(
                idempotencyPort,
                "idempotencyPort must not be null"
        );
        this.idempotencyTtl = Objects.requireNonNull(
                idempotencyTtl,
                "idempotencyTtl must not be null"
        );
    }

    // Registra uma conexão validada com proteção contra requisições duplicadas.
    @Override
    public ConnectionResult create(CreateConnectionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        validate(command);

        final UUID userId = command.userId();
        final String idempotencyKey =
                command.idempotencyKey().trim();

        final ConnectionType type =
                parseConnectionType(command.type());
        final AccountProvider provider =
                parseAccountProvider(command.provider());
        final Map<String, Object> metadata =
                command.metadata() == null
                        ? Map.of()
                        : command.metadata();

        final String requestHash =
                requestHash(userId, type, provider);

        final IdempotencyOutcome outcome =
                idempotencyPort.registerOnce(
                        OP_CREATE_CONNECTION,
                        idempotencyKey,
                        requestHash,
                        idempotencyTtl
                );

        if (outcome == IdempotencyOutcome.CONFLICT) {
            log.warn(
                    "[ConnectionService] - [create] -> Conflito de idempotência operation={} userId={} idempotencyKey={}",
                    OP_CREATE_CONNECTION,
                    userId,
                    idempotencyKey
            );

            throw new IdempotencyViolationException(
                    "Idempotency key already used for operation="
                            + OP_CREATE_CONNECTION
            );
        }

        if (outcome == IdempotencyOutcome.DUPLICATE) {
            var existing = loadPort.findByUserId(userId)
                    .stream()
                    .filter(connection ->
                            connection.getType() == type
                                    && connection.getProvider() == provider
                    )
                    .findFirst();

            if (existing.isPresent()) {
                log.info(
                        "[ConnectionService] - [create] -> Retentativa idempotente connectionId={} userId={}",
                        existing.get().getId(),
                        userId
                );

                return toResult(existing.get());
            }
        }

        final Instant now = Instant.now();

        final Connection connection = Connection.builder()
                .id(UUID.randomUUID())
                .userId(UserId.of(userId))
                .type(type)
                .provider(provider)
                .metadata(metadata)
                .createdAt(now)
                .build();

        final Connection saved = savePort.save(connection);

        log.info(
                "[ConnectionService] - [create] -> status=created connectionId={} userId={} type={} provider={} hasMetadata={}",
                saved.getId(),
                saved.getUserId().value(),
                saved.getType(),
                saved.getProvider(),
                saved.getMetadata() != null
                        && !saved.getMetadata().isEmpty()
        );

        return toResult(saved);
    }

    // Recupera conexões paginadas delegando os critérios ao repositório.
    @Override
    public br.com.ecofy.ms_users.core.application.pagination
            .PagedResult<ConnectionResult> listByUserId(
            UUID userId,
            br.com.ecofy.ms_users.core.application.pagination
                    .PageQuery query
    ) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(query, "query must not be null");

        var page = loadPort.findByUserId(userId, query)
                .map(ConnectionService::toResult);

        log.debug(
                "[ConnectionService] - [listByUserId(paged)] -> userId={} page={} size={} total={}",
                userId,
                page.page(),
                page.size(),
                page.totalElements()
        );

        return page;
    }

    @Override
    public List<ConnectionResult> listByUserId(UUID userId) {
        Objects.requireNonNull(userId, "userId must not be null");

        final List<ConnectionResult> out =
                loadPort.findByUserId(userId)
                        .stream()
                        .map(ConnectionService::toResult)
                        .toList();

        log.debug(
                "[ConnectionService] - [listByUserId] -> userId={} size={}",
                userId,
                out.size()
        );

        return out;
    }

    // Valida os campos obrigatórios antes de criar a conexão.
    private static void validate(CreateConnectionCommand command) {
        if (command.userId() == null) {
            throw new BusinessValidationException(
                    "userId is required"
            );
        }

        if (command.type() == null || command.type().isBlank()) {
            throw new BusinessValidationException(
                    "type is required"
            );
        }

        if (command.provider() == null
                || command.provider().isBlank()) {
            throw new BusinessValidationException(
                    "provider is required"
            );
        }

        if (command.idempotencyKey() == null
                || command.idempotencyKey().isBlank()) {
            throw new BusinessValidationException(
                    "idempotencyKey is required"
            );
        }
    }

    // Converte o tipo informado e rejeita valores incompatíveis.
    private static ConnectionType parseConnectionType(String raw) {
        final String value = raw == null ? null : raw.trim();

        try {
            return ConnectionType.valueOf(value);
        } catch (Exception ex) {
            throw new BusinessValidationException(
                    "Invalid connection type: " + raw
            );
        }
    }

    // Converte o provedor informado com fallback para a categoria genérica.
    private static AccountProvider parseAccountProvider(String raw) {
        final String value = raw == null ? null : raw.trim();

        try {
            return AccountProvider.valueOf(value);
        } catch (Exception ex) {
            return AccountProvider.OTHER;
        }
    }

    // Gera uma assinatura determinística para validar a idempotência.
    private static String requestHash(
            UUID userId,
            ConnectionType type,
            AccountProvider provider
    ) {
        return sha256(
                userId
                        + "|"
                        + type.name()
                        + "|"
                        + provider.name()
        );
    }

    private static ConnectionResult toResult(Connection connection) {
        return new ConnectionResult(
                connection.getId(),
                connection.getUserId().value(),
                connection.getType().name(),
                connection.getProvider().name(),
                connection.getMetadata(),
                connection.getCreatedAt()
        );
    }

    // Calcula o hash da requisição com fallback para falhas inesperadas.
    private static String sha256(String value) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");
            byte[] output = digest.digest(
                    value.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(output);
        } catch (Exception ex) {
            return "sha256_error";
        }
    }
}
