package br.com.ecofy.auth.core.port.in;

import br.com.ecofy.auth.core.domain.AuthUser;

public interface ConfirmEmailUseCase {

    AuthUser confirm(ConfirmEmailCommand command);

    record ConfirmEmailCommand(

            String verificationToken

    ) {}

}
