package br.com.ecofy.auth.core.application.service;

import br.com.ecofy.auth.core.application.exception.AuthErrorCode;
import br.com.ecofy.auth.core.application.exception.AuthException;
import br.com.ecofy.auth.core.domain.AuthUser;
import br.com.ecofy.auth.core.domain.event.PasswordResetRequestedEvent;
import br.com.ecofy.auth.core.domain.valueobject.EmailAddress;
import br.com.ecofy.auth.core.domain.valueobject.PasswordHash;
import br.com.ecofy.auth.core.port.in.RequestPasswordResetUseCase;
import br.com.ecofy.auth.core.port.in.ResetPasswordUseCase;
import br.com.ecofy.auth.core.port.out.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

// Serviço responsável pelo fluxo de recuperação de senha: solicitar reset (gerar token e enviar e-mail) e efetivar reset (validar token e atualizar senha).
@Slf4j
@Service
public class PasswordResetService implements RequestPasswordResetUseCase, ResetPasswordUseCase {

    private final LoadAuthUserByEmailPort loadAuthUserByEmailPort;
    private final PasswordResetTokenStorePort passwordResetTokenStorePort;
    private final SendResetPasswordEmailPort sendResetPasswordEmailPort;
    private final SaveAuthUserPort saveAuthUserPort;
    private final PasswordHashingPort passwordHashingPort;
    private final PublishAuthEventPort publishAuthEventPort;

    // Inicializa o serviço com as portas necessárias para buscar usuário, armazenar token, enviar e-mail, salvar usuário e publicar eventos.
    public PasswordResetService(LoadAuthUserByEmailPort loadAuthUserByEmailPort,
                                PasswordResetTokenStorePort passwordResetTokenStorePort,
                                SendResetPasswordEmailPort sendResetPasswordEmailPort,
                                SaveAuthUserPort saveAuthUserPort,
                                PasswordHashingPort passwordHashingPort,
                                PublishAuthEventPort publishAuthEventPort) {

        this.loadAuthUserByEmailPort =
                Objects.requireNonNull(loadAuthUserByEmailPort, "loadAuthUserByEmailPort must not be null");
        this.passwordResetTokenStorePort =
                Objects.requireNonNull(passwordResetTokenStorePort, "passwordResetTokenStorePort must not be null");
        this.sendResetPasswordEmailPort =
                Objects.requireNonNull(sendResetPasswordEmailPort, "sendResetPasswordEmailPort must not be null");
        this.saveAuthUserPort =
                Objects.requireNonNull(saveAuthUserPort, "saveAuthUserPort must not be null");
        this.passwordHashingPort =
                Objects.requireNonNull(passwordHashingPort, "passwordHashingPort must not be null");
        this.publishAuthEventPort =
                Objects.requireNonNull(publishAuthEventPort, "publishAuthEventPort must not be null");
    }

    // Gera e armazena um token de reset para o e-mail informado, envia o link por e-mail e publica o evento de solicitação de reset.
    @Override
    public void requestReset(RequestPasswordResetCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        EmailAddress email = new EmailAddress(command.email());

        log.debug(
                "[PasswordResetService] - [requestReset] -> Solicitando reset para email={}",
                email.value()
        );

        AuthUser user = loadAuthUserByEmailPort
                .loadByEmail(email)
                .orElseThrow(() -> {
                    log.warn(
                            "[PasswordResetService] - [requestReset] -> Usuário não encontrado email={}",
                            email.value()
                    );
                    return new AuthException(
                            AuthErrorCode.USER_NOT_FOUND,
                            "User not found"
                    );
                });

        String resetToken = UUID.randomUUID().toString();
        String masked = maskToken(resetToken);

        log.debug(
                "[PasswordResetService] - [requestReset] -> Token gerado userId={} token={}",
                user.id().value(), masked
        );

        passwordResetTokenStorePort.store(user, resetToken);
        sendResetPasswordEmailPort.sendReset(user, resetToken);

        log.debug(
                "[PasswordResetService] - [requestReset] -> E-mail de reset enviado userId={} email={}",
                user.id().value(), user.email().value()
        );

        publishAuthEventPort.publish(new PasswordResetRequestedEvent(user, resetToken));

        log.debug(
                "[PasswordResetService] - [requestReset] -> Evento PasswordResetRequestedEvent publicado userId={}",
                user.id().value()
        );
    }

    // Consome o token de reset, gera o hash da nova senha, atualiza o usuário e persiste a alteração.
    @Override
    public void resetPassword(ResetPasswordCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        String maskedToken = maskToken(command.resetToken());

        log.debug(
                "[PasswordResetService] - [resetPassword] -> Validando token para redefinição token={}",
                maskedToken
        );

        AuthUser user = passwordResetTokenStorePort
                .consume(command.resetToken())
                .orElseThrow(() -> {
                    log.warn(
                            "[PasswordResetService] - [resetPassword] -> Token inválido ou expirado token={}",
                            maskedToken
                    );
                    return new AuthException(
                            AuthErrorCode.PASSWORD_RESET_TOKEN_INVALID,
                            "Invalid or expired reset token"
                    );
                });

        log.debug(
                "[PasswordResetService] - [resetPassword] -> Token válido userId={}",
                user.id().value()
        );

        PasswordHash newHash = passwordHashingPort.hash(command.newPassword());

        user.changePassword(newHash);
        saveAuthUserPort.save(user);

        log.debug(
                "[PasswordResetService] - [resetPassword] -> Senha redefinida com sucesso userId={}",
                user.id().value()
        );
    }

    // Mascara tokens para logging, evitando expor o valor completo em logs.
    private String maskToken(String token) {
        if (token == null || token.isBlank()) return "***";
        return token.length() > 10 ? token.substring(0, 10) + "..." : "***";
    }
}
