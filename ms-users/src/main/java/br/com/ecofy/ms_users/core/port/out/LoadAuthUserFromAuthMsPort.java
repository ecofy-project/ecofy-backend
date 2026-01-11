package br.com.ecofy.ms_users.core.port.out;

import java.util.UUID;

public interface LoadAuthUserFromAuthMsPort {
    AuthUserSnapshot loadByUserId(UUID userId);

    record AuthUserSnapshot(
            UUID userId,
            String externalAuthId,
            String fullName,
            String email,
            String phone
    ) { }
}
