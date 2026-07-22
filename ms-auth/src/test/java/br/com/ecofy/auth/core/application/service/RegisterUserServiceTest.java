package br.com.ecofy.auth.core.application.service;

import br.com.ecofy.auth.core.application.exception.AuthErrorCode;
import br.com.ecofy.auth.core.application.exception.AuthException;
import br.com.ecofy.auth.core.domain.AuthUser;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários do serviço de registro de usuários")
class RegisterUserServiceTest {

    private static final String EMAIL = "matheus@ecofy.com";
    private static final String RAW_PASSWORD = "StrongPassword@123";
    private static final String FIRST_NAME = "Matheus";
    private static final String LAST_NAME = "Silva";

    @Mock
    private SaveAuthUserPort saveAuthUserPort;

    @Mock
    private LoadAuthUserByEmailPort loadAuthUserByEmailPort;

    @Mock
    private PasswordHashingPort passwordHashingPort;

    @Mock
    private SendVerificationEmailPort sendVerificationEmailPort;

    @Mock
    private VerificationTokenStorePort verificationTokenStorePort;

    @Mock
    private PublishAuthEventPort publishAuthEventPort;

    @Mock
    private SyncUserToUsersMsPort syncUserToUsersMsPort;

    @Mock
    private PasswordHash passwordHash;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AuthUser existingUser;

    @Test
    @DisplayName("Deve rejeitar dependências nulas recebidas pelo construtor")
    void constructor_dependenciasNulas_deveLancarNullPointerException() {
        // Arrange, Act e Assert
        assertAll(
                () -> assertNullDependency(
                        "saveAuthUserPort must not be null",
                        () -> new RegisterUserService(
                                null,
                                loadAuthUserByEmailPort,
                                passwordHashingPort,
                                sendVerificationEmailPort,
                                verificationTokenStorePort,
                                publishAuthEventPort,
                                syncUserToUsersMsPort
                        )
                ),
                () -> assertNullDependency(
                        "loadAuthUserByEmailPort must not be null",
                        () -> new RegisterUserService(
                                saveAuthUserPort,
                                null,
                                passwordHashingPort,
                                sendVerificationEmailPort,
                                verificationTokenStorePort,
                                publishAuthEventPort,
                                syncUserToUsersMsPort
                        )
                ),
                () -> assertNullDependency(
                        "passwordHashingPort must not be null",
                        () -> new RegisterUserService(
                                saveAuthUserPort,
                                loadAuthUserByEmailPort,
                                null,
                                sendVerificationEmailPort,
                                verificationTokenStorePort,
                                publishAuthEventPort,
                                syncUserToUsersMsPort
                        )
                ),
                () -> assertNullDependency(
                        "sendVerificationEmailPort must not be null",
                        () -> new RegisterUserService(
                                saveAuthUserPort,
                                loadAuthUserByEmailPort,
                                passwordHashingPort,
                                null,
                                verificationTokenStorePort,
                                publishAuthEventPort,
                                syncUserToUsersMsPort
                        )
                ),
                () -> assertNullDependency(
                        "verificationTokenStorePort must not be null",
                        () -> new RegisterUserService(
                                saveAuthUserPort,
                                loadAuthUserByEmailPort,
                                passwordHashingPort,
                                sendVerificationEmailPort,
                                null,
                                publishAuthEventPort,
                                syncUserToUsersMsPort
                        )
                ),
                () -> assertNullDependency(
                        "publishAuthEventPort must not be null",
                        () -> new RegisterUserService(
                                saveAuthUserPort,
                                loadAuthUserByEmailPort,
                                passwordHashingPort,
                                sendVerificationEmailPort,
                                verificationTokenStorePort,
                                null,
                                syncUserToUsersMsPort
                        )
                ),
                () -> assertNullDependency(
                        "syncUserToUsersMsPort must not be null",
                        () -> new RegisterUserService(
                                saveAuthUserPort,
                                loadAuthUserByEmailPort,
                                passwordHashingPort,
                                sendVerificationEmailPort,
                                verificationTokenStorePort,
                                publishAuthEventPort,
                                null
                        )
                )
        );

        verifyNoInteractions(
                saveAuthUserPort,
                loadAuthUserByEmailPort,
                passwordHashingPort,
                sendVerificationEmailPort,
                verificationTokenStorePort,
                publishAuthEventPort,
                syncUserToUsersMsPort
        );
    }

