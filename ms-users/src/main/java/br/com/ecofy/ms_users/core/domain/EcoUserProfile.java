package br.com.ecofy.ms_users.core.domain;

import br.com.ecofy.ms_users.core.domain.enums.UserStatus;
import br.com.ecofy.ms_users.core.domain.valueobject.EmailAddress;
import br.com.ecofy.ms_users.core.domain.valueobject.ExternalAuthId;
import br.com.ecofy.ms_users.core.domain.valueobject.PhoneNumber;
import br.com.ecofy.ms_users.core.domain.valueobject.UserId;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder(toBuilder = true)
public class EcoUserProfile {

    private final UserId id;
    private final ExternalAuthId externalAuthId;
    private final String fullName;
    private final EmailAddress email;
    private final PhoneNumber phone;
    private final UserStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

}
