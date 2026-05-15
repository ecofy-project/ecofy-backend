package br.com.ecofy.auth.core.port.in;

public interface ResetPasswordUseCase {

    void resetPassword(ResetPasswordCommand command);

    record ResetPasswordCommand(

            String resetToken,
            String newPassword

    ) {}

}
