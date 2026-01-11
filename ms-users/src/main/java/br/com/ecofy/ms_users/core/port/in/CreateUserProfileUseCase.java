package br.com.ecofy.ms_users.core.port.in;

import br.com.ecofy.ms_users.core.application.command.CreateUserProfileCommand;
import br.com.ecofy.ms_users.core.application.result.UserProfileResult;

public interface CreateUserProfileUseCase {
    UserProfileResult create(CreateUserProfileCommand command);
}
