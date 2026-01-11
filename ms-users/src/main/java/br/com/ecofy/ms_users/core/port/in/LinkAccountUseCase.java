package br.com.ecofy.ms_users.core.port.in;

import br.com.ecofy.ms_users.core.application.command.LinkAccountCommand;
import br.com.ecofy.ms_users.core.application.result.UserProfileResult;

public interface LinkAccountUseCase {
    UserProfileResult linkAccount(LinkAccountCommand command);
}