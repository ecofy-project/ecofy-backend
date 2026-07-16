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

@Slf4j
@Service
public class ConnectionService implements CreateConnectionUseCase, ListConnectionsUseCase {

    private static final String OP_CREATE_CONNECTION = "users.createConnection";

    private final SaveConnectionPort savePort;
    private final LoadConnectionsPort loadPort;
    private final IdempotencyPort idempotencyPort;
    private final Duration idempotencyTtl;

    // Inicializa o serviço de conexões, injetando portas de persistência/consulta e política de TTL para idempotência.
    public ConnectionService(
            SaveConnectionPort savePort,
            LoadConnectionsPort loadPort,
            IdempotencyPort idempotencyPort,
            @Value("${ecofy.users.idempotency.ttl:24h}") Duration idempotencyTtl
    ) {
        this.savePort = Objects.requireNonNull(savePort, "savePort must not be null");
        this.loadPort = Objects.requireNonNull(loadPort, "loadPort must not be null");
        this.idempotencyPort = Objects.requireNonNull(idempotencyPort, "idempotencyPort must not be null");
        this.idempotencyTtl = Objects.requireNonNull(idempotencyTtl, "idempotencyTtl must not be null");
    }

    // Cria uma Connection para o usuário, aplicando validações, controle de idempotência e persistindo via SaveConnectionPort.
    @Override
    public ConnectionResult create(CreateConnectionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        validate(command);

        final UUID userId = command.userId();
        final String idempotencyKey = command.idempotencyKey().trim();

        final ConnectionType type = parseConnectionType(command.type());
        final AccountProvider provider = parseAccountProvider(command.provider());
        final Map<String, Object> metadata = (command.metadata() == null) ? Map.of() : command.metadata();

        final String requestHash = requestHash(userId, type, provider);

        final IdempotencyOutcome outcome = idempotencyPort.registerOnce(
                OP_CREATE_CONNECTION,
                idempotencyKey,
                requestHash,
                idempotencyTtl
        );

        if (outcome == IdempotencyOutcome.CONFLICT) {
            log.warn(
                    "[ConnectionService] - [create] -> status=idempotency_conflict operation={} userId={} idempotencyKey={}",
                    OP_CREATE_CONNECTION, userId, idempotencyKey
            );
            throw new IdempotencyViolationException("Idempotency key already used for operation=" + OP_CREATE_CONNECTION);
        }
        if (outcome == IdempotencyOutcome.DUPLICATE) {
            // Retry legítimo: retorna a conexão já criada (mesmo type+provider) sem duplicar.
            var existing = loadPort.findByUserId(userId).stream()
                    .filter(c -> c.getType() == type && c.getProvider() == provider)
                    .findFirst();
            if (existing.isPresent()) {
                log.info("[ConnectionService] - [create] -> status=idempotent_retry connectionId={} userId={}",
                        existing.get().getId(), userId);
                return toResult(existing.get());
            }
            // Edge: chave registrada mas conexão não encontrada -> segue para criar.
        }

        final Instant now = Instant.now();

        final Connection conn = Connection.builder()
                .id(UUID.randomUUID())
                .userId(UserId.of(userId))
                .type(type)
                .provider(provider)
                .metadata(metadata)
                .createdAt(now)
                .build();

        final Connection saved = savePort.save(conn);

        log.info(
                "[ConnectionService] - [create] -> status=created connectionId={} userId={} type={} provider={} hasMetadata={}",
                saved.getId(),
                saved.getUserId().value(),
                saved.getType(),
                saved.getProvider(),
                saved.getMetadata() != null && !saved.getMetadata().isEmpty()
        );

        return toResult(saved);
    }

    // Lista as Connections de um usuário e converte os resultados de domínio para ConnectionResult.
    @Override
    public List<ConnectionResult> listByUserId(UUID userId) {
        Objects.requireNonNull(userId, "userId must not be null");

        final List<ConnectionResult> out = loadPort.findByUserId(userId).stream()
                .map(ConnectionService::toResult)
                .toList();

        log.debug(
                "[ConnectionService] - [listByUserId] -> userId={} size={}",
                userId, out.size()
        );

        return out;
    }

    // Valida campos obrigatórios do comando de criação de Connection e lança BusinessValidationException quando inválidos.
    private static void validate(CreateConnectionCommand c) {
        if (c.userId() == null) throw new BusinessValidationException("userId is required");
        if (c.type() == null || c.type().isBlank()) throw new BusinessValidationException("type is required");
        if (c.provider() == null || c.provider().isBlank()) throw new BusinessValidationException("provider is required");
        if (c.idempotencyKey() == null || c.idempotencyKey().isBlank())
            throw new BusinessValidationException("idempotencyKey is required");
    }

    // Converte o tipo de conexão (String) para ConnectionType, lançando BusinessValidationException quando inválido.
    private static ConnectionType parseConnectionType(String raw) {
        final String v = raw == null ? null : raw.trim();
        try {
            return ConnectionType.valueOf(v);
        } catch (Exception e) {
            throw new BusinessValidationException("Invalid connection type: " + raw);
        }
    }

    // Converte o provider (String) para AccountProvider, usando OTHER como fallback quando inválido.
    private static AccountProvider parseAccountProvider(String raw) {
        final String v = raw == null ? null : raw.trim();
        try {
            return AccountProvider.valueOf(v);
        } catch (Exception e) {
            return AccountProvider.OTHER;
        }
    }

    // Gera um hash determinístico do conteúdo relevante da requisição para suportar validações de idempotência.
    private static String requestHash(UUID userId, ConnectionType type, AccountProvider provider) {
        return sha256(userId + "|" + type.name() + "|" + provider.name());
    }

    // Converte uma Connection (domínio) para ConnectionResult (DTO de saída).
    private static ConnectionResult toResult(Connection c) {
        return new ConnectionResult(
                c.getId(),
                c.getUserId().value(),
                c.getType().name(),
                c.getProvider().name(),
                c.getMetadata(),
                c.getCreatedAt()
        );
    }

    // Calcula SHA-256 de uma string e retorna o hex; em falha, retorna um placeholder.
    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (Exception e) {
            return "sha256_error";
        }
    }

}
