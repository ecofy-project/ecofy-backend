package br.com.ecofy.auth.core.application.service;

import br.com.ecofy.auth.core.application.exception.AuthErrorCode;
import br.com.ecofy.auth.core.application.exception.AuthException;
import br.com.ecofy.auth.core.domain.ClientApplication;
import br.com.ecofy.auth.core.domain.enums.ClientType;
import br.com.ecofy.auth.core.domain.enums.GrantType;
import br.com.ecofy.auth.core.domain.valueobject.PasswordHash;
import br.com.ecofy.auth.core.port.in.RegisterClientApplicationUseCase.RegisterClientCommand;
import br.com.ecofy.auth.core.port.out.PasswordHashingPort;
import br.com.ecofy.auth.core.port.out.SaveClientApplicationPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários do serviço de aplicações cliente")
class ClientApplicationServiceTest {

    private static final Pattern CLIENT_ID_PATTERN =
            Pattern.compile("^eco_[A-Za-z0-9_-]{16}$");

    private static final Pattern CLIENT_SECRET_PATTERN =
            Pattern.compile("^[A-Za-z0-9_-]{43}$");

    private static final Set<String> REDIRECT_URIS =
            Set.of("https://ecofy.app/callback");

    private static final Set<String> SCOPES =
            Set.of("openid", "profile");

    @Mock
    private SaveClientApplicationPort saveClientApplicationPort;

    @Mock
    private PasswordHashingPort passwordHashingPort;

    @Mock
    private ClientApplication savedClientApplication;

    @Test
    @DisplayName("Deve criar o serviço quando todas as dependências forem informadas")
    void constructor_dependenciasValidas_deveCriarServico() {
        // Act
        ClientApplicationService service = new ClientApplicationService(
                saveClientApplicationPort,
                passwordHashingPort
        );

        // Assert
        assertNotNull(service);
    }

    @Test
    @DisplayName("Deve lançar NullPointerException quando a porta de persistência for nula")
    void constructor_portaDePersistenciaNula_deveLancarNullPointerException() {
        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new ClientApplicationService(
                        null,
                        passwordHashingPort
                )
        );

        // Assert
        assertEquals(
                "saveClientApplicationPort must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve lançar NullPointerException quando a porta de hash de senha for nula")
    void constructor_portaDeHashNula_deveLancarNullPointerException() {
        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new ClientApplicationService(
                        saveClientApplicationPort,
                        null
                )
        );

