package br.com.ecofy.ms_users.core.port.in;

import br.com.ecofy.ms_users.core.application.command.UpdatePreferencesCommand;
import br.com.ecofy.ms_users.core.application.result.UserPreferencesResult;

public interface UpdateUserPreferencesUseCase {
    UserPreferencesResult update(UpdatePreferencesCommand command);
}