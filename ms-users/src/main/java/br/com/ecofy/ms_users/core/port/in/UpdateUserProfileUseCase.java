package br.com.ecofy.ms_users.core.port.in;

import br.com.ecofy.ms_users.core.application.command.UpdateUserProfileCommand;
import br.com.ecofy.ms_users.core.application.result.UserProfileResult;

public interface UpdateUserProfileUseCase {
    UserProfileResult update(UpdateUserProfileCommand command);
}