package br.com.ecofy.ms_users.core.port.in;

import br.com.ecofy.ms_users.core.application.command.CreateConnectionCommand;
import br.com.ecofy.ms_users.core.application.result.ConnectionResult;

public interface CreateConnectionUseCase {
    ConnectionResult create(CreateConnectionCommand command);
}