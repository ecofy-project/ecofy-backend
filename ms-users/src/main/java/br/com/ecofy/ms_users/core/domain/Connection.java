package br.com.ecofy.ms_users.core.domain;

import br.com.ecofy.ms_users.core.domain.enums.AccountProvider;
import br.com.ecofy.ms_users.core.domain.enums.ConnectionType;
import br.com.ecofy.ms_users.core.domain.valueobject.UserId;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class Connection {
    private final UUID id;
    private final UserId userId;
    private final ConnectionType type;
    private final AccountProvider provider;
    private final Map<String, Object> metadata;
    private final Instant createdAt;
}