package br.com.ecofy.auth.core.application.service;

import br.com.ecofy.auth.core.application.exception.AuthErrorCode;
import br.com.ecofy.auth.core.application.exception.AuthException;
import br.com.ecofy.auth.core.domain.AuthUser;
import br.com.ecofy.auth.core.domain.event.UserEmailConfirmedEvent;
import br.com.ecofy.auth.core.port.in.ConfirmEmailUseCase;
import br.com.ecofy.auth.core.port.out.PublishAuthEventPort;
import br.com.ecofy.auth.core.port.out.SaveAuthUserPort;
import br.com.ecofy.auth.core.port.out.VerificationTokenStorePort;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// Serviço responsável por confirmar o e-mail do usuário a partir de um token de verificação válido.
@Slf4j
@Service
public class EmailConfirmationService implements ConfirmEmailUseCase {

    private final VerificationTokenStorePort verificationTokenStorePort;
    private final SaveAuthUserPort saveAuthUserPort;
    private final PublishAuthEventPort publishAuthEventPort;

    // Inicializa o serviço com as portas de token, persistência de usuário e publicação de eventos.
    public EmailConfirmationService(
            VerificationTokenStorePort verificationTokenStorePort,
            SaveAuthUserPort saveAuthUserPort,
            PublishAuthEventPort publishAuthEventPort
    ) {
        this.verificationTokenStorePort =
                Objects.requireNonNull(verificationTokenStorePort, "verificationTokenStorePort must not be null");
        this.saveAuthUserPort =
                Objects.requireNonNull(saveAuthUserPort, "saveAuthUserPort must not be null");
        this.publishAuthEventPort =
                Objects.requireNonNull(publishAuthEventPort, "publishAuthEventPort must not be null");
    }

    // Consome o token de verificação, marca o e-mail como confirmado, persiste o usuário e publica o evento de confirmação.
    @Override
    public AuthUser confirm(ConfirmEmailCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        String token = Objects.requireNonNull(command.verificationToken(), "verificationToken must not be null");
        String maskedToken = maskToken(token);

        log.debug(
                "[EmailConfirmationService] - [confirm] -> Iniciando confirmação de e-mail token={}",
                maskedToken
        );

        AuthUser user = verificationTokenStorePort
                .consume(token)
                .orElseThrow(() -> {
                    log.warn(
                            "[EmailConfirmationService] - [confirm] -> Token inválido ou expirado token={}",
                            maskedToken
                    );
                    return new AuthException(
                            AuthErrorCode.EMAIL_CONFIRMATION_TOKEN_INVALID,
                            "Invalid or expired verification token"
                    );
                });

        log.debug(
                "[EmailConfirmationService] - [confirm] -> Token válido. Confirmando e-mail userId={} emailVerifiedBefore={}",
                user.id().value(),
                user.isEmailVerified()
        );

        user.confirmEmail();
        AuthUser persisted = saveAuthUserPort.save(user);

        log.debug(
                "[EmailConfirmationService] - [confirm] -> E-mail confirmado e usuário persistido userId={} emailVerifiedAfter={}",
                persisted.id().value(),
                persisted.isEmailVerified()
        );

        publishAuthEventPort.publish(new UserEmailConfirmedEvent(persisted));

        log.debug(
                "[EmailConfirmationService] - [confirm] -> Evento UserEmailConfirmedEvent publicado userId={}",
                persisted.id().value()
        );

        return persisted;
    }

    // Mascara o token para logging, evitando expor o valor completo em logs.
    private String maskToken(String token) {
        if (token == null || token.isBlank()) {
            return "***";
        }
        return token.length() > 10 ? token.substring(0, 10) + "..." : "***";
    }
}