        // Assert
        assertEquals(
                "passwordHashingPort must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve lançar NullPointerException quando o comando de registro for nulo")
    void register_comandoNulo_deveLancarNullPointerException() {
        // Arrange
        ClientApplicationService service = createService();

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> service.register(null)
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "command must not be null",
                        exception.getMessage()
                ),
                () -> verify(
                        saveClientApplicationPort,
                        never()
                ).save(any()),
                () -> verify(
                        passwordHashingPort,
                        never()
                ).hash(any())
        );
    }

    @Test
    @DisplayName("Deve registrar cliente confidencial com grants padrão e segredo protegido")
    void register_clienteConfidencialSemGrants_deveRegistrarComPadroesESegredo() {
        // Arrange
        ClientApplicationService service = createService();
        RegisterClientCommand command = command(
                "EcoFy Web",
                ClientType.CONFIDENTIAL,
                null,
                REDIRECT_URIS
        );
        PasswordHash passwordHash = new PasswordHash("secret-hash");

        when(passwordHashingPort.hash(any()))
                .thenReturn(passwordHash);
        when(saveClientApplicationPort.save(any()))
                .thenReturn(savedClientApplication);
        when(savedClientApplication.clientId())
                .thenReturn("saved-client-id");
        when(savedClientApplication.clientType())
                .thenReturn(ClientType.CONFIDENTIAL);
        when(savedClientApplication.isActive())
                .thenReturn(true);

        // Act
        ClientApplication result = service.register(command);

        // Assert
        ArgumentCaptor<String> secretCaptor =
                ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ClientApplication> clientCaptor =
                ArgumentCaptor.forClass(ClientApplication.class);

        assertAll(
                () -> assertSame(savedClientApplication, result),
                () -> verify(passwordHashingPort).hash(
                        secretCaptor.capture()
                ),
                () -> verify(saveClientApplicationPort).save(
                        clientCaptor.capture()
                )
        );

        ClientApplication createdClient = clientCaptor.getValue();

        assertAll(
                () -> assertTrue(
                        CLIENT_SECRET_PATTERN.matcher(
                                secretCaptor.getValue()
                        ).matches()
                ),
                () -> assertEquals(
                        "EcoFy Web",
                        createdClient.name()
                ),
                () -> assertEquals(
                        ClientType.CONFIDENTIAL,
                        createdClient.clientType()
                ),
                () -> assertEquals(
                        Set.of(
                                GrantType.AUTHORIZATION_CODE,
                                GrantType.REFRESH_TOKEN,
                                GrantType.CLIENT_CREDENTIALS
                        ),
                        createdClient.grantTypes()
                ),
                () -> assertEquals(
                        REDIRECT_URIS,
                        createdClient.redirectUris()
                ),
                () -> assertEquals(
                        SCOPES,
                        createdClient.scopes()
                ),
                () -> assertTrue(createdClient.isFirstParty()),
                () -> assertTrue(
                        CLIENT_ID_PATTERN.matcher(
                                createdClient.clientId()
                        ).matches()
                ),
                () -> assertEquals(
                        "secret-hash",
                        createdClient.clientSecretHash()
                )
        );
    }

    @Test
    @DisplayName("Deve registrar cliente público com grants padrão e sem gerar segredo")
    void register_clientePublicoComGrantsVazios_deveRegistrarSemSegredo() {
        // Arrange
        ClientApplicationService service = createService();
        RegisterClientCommand command = command(
                "EcoFy Mobile",
                ClientType.PUBLIC,
                Set.of(),
                REDIRECT_URIS
        );

        when(saveClientApplicationPort.save(any()))
                .thenReturn(savedClientApplication);
        when(savedClientApplication.clientId())
                .thenReturn("saved-client-id");
        when(savedClientApplication.clientType())
                .thenReturn(ClientType.PUBLIC);
        when(savedClientApplication.isActive())
                .thenReturn(true);

        // Act
        ClientApplication result = service.register(command);

        // Assert
        ArgumentCaptor<ClientApplication> clientCaptor =
                ArgumentCaptor.forClass(ClientApplication.class);

        assertAll(
                () -> assertSame(savedClientApplication, result),
                () -> verify(
                        passwordHashingPort,
                        never()
                ).hash(any()),
                () -> verify(saveClientApplicationPort).save(
                        clientCaptor.capture()
                )
        );

        ClientApplication createdClient = clientCaptor.getValue();

        assertAll(
                () -> assertEquals(
                        Set.of(
                                GrantType.AUTHORIZATION_CODE,
                                GrantType.REFRESH_TOKEN
                        ),
                        createdClient.grantTypes()
                ),
                () -> assertNull(
                        createdClient.clientSecretHash()
                )
        );
    }

    @Test
    @DisplayName("Deve registrar cliente SPA com grants padrão e sem gerar segredo")
    void register_clienteSpaSemGrants_deveRegistrarComPadroesESemSegredo() {
        // Arrange
        ClientApplicationService service = createService();
        RegisterClientCommand command = command(
                "EcoFy SPA",
                ClientType.SPA,
                null,
                REDIRECT_URIS
        );

        when(saveClientApplicationPort.save(any()))
                .thenReturn(savedClientApplication);
        when(savedClientApplication.clientId())
                .thenReturn("saved-client-id");
        when(savedClientApplication.clientType())
                .thenReturn(ClientType.SPA);
        when(savedClientApplication.isActive())
                .thenReturn(true);

        // Act
        ClientApplication result = service.register(command);

        // Assert
        ArgumentCaptor<ClientApplication> clientCaptor =
                ArgumentCaptor.forClass(ClientApplication.class);

        assertAll(
                () -> assertSame(savedClientApplication, result),
                () -> verify(
                        passwordHashingPort,
                        never()
                ).hash(any()),
                () -> verify(saveClientApplicationPort).save(
                        clientCaptor.capture()
                )
        );

        ClientApplication createdClient = clientCaptor.getValue();

        assertAll(
                () -> assertEquals(
                        Set.of(
                                GrantType.AUTHORIZATION_CODE,
                                GrantType.REFRESH_TOKEN
                        ),
                        createdClient.grantTypes()
                ),
                () -> assertNull(
                        createdClient.clientSecretHash()
                )
        );
    }

    @Test
    @DisplayName("Deve registrar cliente máquina a máquina com grant padrão e segredo protegido")
    void register_clienteMaquinaAMaquinaSemGrants_deveRegistrarComGrantPadraoESegredo() {
        // Arrange
        ClientApplicationService service = createService();
        RegisterClientCommand command = command(
                "EcoFy Worker",
                ClientType.MACHINE_TO_MACHINE,
                null,
                null
        );
        PasswordHash passwordHash =
                new PasswordHash("m2m-secret-hash");

        when(passwordHashingPort.hash(any()))
                .thenReturn(passwordHash);
        when(saveClientApplicationPort.save(any()))
                .thenReturn(savedClientApplication);
        when(savedClientApplication.clientId())
                .thenReturn("saved-client-id");
        when(savedClientApplication.clientType())
                .thenReturn(ClientType.MACHINE_TO_MACHINE);
        when(savedClientApplication.isActive())
                .thenReturn(true);

        // Act
        ClientApplication result = service.register(command);

        // Assert
        ArgumentCaptor<ClientApplication> clientCaptor =
                ArgumentCaptor.forClass(ClientApplication.class);

        assertAll(
                () -> assertSame(savedClientApplication, result),
                () -> verify(passwordHashingPort).hash(any()),
                () -> verify(saveClientApplicationPort).save(
                        clientCaptor.capture()
                )
        );

        ClientApplication createdClient = clientCaptor.getValue();

        assertAll(
                () -> assertEquals(
                        Set.of(GrantType.CLIENT_CREDENTIALS),
                        createdClient.grantTypes()
                ),
                () -> assertTrue(
                        createdClient.redirectUris().isEmpty()
                ),
                () -> assertEquals(
                        "m2m-secret-hash",
                        createdClient.clientSecretHash()
                )
        );
    }

    @Test
    @DisplayName("Deve preservar os grants explicitamente solicitados para cliente confidencial")
    void register_clienteConfidencialComGrantsInformados_devePreservarGrants() {
        // Arrange
        ClientApplicationService service = createService();
        Set<GrantType> requestedGrants =
                Set.of(GrantType.CLIENT_CREDENTIALS);
        RegisterClientCommand command = command(
                "EcoFy Integration",
                ClientType.CONFIDENTIAL,
                requestedGrants,
                null
        );

        when(passwordHashingPort.hash(any()))
                .thenReturn(new PasswordHash("secret-hash"));
        when(saveClientApplicationPort.save(any()))
                .thenReturn(savedClientApplication);
        when(savedClientApplication.clientId())
                .thenReturn("saved-client-id");
        when(savedClientApplication.clientType())
                .thenReturn(ClientType.CONFIDENTIAL);
        when(savedClientApplication.isActive())
                .thenReturn(true);

        // Act
        ClientApplication result = service.register(command);

        // Assert
        ArgumentCaptor<ClientApplication> clientCaptor =
                ArgumentCaptor.forClass(ClientApplication.class);

        assertAll(
                () -> assertSame(savedClientApplication, result),
                () -> verify(saveClientApplicationPort).save(
                        clientCaptor.capture()
                )
        );

        assertEquals(
                requestedGrants,
                clientCaptor.getValue().grantTypes()
        );
    }

    @Test
    @DisplayName("Deve rejeitar cliente máquina a máquina sem o grant de credenciais do cliente")
    void register_clienteMaquinaAMaquinaSemClientCredentials_deveLancarAuthException() {
        // Arrange
        ClientApplicationService service = createService();
        RegisterClientCommand command = command(
                "EcoFy Worker",
                ClientType.MACHINE_TO_MACHINE,
                Set.of(GrantType.REFRESH_TOKEN),
                null
        );

        when(passwordHashingPort.hash(any()))
                .thenReturn(new PasswordHash("secret-hash"));

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.register(command)
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        AuthErrorCode.CLIENT_NOT_ALLOWED_FOR_GRANT_TYPE,
                        exception.getErrorCode()
                ),
                () -> assertEquals(
                        "M2M client must support CLIENT_CREDENTIALS grant",
                        exception.getMessage()
                ),
                () -> verify(
                        saveClientApplicationPort,
                        never()
                ).save(any())
        );
    }

    @Test
    @DisplayName("Deve rejeitar o grant de código de autorização para cliente máquina a máquina")
    void register_clienteMaquinaAMaquinaComAuthorizationCode_deveLancarAuthException() {
        // Arrange
        ClientApplicationService service = createService();
        RegisterClientCommand command = command(
                "EcoFy Worker",
                ClientType.MACHINE_TO_MACHINE,
                Set.of(
                        GrantType.CLIENT_CREDENTIALS,
                        GrantType.AUTHORIZATION_CODE
                ),
                REDIRECT_URIS
        );

        when(passwordHashingPort.hash(any()))
                .thenReturn(new PasswordHash("secret-hash"));

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.register(command)
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        AuthErrorCode.CLIENT_NOT_ALLOWED_FOR_GRANT_TYPE,
                        exception.getErrorCode()
                ),
                () -> assertEquals(
                        "M2M client cannot use AUTHORIZATION_CODE grant",
                        exception.getMessage()
                ),
                () -> verify(
                        saveClientApplicationPort,
                        never()
                ).save(any())
        );
    }

    @Test
    @DisplayName("Deve rejeitar o grant de senha para cliente máquina a máquina")
    void register_clienteMaquinaAMaquinaComPassword_deveLancarAuthException() {
        // Arrange
        ClientApplicationService service = createService();
        RegisterClientCommand command = command(
                "EcoFy Worker",
                ClientType.MACHINE_TO_MACHINE,
                Set.of(
                        GrantType.CLIENT_CREDENTIALS,
                        GrantType.PASSWORD
                ),
                null
        );

        when(passwordHashingPort.hash(any()))
                .thenReturn(new PasswordHash("secret-hash"));

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.register(command)
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        AuthErrorCode.CLIENT_NOT_ALLOWED_FOR_GRANT_TYPE,
                        exception.getErrorCode()
                ),
                () -> assertEquals(
                        "M2M client cannot use PASSWORD grant",
                        exception.getMessage()
                ),
                () -> verify(
                        saveClientApplicationPort,
                        never()
                ).save(any())
        );
    }

    @Test
    @DisplayName("Deve rejeitar grant de credenciais do cliente para cliente público")
    void register_clientePublicoComClientCredentials_deveLancarAuthException() {
        // Arrange
        ClientApplicationService service = createService();
        RegisterClientCommand command = command(
                "EcoFy Mobile",
                ClientType.PUBLIC,
                Set.of(GrantType.CLIENT_CREDENTIALS),
                null
        );

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.register(command)
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        AuthErrorCode.CLIENT_NOT_ALLOWED_FOR_GRANT_TYPE,
                        exception.getErrorCode()
                ),
                () -> assertEquals(
                        "PUBLIC/SPA clients cannot use CLIENT_CREDENTIALS grant",
                        exception.getMessage()
                ),
                () -> verify(
                        passwordHashingPort,
                        never()
                ).hash(any()),
                () -> verify(
                        saveClientApplicationPort,
                        never()
                ).save(any())
        );
    }

    @Test
    @DisplayName("Deve rejeitar grant de credenciais do cliente para aplicação SPA")
    void register_clienteSpaComClientCredentials_deveLancarAuthException() {
        // Arrange
        ClientApplicationService service = createService();
        RegisterClientCommand command = command(
                "EcoFy SPA",
                ClientType.SPA,
                Set.of(GrantType.CLIENT_CREDENTIALS),
                null
        );

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.register(command)
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        AuthErrorCode.CLIENT_NOT_ALLOWED_FOR_GRANT_TYPE,
                        exception.getErrorCode()
                ),
                () -> assertEquals(
                        "PUBLIC/SPA clients cannot use CLIENT_CREDENTIALS grant",
                        exception.getMessage()
                ),
                () -> verify(
                        passwordHashingPort,
                        never()
                ).hash(any()),
                () -> verify(
                        saveClientApplicationPort,
                        never()
                ).save(any())
        );
    }

    @Test
    @DisplayName("Deve rejeitar código de autorização quando os redirecionamentos forem nulos")
    void register_authorizationCodeComRedirectUrisNulas_deveLancarAuthException() {
        // Arrange
        ClientApplicationService service = createService();
        RegisterClientCommand command = command(
                "EcoFy Mobile",
                ClientType.PUBLIC,
                Set.of(GrantType.AUTHORIZATION_CODE),
                null
        );

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.register(command)
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        AuthErrorCode.INVALID_REDIRECT_URI,
                        exception.getErrorCode()
                ),
                () -> assertEquals(
                        "AUTHORIZATION_CODE grant requires at least one redirectUri configured",
                        exception.getMessage()
                ),
                () -> verify(
                        saveClientApplicationPort,
                        never()
                ).save(any())
        );
    }

    @Test
    @DisplayName("Deve rejeitar código de autorização quando os redirecionamentos estiverem vazios")
    void register_authorizationCodeComRedirectUrisVazias_deveLancarAuthException() {
        // Arrange
        ClientApplicationService service = createService();
        RegisterClientCommand command = command(
                "EcoFy Mobile",
                ClientType.PUBLIC,
                Set.of(GrantType.AUTHORIZATION_CODE),
                Set.of()
        );

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.register(command)
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        AuthErrorCode.INVALID_REDIRECT_URI,
                        exception.getErrorCode()
                ),
                () -> assertEquals(
                        "AUTHORIZATION_CODE grant requires at least one redirectUri configured",
                        exception.getMessage()
                ),
                () -> verify(
                        saveClientApplicationPort,
                        never()
                ).save(any())
        );
    }

    private ClientApplicationService createService() {
        return new ClientApplicationService(
                saveClientApplicationPort,
                passwordHashingPort
        );
    }

    private RegisterClientCommand command(
            String name,
            ClientType clientType,
            Set<GrantType> grantTypes,
            Set<String> redirectUris
    ) {
        return new RegisterClientCommand(
                name,
                clientType,
                grantTypes,
                redirectUris,
                SCOPES,
                true
        );
    }
}
