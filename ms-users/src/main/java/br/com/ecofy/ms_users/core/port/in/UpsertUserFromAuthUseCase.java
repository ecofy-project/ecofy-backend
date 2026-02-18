package br.com.ecofy.ms_users.core.port.in;

import br.com.ecofy.ms_users.core.application.result.UserProfileResult;

public interface UpsertUserFromAuthUseCase {

    record Command(
            String authUserId,
            String email,
            String firstName,
            String lastName,
            String fullName,
            Boolean emailVerified,
            String status,
            String locale
    ) {}

    UserProfileResult upsert(Command command);
}
