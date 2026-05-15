package br.com.ecofy.auth.core.application.service;

import br.com.ecofy.auth.core.application.exception.AuthErrorCode;
import br.com.ecofy.auth.core.application.exception.AuthException;
import br.com.ecofy.auth.core.domain.AuthUser;
import br.com.ecofy.auth.core.domain.Role;
import br.com.ecofy.auth.core.domain.event.UserRegisteredEvent;
import br.com.ecofy.auth.core.domain.valueobject.EmailAddress;
import br.com.ecofy.auth.core.domain.valueobject.PasswordHash;
import br.com.ecofy.auth.core.port.in.RegisterUserUseCase;
import br.com.ecofy.auth.core.port.out.LoadAuthUserByEmailPort;
import br.com.ecofy.auth.core.port.out.PasswordHashingPort;
import br.com.ecofy.auth.core.port.out.PublishAuthEventPort;
import br.com.ecofy.auth.core.port.out.SaveAuthUserPort;
import br.com.ecofy.auth.core.port.out.SendVerificationEmailPort;
import br.com.ecofy.auth.core.port.out.SyncUserToUsersMsPort;
import br.com.ecofy.auth.core.port.out.VerificationTokenStorePort;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// Serviço responsável por registrar novos usuários, validar duplicidade de e-mail, persistir o usuário e disparar verificação/publicação de eventos.
@Slf4j
@Service
public class RegisterUserService implements RegisterUserUseCase {

    private final SaveAuthUserPort saveAuthUserPort;
    private final LoadAuthUserByEmailPort loadAuthUserByEmailPort;
    private final PasswordHashingPort passwordHashingPort;
    private final SendVerificationEmailPort sendVerificationEmailPort;
    private final VerificationTokenStorePort verificationTokenStorePort;
    private final PublishAuthEventPort publishAuthEventPort;

    // Port responsável por sincronizar (upsert) o usuário no ms-users após o registro no ms-auth.
    private final SyncUserToUsersMsPort syncUserToUsersMsPort;

    // Inicializa o serviço com as portas necessárias para persistência, consulta, hashing, envio/armazenamento de token, publicação de eventos e sincronização com ms-users.
    public RegisterUserService(
            SaveAuthUserPort saveAuthUserPort,
            LoadAuthUserByEmailPort loadAuthUserByEmailPort,
            PasswordHashingPort passwordHashingPort,
            SendVerificationEmailPort sendVerificationEmailPort,
            VerificationTokenStorePort verificationTokenStorePort,
            PublishAuthEventPort publishAuthEventPort,
            SyncUserToUsersMsPort syncUserToUsersMsPort
    ) {
        this.saveAuthUserPort = Objects.requireNonNull(saveAuthUserPort, "saveAuthUserPort must not be null");
        this.loadAuthUserByEmailPort =
                Objects.requireNonNull(loadAuthUserByEmailPort, "loadAuthUserByEmailPort must not be null");
        this.passwordHashingPort =
                Objects.requireNonNull(passwordHashingPort, "passwordHashingPort must not be null");
        this.sendVerificationEmailPort =
                Objects.requireNonNull(sendVerificationEmailPort, "sendVerificationEmailPort must not be null");
        this.verificationTokenStorePort =
                Objects.requireNonNull(verificationTokenStorePort, "verificationTokenStorePort must not be null");
        this.publishAuthEventPort =
                Objects.requireNonNull(publishAuthEventPort, "publishAuthEventPort must not be null");
        this.syncUserToUsersMsPort =
                Objects.requireNonNull(syncUserToUsersMsPort, "syncUserToUsersMsPort must not be null");
    }

    // Registra um novo usuário (com roles/defaults), impede e-mail duplicado, persiste, opcionalmente auto-confirma e dispara e-mail/token e evento.
    @Override
    public AuthUser register(RegisterUserCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        EmailAddress email = new EmailAddress(command.email());
        String locale = command.locale() != null ? command.locale() : "pt-BR";

        List<String> roleNames = (command.roles() == null || command.roles().isEmpty())
                ? List.of("AUTH_USER")
                : command.roles();

        Set<Role> roles = roleNames.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(name -> new Role(name, null, Set.of()))
                .collect(Collectors.toSet());

        log.debug(
                "[RegisterUserService] - [register] -> Iniciando registro de usuário email={} firstName={} lastName={} locale={} roles={}",
                email.value(),
                command.firstName(),
                command.lastName(),
                locale,
                roleNames
        );

        loadAuthUserByEmailPort.loadByEmail(email).ifPresent(existing -> {
            log.warn(
                    "[RegisterUserService] - [register] -> Email já registrado email={} userId={}",
                    email.value(),
                    existing.id().value()
            );
            throw new AuthException(
                    AuthErrorCode.EMAIL_ALREADY_REGISTERED,
                    "Email already registered: " + email.value()
            );
        });

        PasswordHash passwordHash = passwordHashingPort.hash(command.rawPassword());

        AuthUser newUser = AuthUser.newPendingUser(
                email,
                passwordHash,
                command.firstName(),
                command.lastName(),
                locale,
                roles
        );

        if (command.autoConfirmEmail()) {
            log.debug(
                    "[RegisterUserService] - [register] -> Auto-confirmando email={} userStatusBefore={}",
                    email.value(),
                    newUser.status()
            );
            newUser.confirmEmail();
        }

        AuthUser persisted = saveAuthUserPort.save(newUser);

        log.debug(
                "[RegisterUserService] - [register] -> Usuário persistido userId={} emailVerified={} status={}",
                persisted.id().value(),
                persisted.isEmailVerified(),
                persisted.status()
        );

        // Sincroniza o usuário no ms-users logo após persistir no ms-auth (upsert).
        // Observação: best-effort para não impedir registro em caso de indisponibilidade temporária do ms-users.
        try {
            syncUserToUsersMsPort.upsertUser(persisted);

            log.debug(
                    "[RegisterUserService] - [register] -> Sync com ms-users realizado userId={} externalAuthId={}",
                    persisted.id().value(),
                    persisted.id().value()
            );
        } catch (Exception ex) {
            log.warn(
                    "[RegisterUserService] - [register] -> Falha ao sincronizar com ms-users userId={} cause={}",
                    persisted.id().value(),
                    ex.getMessage()
            );
        }

        if (!command.autoConfirmEmail()) {
            String token = UUID.randomUUID().toString();
            verificationTokenStorePort.store(persisted, token);

            log.debug(
                    "[RegisterUserService] - [register] -> Token de verificação criado userId={} tokenMask={}",
                    persisted.id().value(),
                    maskToken(token)
            );

            sendVerificationEmailPort.send(persisted, token);

            log.debug(
                    "[RegisterUserService] - [register] -> Email de verificação enviado userId={} email={}",
                    persisted.id().value(),
                    persisted.email().value()
            );
        }

        publishAuthEventPort.publish(new UserRegisteredEvent(persisted));

        log.debug(
                "[RegisterUserService] - [register] -> Evento UserRegisteredEvent publicado userId={}",
                persisted.id().value()
        );

        return persisted;
    }

    // Mascara o token para logging, evitando expor o valor completo em logs.
    private String maskToken(String token) {
        if (token == null || token.isBlank()) {
            return "***";
        }
        return token.length() > 10
                ? token.substring(0, 10) + "..."
                : "***";
    }
}