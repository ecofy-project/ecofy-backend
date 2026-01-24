package br.com.ecofy.ms_users.core.domain;

import br.com.ecofy.ms_users.core.domain.enums.AccountProvider;
import br.com.ecofy.ms_users.core.domain.valueobject.UserId;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class LinkedAccount {

    private final UUID id;
    private final UserId userId;
    private final AccountProvider provider;
    private final String externalAccountRef;
    private final boolean active;
    private final Instant linkedAt;

}