package br.com.ecofy.auth.core.port.in;

public interface RequestPasswordResetUseCase {

    void requestReset(RequestPasswordResetCommand command);

    record RequestPasswordResetCommand(

            String email

    ) {}

}
