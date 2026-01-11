package br.com.ecofy.ms_users.core.application.service;

import br.com.ecofy.ms_users.config.UsersProperties;
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
import br.com.ecofy.ms_users.core.port.out.IdempotencyPort;
import br.com.ecofy.ms_users.core.port.out.LoadConnectionsPort;
import br.com.ecofy.ms_users.core.port.out.SaveConnectionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConnectionService implements CreateConnectionUseCase, ListConnectionsUseCase {

    private final SaveConnectionPort savePort;
    private final LoadConnectionsPort loadPort;
    private final IdempotencyPort idempotencyPort;
    private final UsersProperties props;

    @Override
    public ConnectionResult create(CreateConnectionCommand command) {
        validate(command);

        String op = "users.createConnection";
        String reqHash = sha256(command.userId() + "|" + command.type() + "|" + command.provider());

        boolean first = idempotencyPort.registerOnce(op, command.idempotencyKey(), reqHash, props.idempotency().ttl());
        if (!first) throw new IdempotencyViolationException("Idempotency key already used for operation=" + op);

        ConnectionType type;
        try { type = ConnectionType.valueOf(command.type()); }
        catch (Exception e) { throw new BusinessValidationException("Invalid connection type: " + command.type()); }

        AccountProvider provider;
        try { provider = AccountProvider.valueOf(command.provider()); }
        catch (Exception e) { provider = AccountProvider.OTHER; }

        Map<String, Object> md = command.metadata() != null ? command.metadata() : Map.of();

        Connection conn = Connection.builder()
                .id(UUID.randomUUID())
                .userId(UserId.of(command.userId()))
                .type(type)
                .provider(provider)
                .metadata(md)
                .createdAt(Instant.now())
                .build();

        var saved = savePort.save(conn);
        return toResult(saved);
    }

    @Override
    public List<ConnectionResult> listByUserId(UUID userId) {
        return loadPort.findByUserId(userId).stream().map(ConnectionService::toResult).toList();
    }

    private static void validate(CreateConnectionCommand c) {
        if (c.userId() == null) throw new BusinessValidationException("userId is required");
        if (c.type() == null || c.type().isBlank()) throw new BusinessValidationException("type is required");
        if (c.provider() == null || c.provider().isBlank()) throw new BusinessValidationException("provider is required");
        if (c.idempotencyKey() == null || c.idempotencyKey().isBlank()) throw new BusinessValidationException("idempotencyKey is required");
    }

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

    private static String sha256(String s) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (Exception e) {
            return "sha256_error";
        }
    }
}