package br.com.ecofy.ms_users.core.port.in;

import br.com.ecofy.ms_users.core.application.result.UserPreferencesResult;

import java.util.UUID;

public interface GetUserPreferencesUseCase {
    UserPreferencesResult getByUserId(UUID userId);
}