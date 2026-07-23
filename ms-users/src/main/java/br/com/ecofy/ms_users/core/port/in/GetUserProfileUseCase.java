package br.com.ecofy.ms_users.core.port.in;

import br.com.ecofy.ms_users.core.application.result.UserProfileResult;

import java.util.UUID;

public interface GetUserProfileUseCase {

    UserProfileResult getById(UUID userId);

    UserProfileResult getByExternalAuthId(String externalAuthId);
}