    @Test
    @DisplayName("Deve rejeitar o comando de registro nulo")
    void register_comandoNulo_deveLancarNullPointerException() {
        // Arrange
        RegisterUserService service = createService();

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> service.register(null)
        );

        // Assert
        assertEquals(
                "command must not be null",
                exception.getMessage()
        );

        verifyNoInteractions(
                saveAuthUserPort,
                loadAuthUserByEmailPort,
                passwordHashingPort,
                sendVerificationEmailPort,
                verificationTokenStorePort,
                publishAuthEventPort,
                syncUserToUsersMsPort
        );
    }

    @Test
    @DisplayName("Deve rejeitar o registro quando o e-mail já estiver cadastrado")
    void register_emailJaCadastrado_deveLancarAuthException() {
        // Arrange
        RegisterUserService service = createService();

        RegisterUserUseCase.RegisterUserCommand command =
                createCommand();

        stubBasicCommand(
                command,
                null,
                null
        );

        when(loadAuthUserByEmailPort.loadByEmail(
                any(EmailAddress.class)
        )).thenReturn(Optional.of(existingUser));

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.register(command)
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        AuthErrorCode.EMAIL_ALREADY_REGISTERED,
                        exception.getErrorCode()
                ),
                () -> assertEquals(
                        "Email already registered: " + EMAIL,
                        exception.getMessage()
                )
        );

        ArgumentCaptor<EmailAddress> emailCaptor =
                ArgumentCaptor.forClass(EmailAddress.class);

        verify(loadAuthUserByEmailPort)
                .loadByEmail(emailCaptor.capture());

        assertEquals(
                EMAIL,
                emailCaptor.getValue().value()
        );

        verifyNoInteractions(
                saveAuthUserPort,
                passwordHashingPort,
                sendVerificationEmailPort,
                verificationTokenStorePort,
                publishAuthEventPort,
                syncUserToUsersMsPort
        );
    }

    @Test
    @DisplayName("Deve registrar usuário pendente com configurações padrão e enviar a verificação de e-mail")
    void register_configuracoesPadrao_deveRegistrarUsuarioPendenteEEnviarVerificacao() {
        // Arrange
        RegisterUserService service = createService();

        RegisterUserUseCase.RegisterUserCommand command =
                createCommand();

        stubBasicCommand(
                command,
                null,
                null
        );

        when(command.rawPassword())
                .thenReturn(RAW_PASSWORD);

        when(command.autoConfirmEmail())
                .thenReturn(false);

        when(loadAuthUserByEmailPort.loadByEmail(
                any(EmailAddress.class)
        )).thenReturn(Optional.empty());

        when(passwordHashingPort.hash(RAW_PASSWORD))
                .thenReturn(passwordHash);

        when(saveAuthUserPort.save(any(AuthUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<EmailAddress> emailCaptor =
                ArgumentCaptor.forClass(EmailAddress.class);

        ArgumentCaptor<String> storedTokenCaptor =
                ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<String> emailedTokenCaptor =
                ArgumentCaptor.forClass(String.class);

        // Act
        AuthUser result = service.register(command);

        // Assert
        assertAll(
                () -> assertEquals(
                        EMAIL,
                        result.email().value()
                ),
                () -> assertFalse(result.isEmailVerified())
        );

        InOrder inOrder = inOrder(
                loadAuthUserByEmailPort,
                passwordHashingPort,
                saveAuthUserPort,
                syncUserToUsersMsPort,
                verificationTokenStorePort,
                sendVerificationEmailPort,
                publishAuthEventPort
        );

        inOrder.verify(loadAuthUserByEmailPort)
                .loadByEmail(emailCaptor.capture());

        inOrder.verify(passwordHashingPort)
                .hash(RAW_PASSWORD);

        inOrder.verify(saveAuthUserPort)
                .save(same(result));

        inOrder.verify(syncUserToUsersMsPort)
                .upsertUser(same(result));

        inOrder.verify(verificationTokenStorePort)
                .store(
                        same(result),
                        storedTokenCaptor.capture()
                );

        inOrder.verify(sendVerificationEmailPort)
                .send(
                        same(result),
                        emailedTokenCaptor.capture()
                );

        inOrder.verify(publishAuthEventPort)
                .publish(any(UserRegisteredEvent.class));

        String generatedToken = storedTokenCaptor.getValue();

        assertAll(
                () -> assertEquals(
                        EMAIL,
                        emailCaptor.getValue().value()
                ),
                () -> assertEquals(
                        generatedToken,
                        emailedTokenCaptor.getValue()
                ),
                () -> assertDoesNotThrow(
                        () -> UUID.fromString(generatedToken)
                )
        );
    }

    @Test
    @DisplayName("Deve auto-confirmar o e-mail, ignorar papéis inválidos e continuar quando a sincronização falhar")
    void register_autoConfirmacaoESincronizacaoFalha_devePersistirPublicarEventoEContinuar() {
        // Arrange
        RegisterUserService service = createService();

        RegisterUserUseCase.RegisterUserCommand command =
                createCommand();

        List<String> roles = Arrays.asList(
                " ROLE_ADMIN ",
                null,
                " ",
                "",
                "ROLE_ADMIN",
                "ROLE_USER"
        );

        stubBasicCommand(
                command,
                "en-US",
                roles
        );

        when(command.rawPassword())
                .thenReturn(RAW_PASSWORD);

        when(command.autoConfirmEmail())
                .thenReturn(true);

        when(loadAuthUserByEmailPort.loadByEmail(
                any(EmailAddress.class)
        )).thenReturn(Optional.empty());

        when(passwordHashingPort.hash(RAW_PASSWORD))
                .thenReturn(passwordHash);

        when(saveAuthUserPort.save(any(AuthUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IllegalStateException syncException =
                new IllegalStateException("ms-users unavailable");

        doThrow(syncException)
                .when(syncUserToUsersMsPort)
                .upsertUser(any(AuthUser.class));

        // Act
        AuthUser result = service.register(command);

        // Assert
        assertTrue(result.isEmailVerified());

        verify(syncUserToUsersMsPort)
                .upsertUser(same(result));

        verify(publishAuthEventPort)
                .publish(any(UserRegisteredEvent.class));

        verifyNoInteractions(
                verificationTokenStorePort,
                sendVerificationEmailPort
        );
    }

    @Test
    @DisplayName("Deve aplicar o papel padrão quando a lista estiver vazia e propagar falha de persistência")
    void register_listaDePapeisVaziaEPersistenciaFalha_devePropagarExcecao() {
        // Arrange
        RegisterUserService service = createService();

        RegisterUserUseCase.RegisterUserCommand command =
                createCommand();

        stubBasicCommand(
                command,
                "pt-BR",
                List.of()
        );

        when(command.rawPassword())
                .thenReturn(RAW_PASSWORD);

        when(command.autoConfirmEmail())
                .thenReturn(false);

        when(loadAuthUserByEmailPort.loadByEmail(
                any(EmailAddress.class)
        )).thenReturn(Optional.empty());

        when(passwordHashingPort.hash(RAW_PASSWORD))
                .thenReturn(passwordHash);

        IllegalStateException expectedException =
                new IllegalStateException("Persistence unavailable");

        when(saveAuthUserPort.save(any(AuthUser.class)))
                .thenThrow(expectedException);

        // Act
        IllegalStateException actualException = assertThrows(
                IllegalStateException.class,
                () -> service.register(command)
        );

        // Assert
        assertSame(
                expectedException,
                actualException
        );

        verify(passwordHashingPort)
                .hash(RAW_PASSWORD);

        verify(saveAuthUserPort)
                .save(any(AuthUser.class));

        verifyNoInteractions(
                syncUserToUsersMsPort,
                verificationTokenStorePort,
                sendVerificationEmailPort,
                publishAuthEventPort
        );
    }

    @Test
    @DisplayName("Deve propagar falha na geração do hash sem persistir o usuário")
    void register_geracaoDoHashFalha_devePropagarExcecaoSemPersistirUsuario() {
        // Arrange
        RegisterUserService service = createService();

        RegisterUserUseCase.RegisterUserCommand command =
                createCommand();

        stubBasicCommand(
                command,
                "pt-BR",
                List.of("ROLE_USER")
        );

        when(command.rawPassword())
                .thenReturn(RAW_PASSWORD);

        when(loadAuthUserByEmailPort.loadByEmail(
                any(EmailAddress.class)
        )).thenReturn(Optional.empty());

        IllegalStateException expectedException =
                new IllegalStateException("Hashing unavailable");

        when(passwordHashingPort.hash(RAW_PASSWORD))
                .thenThrow(expectedException);

        // Act
        IllegalStateException actualException = assertThrows(
                IllegalStateException.class,
                () -> service.register(command)
        );

        // Assert
        assertSame(
                expectedException,
                actualException
        );

        verify(saveAuthUserPort, never())
                .save(any(AuthUser.class));

        verifyNoInteractions(
                syncUserToUsersMsPort,
                verificationTokenStorePort,
                sendVerificationEmailPort,
                publishAuthEventPort
        );
    }

    @Test
    @DisplayName("Deve mascarar tokens nulos, em branco, curtos e longos conforme o limite definido")
    void maskToken_diferentesFormatos_deveRetornarTokenMascarado() throws Exception {
        // Arrange
        RegisterUserService service = createService();

        Method method = RegisterUserService.class.getDeclaredMethod(
                "maskToken",
                String.class
        );

        method.setAccessible(true);

        // Act
        String nullTokenResult =
                (String) method.invoke(service, (Object) null);

        String blankTokenResult =
                (String) method.invoke(service, "   ");

        String shortTokenResult =
                (String) method.invoke(service, "1234567890");

        String longTokenResult =
                (String) method.invoke(service, "12345678901");

        // Assert
        assertAll(
                () -> assertEquals(
                        "***",
                        nullTokenResult
                ),
                () -> assertEquals(
                        "***",
                        blankTokenResult
                ),
                () -> assertEquals(
                        "***",
                        shortTokenResult
                ),
                () -> assertEquals(
                        "1234567890...",
                        longTokenResult
                )
        );
    }

    private RegisterUserService createService() {
        return new RegisterUserService(
                saveAuthUserPort,
                loadAuthUserByEmailPort,
                passwordHashingPort,
                sendVerificationEmailPort,
                verificationTokenStorePort,
                publishAuthEventPort,
                syncUserToUsersMsPort
        );
    }

    private RegisterUserUseCase.RegisterUserCommand createCommand() {
        return mock(
                RegisterUserUseCase.RegisterUserCommand.class
        );
    }

    private void stubBasicCommand(
            RegisterUserUseCase.RegisterUserCommand command,
            String locale,
            List<String> roles
    ) {
        when(command.email())
                .thenReturn(EMAIL);

        when(command.firstName())
                .thenReturn(FIRST_NAME);

        when(command.lastName())
                .thenReturn(LAST_NAME);

        when(command.locale())
                .thenReturn(locale);

        when(command.roles())
                .thenReturn(roles);
    }

    private void assertNullDependency(
            String expectedMessage,
            Executable executable
    ) {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                executable
        );

        assertEquals(
                expectedMessage,
                exception.getMessage()
        );
    }
}
