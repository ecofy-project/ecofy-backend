package br.com.ecofy.ms_users.core.domain;

import br.com.ecofy.ms_users.core.domain.enums.PreferenceKey;
import br.com.ecofy.ms_users.core.domain.valueobject.UserId;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class UserPreference {

    private final UUID id;
    private final UserId userId;
    private final PreferenceKey key;
    private final String value;
    private final Instant updatedAt;

}