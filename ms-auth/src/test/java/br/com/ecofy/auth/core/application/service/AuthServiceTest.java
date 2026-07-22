package br.com.ecofy.auth.core.application.service;

import br.com.ecofy.auth.config.JwtProperties;
import br.com.ecofy.auth.core.application.exception.AuthErrorCode;
import br.com.ecofy.auth.core.application.exception.AuthException;
import br.com.ecofy.auth.core.domain.AuthUser;
import br.com.ecofy.auth.core.domain.ClientApplication;
import br.com.ecofy.auth.core.domain.JwtToken;
import br.com.ecofy.auth.core.domain.valueobject.PasswordHash;
import br.com.ecofy.auth.core.domain.Permission;
import br.com.ecofy.auth.core.domain.RefreshToken;
import br.com.ecofy.auth.core.domain.Role;
import br.com.ecofy.auth.core.domain.enums.AuthUserStatus;
import br.com.ecofy.auth.core.domain.enums.ClientType;
import br.com.ecofy.auth.core.domain.enums.GrantType;
import br.com.ecofy.auth.core.domain.enums.TokenType;
import br.com.ecofy.auth.core.domain.event.UserAuthenticatedEvent;
import br.com.ecofy.auth.core.domain.valueobject.AuthUserId;
import br.com.ecofy.auth.core.domain.valueobject.EmailAddress;
import br.com.ecofy.auth.core.port.in.AuthenticateUserUseCase;
import br.com.ecofy.auth.core.port.in.RefreshTokenUseCase;
import br.com.ecofy.auth.core.port.out.JwtTokenProviderPort;
import br.com.ecofy.auth.core.port.out.LoadAuthUserByEmailPort;
import br.com.ecofy.auth.core.port.out.LoadClientApplicationByClientIdPort;
import br.com.ecofy.auth.core.port.out.PasswordHashingPort;
import br.com.ecofy.auth.core.port.out.PublishAuthEventPort;
import br.com.ecofy.auth.core.port.out.RefreshTokenStorePort;
import br.com.ecofy.auth.core.port.out.SaveAuthUserPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Testes unitários do serviço de autenticação")
class AuthServiceTest {

    private static final long ACCESS_TOKEN_TTL = 900L;
    private static final long REFRESH_TOKEN_TTL = 3600L;

    private static final UUID USER_UUID =
            UUID.fromString("11111111-2222-3333-4444-555555555555");

    private static final String CLIENT_ID = "ecofy-web";
    private static final String USER_EMAIL = "user@ecofy.com";
    private static final String PASSWORD = "valid-password";
    private static final String REFRESH_TOKEN_VALUE =
            "refresh-token-value-with-more-than-twelve-characters";

    private LoadAuthUserByEmailPort loadAuthUserByEmailPort;
    private LoadClientApplicationByClientIdPort loadClientApplicationByClientIdPort;
    private PasswordHashingPort passwordHashingPort;
    private JwtTokenProviderPort jwtTokenProviderPort;
    private RefreshTokenStorePort refreshTokenStorePort;
    private PublishAuthEventPort publishAuthEventPort;
    private SaveAuthUserPort saveAuthUserPort;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        loadAuthUserByEmailPort =
                mock(LoadAuthUserByEmailPort.class);

        loadClientApplicationByClientIdPort =
                mock(LoadClientApplicationByClientIdPort.class);

        passwordHashingPort =
                mock(PasswordHashingPort.class);

        jwtTokenProviderPort =
                mock(JwtTokenProviderPort.class);

        refreshTokenStorePort =
                mock(RefreshTokenStorePort.class);

        publishAuthEventPort =
                mock(PublishAuthEventPort.class);

        saveAuthUserPort =
                mock(SaveAuthUserPort.class);

        jwtProperties =
                mock(JwtProperties.class);
    }

    @Test
    @DisplayName("Deve rejeitar cada dependência nula recebida pelo construtor")
    void constructor_dependenciaNula_deveLancarNullPointerException() {
        // Arrange, Act e Assert
        assertAll(
                () -> assertNullDependency(
                        "loadAuthUserByEmailPort must not be null",
                        () -> new AuthService(
                                null,
                                loadClientApplicationByClientIdPort,
                                passwordHashingPort,
                                jwtTokenProviderPort,
                                refreshTokenStorePort,
                                publishAuthEventPort,
                                saveAuthUserPort,
                                jwtProperties
                        )
                ),
                () -> assertNullDependency(
                        "loadClientApplicationByClientIdPort must not be null",
                        () -> new AuthService(
                                loadAuthUserByEmailPort,
                                null,
                                passwordHashingPort,
                                jwtTokenProviderPort,
                                refreshTokenStorePort,
                                publishAuthEventPort,
                                saveAuthUserPort,
                                jwtProperties
                        )
                ),
                () -> assertNullDependency(
                        "passwordHashingPort must not be null",
                        () -> new AuthService(
                                loadAuthUserByEmailPort,
                                loadClientApplicationByClientIdPort,
                                null,
                                jwtTokenProviderPort,
                                refreshTokenStorePort,
                                publishAuthEventPort,
                                saveAuthUserPort,
                                jwtProperties
                        )
                ),
                () -> assertNullDependency(
                        "jwtTokenProviderPort must not be null",
                        () -> new AuthService(
                                loadAuthUserByEmailPort,
                                loadClientApplicationByClientIdPort,
                                passwordHashingPort,
                                null,
                                refreshTokenStorePort,
                                publishAuthEventPort,
                                saveAuthUserPort,
                                jwtProperties
                        )
                ),
                () -> assertNullDependency(
                        "refreshTokenStorePort must not be null",
                        () -> new AuthService(
                                loadAuthUserByEmailPort,
                                loadClientApplicationByClientIdPort,
                                passwordHashingPort,
                                jwtTokenProviderPort,
                                null,
                                publishAuthEventPort,
                                saveAuthUserPort,
                                jwtProperties
                        )
                ),
                () -> assertNullDependency(
                        "publishAuthEventPort must not be null",
                        () -> new AuthService(
                                loadAuthUserByEmailPort,
                                loadClientApplicationByClientIdPort,
                                passwordHashingPort,
                                jwtTokenProviderPort,
                                refreshTokenStorePort,
                                null,
                                saveAuthUserPort,
                                jwtProperties
                        )
                ),
                () -> assertNullDependency(
                        "saveAuthUserPort must not be null",
                        () -> new AuthService(
                                loadAuthUserByEmailPort,
                                loadClientApplicationByClientIdPort,
                                passwordHashingPort,
                                jwtTokenProviderPort,
                                refreshTokenStorePort,
                                publishAuthEventPort,
                                null,
                                jwtProperties
                        )
                ),
                () -> assertNullDependency(
                        "jwtProperties must not be null",
                        () -> new AuthService(
                                loadAuthUserByEmailPort,
                                loadClientApplicationByClientIdPort,
                                passwordHashingPort,
                                jwtTokenProviderPort,
                                refreshTokenStorePort,
                                publishAuthEventPort,
                                saveAuthUserPort,
                                null
                        )
                )
        );
    }

    @Test
    @DisplayName("Deve rejeitar o comando de autenticação nulo")
    void authenticate_comandoNulo_deveLancarNullPointerException() {
        // Arrange
        AuthService service = createService();

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> service.authenticate(null)
        );

        // Assert
        assertEquals(
                "command must not be null",
                exception.getMessage()
        );

        verifyNoInteractions(
                loadClientApplicationByClientIdPort,
                loadAuthUserByEmailPort,
                passwordHashingPort
        );
    }

    @Test
    @DisplayName("Deve rejeitar a autenticação quando o cliente não existir")
    void authenticate_clienteInexistente_deveLancarClientNotFound() {
        // Arrange
        AuthService service = createService();

        AuthenticateUserUseCase.AuthenticationCommand command =
                authenticationCommand(null);

        when(
                loadClientApplicationByClientIdPort.loadByClientId(
                        CLIENT_ID
                )
        ).thenReturn(Optional.empty());

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.authenticate(command)
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        AuthErrorCode.CLIENT_NOT_FOUND,
                        exception.getErrorCode()
                ),
                () -> assertEquals(
                        "Invalid client_id",
                        exception.getMessage()
                )
        );

        verifyNoInteractions(
                loadAuthUserByEmailPort,
                passwordHashingPort,
                jwtTokenProviderPort,
                refreshTokenStorePort,
                publishAuthEventPort,
                saveAuthUserPort
        );
    }

    @Test
    @DisplayName("Deve rejeitar a autenticação quando o cliente estiver inativo")
    void authenticate_clienteInativo_deveLancarClientInactive() {
        // Arrange
        AuthService service = createService();

        AuthenticateUserUseCase.AuthenticationCommand command =
                authenticationCommand(null);

        ClientApplication client = clientApplication(
                false,
                true,
                false,
                ClientType.CONFIDENTIAL
        );

        when(
                loadClientApplicationByClientIdPort.loadByClientId(
                        CLIENT_ID
                )
        ).thenReturn(Optional.of(client));

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.authenticate(command)
        );

        // Assert
        assertEquals(
                AuthErrorCode.CLIENT_INACTIVE,
                exception.getErrorCode()
        );

        verifyNoInteractions(
                loadAuthUserByEmailPort,
                passwordHashingPort,
                jwtTokenProviderPort
        );
    }

    @Test
    @DisplayName("Deve rejeitar a autenticação quando o cliente não suportar o fluxo de senha")
    void authenticate_clienteSemPasswordGrant_deveLancarGrantTypeNaoPermitido() {
        // Arrange
        AuthService service = createService();

        AuthenticateUserUseCase.AuthenticationCommand command =
                authenticationCommand(null);

        ClientApplication client = clientApplication(
                true,
                false,
                false,
                ClientType.CONFIDENTIAL
        );

        when(
                loadClientApplicationByClientIdPort.loadByClientId(
                        CLIENT_ID
                )
        ).thenReturn(Optional.of(client));

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.authenticate(command)
        );

        // Assert
        assertEquals(
                AuthErrorCode.CLIENT_NOT_ALLOWED_FOR_GRANT_TYPE,
                exception.getErrorCode()
        );

        verifyNoInteractions(
                loadAuthUserByEmailPort,
                passwordHashingPort,
                jwtTokenProviderPort
        );
    }

    @Test
    @DisplayName("Deve rejeitar a autenticação quando o tipo do cliente não for permitido")
    void authenticate_tipoDoClienteNulo_deveLancarGrantTypeNaoPermitido() {
        // Arrange
        AuthService service = createService();

        AuthenticateUserUseCase.AuthenticationCommand command =
                authenticationCommand(null);

        ClientApplication client = clientApplication(
                true,
                true,
                false,
                null
        );

        when(
                loadClientApplicationByClientIdPort.loadByClientId(
                        CLIENT_ID
                )
        ).thenReturn(Optional.of(client));

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.authenticate(command)
        );

        // Assert
        assertEquals(
                AuthErrorCode.CLIENT_NOT_ALLOWED_FOR_GRANT_TYPE,
                exception.getErrorCode()
        );

        verifyNoInteractions(
                loadAuthUserByEmailPort,
                passwordHashingPort,
                jwtTokenProviderPort
        );
    }

    @Test
    @DisplayName("Deve rejeitar a autenticação quando o usuário não existir")
    void authenticate_usuarioInexistente_deveLancarInvalidCredentials() {
        // Arrange
        AuthService service = createService();

        AuthenticateUserUseCase.AuthenticationCommand command =
                authenticationCommand(null);

        ClientApplication client = validPasswordClient(
                ClientType.CONFIDENTIAL
        );

        when(
                loadClientApplicationByClientIdPort.loadByClientId(
                        CLIENT_ID
                )
        ).thenReturn(Optional.of(client));

        when(
                loadAuthUserByEmailPort.loadByEmail(
                        any(EmailAddress.class)
                )
        ).thenReturn(Optional.empty());

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.authenticate(command)
        );

        // Assert
        assertEquals(
                AuthErrorCode.INVALID_CREDENTIALS,
                exception.getErrorCode()
        );

        verifyNoInteractions(
                passwordHashingPort,
                jwtTokenProviderPort,
                refreshTokenStorePort,
                publishAuthEventPort,
                saveAuthUserPort
        );
    }

    @Test
    @DisplayName("Deve registrar e persistir a tentativa quando a senha estiver incorreta")
    void authenticate_senhaIncorreta_deveRegistrarFalhaEPersistirUsuario() {
        // Arrange
        AuthService service = createService();

        AuthenticateUserUseCase.AuthenticationCommand command =
                authenticationCommand(null);

        ClientApplication client = validPasswordClient(
                ClientType.CONFIDENTIAL
        );

        AuthUser user = authUser(
                AuthUserStatus.ACTIVE,
                true,
                Set.of(),
                Set.of()
        );

        when(user.failedLoginAttempts())
                .thenReturn(5);

        prepareAuthentication(
                client,
                user,
                false
        );

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.authenticate(command)
        );

        // Assert
        assertEquals(
                AuthErrorCode.INVALID_CREDENTIALS,
                exception.getErrorCode()
        );

        verify(user).registerFailedLogin(5);
        verify(saveAuthUserPort).save(user);

        verifyNoInteractions(
                jwtTokenProviderPort,
                refreshTokenStorePort,
                publishAuthEventPort
        );
    }

    @Test
    @DisplayName("Deve rejeitar a autenticação quando o usuário estiver excluído")
    void authenticate_usuarioExcluido_deveLancarUserBlocked() {
        assertUserCannotAuthenticate(
                AuthUserStatus.DELETED,
                true,
                AuthErrorCode.USER_BLOCKED
        );
    }

    @Test
    @DisplayName("Deve rejeitar a autenticação quando o usuário estiver bloqueado")
    void authenticate_usuarioBloqueado_deveLancarUserBlocked() {
        assertUserCannotAuthenticate(
                AuthUserStatus.BLOCKED,
                true,
                AuthErrorCode.USER_BLOCKED
        );
    }

    @Test
    @DisplayName("Deve rejeitar a autenticação quando o usuário estiver bloqueado por tentativas")
    void authenticate_usuarioLocked_deveLancarUserLocked() {
        assertUserCannotAuthenticate(
                AuthUserStatus.LOCKED,
                true,
                AuthErrorCode.USER_LOCKED
        );
    }

    @Test
    @DisplayName("Deve rejeitar a autenticação quando a confirmação de e-mail estiver pendente")
    void authenticate_usuarioPendenteDeConfirmacao_deveLancarEmailNotVerified() {
        assertUserCannotAuthenticate(
                AuthUserStatus.PENDING_EMAIL_CONFIRMATION,
                true,
                AuthErrorCode.EMAIL_NOT_VERIFIED
        );
    }

    @Test
    @DisplayName("Deve rejeitar a autenticação quando o usuário ativo não tiver confirmado o e-mail")
    void authenticate_usuarioAtivoComEmailNaoVerificado_deveLancarEmailNotVerified() {
        assertUserCannotAuthenticate(
                AuthUserStatus.ACTIVE,
                false,
                AuthErrorCode.EMAIL_NOT_VERIFIED
        );
    }

    @Test
    @DisplayName("Deve autenticar o usuário e emitir claims, tokens e evento")
    void authenticate_dadosValidos_deveEmitirTokensSalvarSessaoEPublicarEvento() {
        // Arrange
        AuthService service = createService();

        String scope = "profile:read transactions:read";

        AuthenticateUserUseCase.AuthenticationCommand command =
                authenticationCommand(scope);

        ClientApplication client = validPasswordClient(
                ClientType.CONFIDENTIAL
        );

        Permission roleWritePermission = permission(
                "auth:user:write"
        );

        Permission roleReadPermission = permission(
                "auth:user:read"
        );

        Permission directPermission = permission(
                "auth:user:admin"
        );

        Permission duplicatedDirectPermission = permission(
                "auth:user:write"
        );

        Role userRole = role(
                "ROLE_USER",
                Set.of(roleReadPermission)
        );

        Role adminRole = role(
                "ROLE_ADMIN",
                Set.of(roleWritePermission)
        );

        Role unnamedRole = role(
                null,
                Set.of()
        );

        AuthUser user = authUser(
                AuthUserStatus.ACTIVE,
                true,
                Set.of(
                        userRole,
                        adminRole,
                        unnamedRole
                ),
                Set.of(
                        directPermission,
                        duplicatedDirectPermission
                )
        );

        prepareAuthentication(
                client,
                user,
                true
        );

        JwtToken accessToken = mock(JwtToken.class);
        JwtToken refreshJwt = mock(JwtToken.class);

        when(refreshJwt.value())
                .thenReturn(REFRESH_TOKEN_VALUE);

        when(
                jwtTokenProviderPort.generateAccessToken(
                        anyString(),
                        anyMap(),
                        eq(ACCESS_TOKEN_TTL)
                )
        ).thenReturn(accessToken);

        when(
                jwtTokenProviderPort.generateRefreshToken(
                        anyString(),
                        anyMap(),
                        eq(REFRESH_TOKEN_TTL)
                )
        ).thenReturn(refreshJwt);

        ArgumentCaptor<Map<String, Object>> accessClaimsCaptor =
                mapCaptor();

        ArgumentCaptor<Map<String, Object>> refreshClaimsCaptor =
                mapCaptor();

        ArgumentCaptor<RefreshToken> storedTokenCaptor =
                ArgumentCaptor.forClass(RefreshToken.class);

        // Act
        AuthenticateUserUseCase.AuthenticationResult result =
                service.authenticate(command);

        // Assert
        assertAll(
                () -> assertSame(
                        accessToken,
                        result.accessToken()
                ),
                () -> assertEquals(
                        REFRESH_TOKEN_VALUE,
                        result.refreshToken()
                ),
                () -> assertEquals(
                        ACCESS_TOKEN_TTL,
                        result.expiresInSeconds()
                ),
                () -> assertEquals(
                        "Bearer",
                        result.tokenType()
                )
        );

        verify(user).registerSuccessfulLogin();
        verify(saveAuthUserPort).save(user);

        verify(jwtTokenProviderPort).generateAccessToken(
                eq(USER_UUID.toString()),
                accessClaimsCaptor.capture(),
                eq(ACCESS_TOKEN_TTL)
        );

        verify(jwtTokenProviderPort).generateRefreshToken(
                eq(USER_UUID.toString()),
                refreshClaimsCaptor.capture(),
                eq(REFRESH_TOKEN_TTL)
        );

        Map<String, Object> accessClaims =
                accessClaimsCaptor.getValue();

        Map<String, Object> refreshClaims =
                refreshClaimsCaptor.getValue();

        assertAll(
                () -> assertEquals(
                        USER_UUID.toString(),
                        accessClaims.get("sub")
                ),
                () -> assertEquals(
                        USER_EMAIL,
                        accessClaims.get("email")
                ),
                () -> assertEquals(
                        "EcoFy User",
                        accessClaims.get("name")
                ),
                () -> assertEquals(
                        CLIENT_ID,
                        accessClaims.get("client_id")
                ),
                () -> assertEquals(
                        scope,
                        accessClaims.get("scope")
                ),
                () -> assertEquals(
                        List.of(
                                "ROLE_ADMIN",
                                "ROLE_USER"
                        ),
                        accessClaims.get("roles")
                ),
                () -> assertEquals(
                        List.of(
                                "auth:user:admin",
                                "auth:user:read",
                                "auth:user:write"
                        ),
                        accessClaims.get("permissions")
                ),
                () -> assertEquals(
                        USER_UUID.toString(),
                        refreshClaims.get("sub")
                ),
                () -> assertEquals(
                        CLIENT_ID,
                        refreshClaims.get("client_id")
                )
        );

        verify(refreshTokenStorePort)
                .save(storedTokenCaptor.capture());

        RefreshToken storedToken =
                storedTokenCaptor.getValue();

        assertAll(
                () -> assertSame(
                        user.id(),
                        storedToken.userId()
                ),
                () -> assertEquals(
                        CLIENT_ID,
                        storedToken.clientId()
                ),
                () -> assertEquals(
                        REFRESH_TOKEN_VALUE,
                        storedToken.tokenValue()
                ),
                () -> assertTrue(storedToken.isActive())
        );

        verify(publishAuthEventPort)
                .publish(any(UserAuthenticatedEvent.class));
    }

    @Test
    @DisplayName("Deve permitir cliente SPA e omitir claims opcionais quando estiverem vazias")
    void authenticate_clienteSpaComEscopoEmBranco_deveOmitirClaimsOpcionais() {
        // Arrange
        AuthService service = createService();

        AuthenticateUserUseCase.AuthenticationCommand command =
                authenticationCommand("   ");

        ClientApplication client = validPasswordClient(
                ClientType.SPA
        );

        AuthUser user = authUser(
                AuthUserStatus.ACTIVE,
                true,
                Set.of(),
                Set.of()
        );

        prepareAuthentication(
                client,
                user,
                true
        );

        JwtToken accessToken = mock(JwtToken.class);
        JwtToken refreshJwt = mock(JwtToken.class);

        when(refreshJwt.value())
                .thenReturn(REFRESH_TOKEN_VALUE);

        when(
                jwtTokenProviderPort.generateAccessToken(
                        anyString(),
                        anyMap(),
                        eq(ACCESS_TOKEN_TTL)
                )
        ).thenReturn(accessToken);

        when(
                jwtTokenProviderPort.generateRefreshToken(
                        anyString(),
                        anyMap(),
                        eq(REFRESH_TOKEN_TTL)
                )
        ).thenReturn(refreshJwt);

        ArgumentCaptor<Map<String, Object>> claimsCaptor =
                mapCaptor();

        // Act
        AuthenticateUserUseCase.AuthenticationResult result =
                service.authenticate(command);

        // Assert
        assertNotNull(result);

        verify(jwtTokenProviderPort).generateAccessToken(
                eq(USER_UUID.toString()),
                claimsCaptor.capture(),
                eq(ACCESS_TOKEN_TTL)
        );

        Map<String, Object> claims =
                claimsCaptor.getValue();

        assertAll(
                () -> assertFalse(claims.containsKey("scope")),
                () -> assertFalse(claims.containsKey("roles")),
                () -> assertFalse(claims.containsKey("permissions"))
        );
    }

    @Test
    @DisplayName("Deve omitir a claim de escopo quando o valor informado for nulo")
    void authenticate_escopoNulo_deveOmitirClaimDeEscopo() {
        // Arrange
        AuthService service = createService();

        AuthenticateUserUseCase.AuthenticationCommand command =
                authenticationCommand(null);

        ClientApplication client = validPasswordClient(
                ClientType.CONFIDENTIAL
        );

        AuthUser user = authUser(
                AuthUserStatus.ACTIVE,
                true,
                Set.of(),
                Set.of()
        );

        prepareAuthentication(
                client,
                user,
                true
        );

        JwtToken accessToken = mock(JwtToken.class);
        JwtToken refreshJwt = mock(JwtToken.class);

        when(refreshJwt.value())
                .thenReturn(REFRESH_TOKEN_VALUE);

        when(
                jwtTokenProviderPort.generateAccessToken(
                        anyString(),
                        anyMap(),
                        eq(ACCESS_TOKEN_TTL)
                )
        ).thenReturn(accessToken);

        when(
                jwtTokenProviderPort.generateRefreshToken(
                        anyString(),
                        anyMap(),
                        eq(REFRESH_TOKEN_TTL)
                )
        ).thenReturn(refreshJwt);

        ArgumentCaptor<Map<String, Object>> claimsCaptor =
                mapCaptor();

        // Act
        service.authenticate(command);

        // Assert
        verify(jwtTokenProviderPort).generateAccessToken(
                eq(USER_UUID.toString()),
                claimsCaptor.capture(),
                eq(ACCESS_TOKEN_TTL)
        );

        assertFalse(
                claimsCaptor.getValue().containsKey("scope")
        );
    }

    @Test
    @DisplayName("Deve rejeitar o comando de renovação nulo")
    void refresh_comandoNulo_deveLancarNullPointerException() {
        // Arrange
        AuthService service = createService();

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> service.refresh(null)
        );

        // Assert
        assertEquals(
                "command must not be null",
                exception.getMessage()
        );

        verifyNoInteractions(
                refreshTokenStorePort,
                jwtTokenProviderPort,
                loadClientApplicationByClientIdPort
        );
    }

    @Test
    @DisplayName("Deve rejeitar o refresh token nulo quando ele não estiver armazenado")
    void refresh_tokenNulo_deveLancarTokenNotFound() {
        assertRefreshTokenNotFound(null);
    }

    @Test
    @DisplayName("Deve rejeitar o refresh token em branco quando ele não estiver armazenado")
    void refresh_tokenEmBranco_deveLancarTokenNotFound() {
        assertRefreshTokenNotFound("   ");
    }

    @Test
    @DisplayName("Deve rejeitar o refresh token curto quando ele não estiver armazenado")
    void refresh_tokenCurto_deveLancarTokenNotFound() {
        assertRefreshTokenNotFound("short-token");
    }

    @Test
    @DisplayName("Deve rejeitar o refresh token longo quando ele não estiver armazenado")
    void refresh_tokenLongo_deveLancarTokenNotFound() {
        assertRefreshTokenNotFound(
                REFRESH_TOKEN_VALUE
        );
    }

    @Test
    @DisplayName("Deve rejeitar o refresh token expirado ou revogado")
    void refresh_tokenInativo_deveLancarTokenExpired() {
        // Arrange
        AuthService service = createService();

        RefreshTokenUseCase.RefreshTokenCommand command =
                refreshCommand(
                        REFRESH_TOKEN_VALUE,
                        CLIENT_ID
                );

        RefreshToken stored = storedRefreshToken(
                false,
                REFRESH_TOKEN_VALUE,
                CLIENT_ID
        );

        when(
                refreshTokenStorePort.findByTokenValue(
                        REFRESH_TOKEN_VALUE
                )
        ).thenReturn(Optional.of(stored));

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.refresh(command)
        );

        // Assert
        assertEquals(
                AuthErrorCode.TOKEN_EXPIRED,
                exception.getErrorCode()
        );

        verifyNoInteractions(
                jwtTokenProviderPort,
                loadClientApplicationByClientIdPort
        );
    }

    @Test
    @DisplayName("Deve rejeitar as claims quando o tipo do token estiver ausente")
    void refresh_tipoDoTokenAusente_deveLancarTipoNaoSuportado() {
        // Arrange
        Map<String, Object> claims = new HashMap<>();

        claims.put("sub", USER_UUID.toString());
        claims.put("client_id", CLIENT_ID);

        // Act
        AuthException exception =
                executeRefreshUntilClaimsValidation(claims);

        // Assert
        assertEquals(
                AuthErrorCode.TOKEN_TYPE_NOT_SUPPORTED_FOR_REVOCATION,
                exception.getErrorCode()
        );
    }

    @Test
    @DisplayName("Deve rejeitar as claims quando o token não for do tipo refresh")
    void refresh_tipoDoTokenIncorreto_deveLancarTipoNaoSuportado() {
        // Arrange
        Map<String, Object> claims = new HashMap<>();

        claims.put("typ", "ACCESS");
        claims.put("sub", USER_UUID.toString());
        claims.put("client_id", CLIENT_ID);

        // Act
        AuthException exception =
                executeRefreshUntilClaimsValidation(claims);

        // Assert
        assertEquals(
                AuthErrorCode.TOKEN_TYPE_NOT_SUPPORTED_FOR_REVOCATION,
                exception.getErrorCode()
        );
    }

    @Test
    @DisplayName("Deve rejeitar as claims quando o identificador do usuário estiver ausente")
    void refresh_subAusente_deveLancarInvalidTokenSignature() {
        // Arrange
        Map<String, Object> claims = new HashMap<>();

        claims.put("typ", TokenType.REFRESH.name());
        claims.put("client_id", CLIENT_ID);

        // Act
        AuthException exception =
                executeRefreshUntilClaimsValidation(claims);

        // Assert
        assertEquals(
                AuthErrorCode.INVALID_TOKEN_SIGNATURE,
                exception.getErrorCode()
        );
    }

    @Test
    @DisplayName("Deve rejeitar as claims quando o identificador do cliente estiver ausente")
    void refresh_clientIdAusente_deveLancarInvalidTokenSignature() {
        // Arrange
        Map<String, Object> claims = new HashMap<>();

        claims.put("typ", TokenType.REFRESH.name());
        claims.put("sub", USER_UUID.toString());

        // Act
        AuthException exception =
                executeRefreshUntilClaimsValidation(claims);

        // Assert
        assertEquals(
                AuthErrorCode.INVALID_TOKEN_SIGNATURE,
                exception.getErrorCode()
        );
    }

    @Test
    @DisplayName("Deve rejeitar o token quando o cliente armazenado for diferente do solicitado")
    void refresh_clienteArmazenadoDiferente_deveLancarTokenOwnerMismatch() {
        // Arrange
        AuthService service = createService();

        RefreshTokenUseCase.RefreshTokenCommand command =
                refreshCommand(
                        REFRESH_TOKEN_VALUE,
                        CLIENT_ID
                );

        RefreshToken stored = storedRefreshToken(
                true,
                REFRESH_TOKEN_VALUE,
                "another-client"
        );

        Map<String, Object> claims =
                validRefreshClaims(CLIENT_ID);

        when(
                refreshTokenStorePort.findByTokenValue(
                        REFRESH_TOKEN_VALUE
                )
        ).thenReturn(Optional.of(stored));

        when(
                jwtTokenProviderPort.parseClaims(
                        REFRESH_TOKEN_VALUE
                )
        ).thenReturn(claims);

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.refresh(command)
        );

        // Assert
        assertEquals(
                AuthErrorCode.TOKEN_OWNER_MISMATCH,
                exception.getErrorCode()
        );

        verifyNoInteractions(
                loadClientApplicationByClientIdPort
        );
    }

    @Test
    @DisplayName("Deve rejeitar o token quando o cliente das claims for diferente do solicitado")
    void refresh_clienteDasClaimsDiferente_deveLancarTokenOwnerMismatch() {
        // Arrange
        AuthService service = createService();

        RefreshTokenUseCase.RefreshTokenCommand command =
                refreshCommand(
                        REFRESH_TOKEN_VALUE,
                        CLIENT_ID
                );

        RefreshToken stored = storedRefreshToken(
                true,
                REFRESH_TOKEN_VALUE,
                CLIENT_ID
        );

        Map<String, Object> claims =
                validRefreshClaims("another-client");

        when(
                refreshTokenStorePort.findByTokenValue(
                        REFRESH_TOKEN_VALUE
                )
        ).thenReturn(Optional.of(stored));

        when(
                jwtTokenProviderPort.parseClaims(
                        REFRESH_TOKEN_VALUE
                )
        ).thenReturn(claims);

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.refresh(command)
        );

        // Assert
        assertEquals(
                AuthErrorCode.TOKEN_OWNER_MISMATCH,
                exception.getErrorCode()
        );

        verifyNoInteractions(
                loadClientApplicationByClientIdPort
        );
    }

    @Test
    @DisplayName("Deve rejeitar a renovação quando o cliente não existir")
    void refresh_clienteInexistente_deveLancarClientNotFound() {
        // Arrange
        AuthService service = createService();

        prepareValidStoredRefreshToken();

        when(
                loadClientApplicationByClientIdPort.loadByClientId(
                        CLIENT_ID
                )
        ).thenReturn(Optional.empty());

        RefreshTokenUseCase.RefreshTokenCommand command =
                refreshCommand(
                        REFRESH_TOKEN_VALUE,
                        CLIENT_ID
                );

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.refresh(command)
        );

        // Assert
        assertEquals(
                AuthErrorCode.CLIENT_NOT_FOUND,
                exception.getErrorCode()
        );

        verify(jwtTokenProviderPort, never())
                .generateAccessToken(
                        anyString(),
                        anyMap(),
                        eq(ACCESS_TOKEN_TTL)
                );
    }

    @Test
    @DisplayName("Deve rejeitar a renovação quando o cliente estiver inativo")
    void refresh_clienteInativo_deveLancarClientInactive() {
        // Arrange
        AuthService service = createService();

        prepareValidStoredRefreshToken();

        ClientApplication client = clientApplication(
                false,
                false,
                true,
                ClientType.CONFIDENTIAL
        );

        when(
                loadClientApplicationByClientIdPort.loadByClientId(
                        CLIENT_ID
                )
        ).thenReturn(Optional.of(client));

        RefreshTokenUseCase.RefreshTokenCommand command =
                refreshCommand(
                        REFRESH_TOKEN_VALUE,
                        CLIENT_ID
                );

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.refresh(command)
        );

        // Assert
        assertEquals(
                AuthErrorCode.CLIENT_INACTIVE,
                exception.getErrorCode()
        );

        verify(refreshTokenStorePort, never())
                .revoke(anyString());
    }

    @Test
    @DisplayName("Deve rejeitar a renovação quando o cliente não suportar refresh token")
    void refresh_clienteSemRefreshGrant_deveLancarGrantTypeNaoPermitido() {
        // Arrange
        AuthService service = createService();

        prepareValidStoredRefreshToken();

        ClientApplication client = clientApplication(
                true,
                false,
                false,
                ClientType.CONFIDENTIAL
        );

        when(
                loadClientApplicationByClientIdPort.loadByClientId(
                        CLIENT_ID
                )
        ).thenReturn(Optional.of(client));

        RefreshTokenUseCase.RefreshTokenCommand command =
                refreshCommand(
                        REFRESH_TOKEN_VALUE,
                        CLIENT_ID
                );

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.refresh(command)
        );

        // Assert
        assertEquals(
                AuthErrorCode.CLIENT_NOT_ALLOWED_FOR_GRANT_TYPE,
                exception.getErrorCode()
        );

        verify(refreshTokenStorePort, never())
                .revoke(anyString());
    }

    @Test
    @DisplayName("Deve emitir novos tokens, armazenar o refresh e revogar o anterior")
    void refresh_tokenValido_deveRenovarTokensERevogarTokenAnterior() {
        // Arrange
        AuthService service = createService();

        RefreshTokenUseCase.RefreshTokenCommand command =
                refreshCommand(
                        REFRESH_TOKEN_VALUE,
                        CLIENT_ID
                );

        RefreshToken stored = storedRefreshToken(
                true,
                REFRESH_TOKEN_VALUE,
                CLIENT_ID
        );

        Map<String, Object> claims =
                validRefreshClaims(CLIENT_ID);

        ClientApplication client = clientApplication(
                true,
                false,
                true,
                ClientType.CONFIDENTIAL
        );

        when(
                refreshTokenStorePort.findByTokenValue(
                        REFRESH_TOKEN_VALUE
                )
        ).thenReturn(Optional.of(stored));

        when(
                jwtTokenProviderPort.parseClaims(
                        REFRESH_TOKEN_VALUE
                )
        ).thenReturn(claims);

        when(
                loadClientApplicationByClientIdPort.loadByClientId(
                        CLIENT_ID
                )
        ).thenReturn(Optional.of(client));

        JwtToken newAccess = mock(JwtToken.class);
        JwtToken newRefresh = mock(JwtToken.class);

        when(newAccess.value())
                .thenReturn("new-access-token");

        when(newRefresh.value())
                .thenReturn("new-refresh-token");

        when(
                jwtTokenProviderPort.generateAccessToken(
                        USER_UUID.toString(),
                        claims,
                        ACCESS_TOKEN_TTL
                )
        ).thenReturn(newAccess);

        when(
                jwtTokenProviderPort.generateRefreshToken(
                        eq(USER_UUID.toString()),
                        anyMap(),
                        eq(REFRESH_TOKEN_TTL)
                )
        ).thenReturn(newRefresh);

        ArgumentCaptor<Map<String, Object>> refreshClaimsCaptor =
                mapCaptor();

        ArgumentCaptor<RefreshToken> newStoredTokenCaptor =
                ArgumentCaptor.forClass(RefreshToken.class);

        // Act
        RefreshTokenUseCase.RefreshTokenResult result =
                service.refresh(command);

        // Assert
        assertAll(
                () -> assertEquals(
                        "new-access-token",
                        result.accessToken()
                ),
                () -> assertEquals(
                        "new-refresh-token",
                        result.refreshToken()
                ),
                () -> assertEquals(
                        ACCESS_TOKEN_TTL,
                        result.expiresInSeconds()
                )
        );

        verify(jwtTokenProviderPort).generateAccessToken(
                USER_UUID.toString(),
                claims,
                ACCESS_TOKEN_TTL
        );

        verify(jwtTokenProviderPort).generateRefreshToken(
                eq(USER_UUID.toString()),
                refreshClaimsCaptor.capture(),
                eq(REFRESH_TOKEN_TTL)
        );

        assertEquals(
                Map.of("client_id", CLIENT_ID),
                refreshClaimsCaptor.getValue()
        );

        verify(refreshTokenStorePort)
                .save(newStoredTokenCaptor.capture());

        RefreshToken newStoredToken =
                newStoredTokenCaptor.getValue();

        assertAll(
                () -> assertSame(
                        stored.userId(),
                        newStoredToken.userId()
                ),
                () -> assertEquals(
                        CLIENT_ID,
                        newStoredToken.clientId()
                ),
                () -> assertEquals(
                        "new-refresh-token",
                        newStoredToken.tokenValue()
                ),
                () -> assertTrue(
                        newStoredToken.isActive()
                )
        );

        verify(refreshTokenStorePort)
                .revoke(REFRESH_TOKEN_VALUE);
    }

    private AuthService createService() {
        when(jwtProperties.getAccessTokenTtlSeconds())
                .thenReturn(ACCESS_TOKEN_TTL);

        when(jwtProperties.getRefreshTokenTtlSeconds())
                .thenReturn(REFRESH_TOKEN_TTL);

        return new AuthService(
                loadAuthUserByEmailPort,
                loadClientApplicationByClientIdPort,
                passwordHashingPort,
                jwtTokenProviderPort,
                refreshTokenStorePort,
                publishAuthEventPort,
                saveAuthUserPort,
                jwtProperties
        );
    }

    private AuthenticateUserUseCase.AuthenticationCommand authenticationCommand(
            String scope
    ) {
        AuthenticateUserUseCase.AuthenticationCommand command =
                mock(AuthenticateUserUseCase.AuthenticationCommand.class);

        when(command.clientId())
                .thenReturn(CLIENT_ID);

        when(command.username())
                .thenReturn(USER_EMAIL);

        when(command.password())
                .thenReturn(PASSWORD);

        when(command.scope())
                .thenReturn(scope);

        when(command.ipAddress())
                .thenReturn("127.0.0.1");

        return command;
    }

    private RefreshTokenUseCase.RefreshTokenCommand refreshCommand(
            String refreshToken,
            String clientId
    ) {
        RefreshTokenUseCase.RefreshTokenCommand command =
                mock(RefreshTokenUseCase.RefreshTokenCommand.class);

        when(command.refreshToken())
                .thenReturn(refreshToken);

        when(command.clientId())
                .thenReturn(clientId);

        return command;
    }

    private ClientApplication validPasswordClient(
            ClientType clientType
    ) {
        return clientApplication(
                true,
                true,
                false,
                clientType
        );
    }

    private ClientApplication clientApplication(
            boolean active,
            boolean supportsPasswordGrant,
            boolean supportsRefreshGrant,
            ClientType clientType
    ) {
        ClientApplication client =
                mock(ClientApplication.class);

        when(client.clientId())
                .thenReturn(CLIENT_ID);

        when(client.isActive())
                .thenReturn(active);

        when(
                client.supportsGrant(
                        GrantType.PASSWORD
                )
        ).thenReturn(supportsPasswordGrant);

        when(
                client.supportsGrant(
                        GrantType.REFRESH_TOKEN
                )
        ).thenReturn(supportsRefreshGrant);

        when(client.clientType())
                .thenReturn(clientType);

        return client;
    }

    private AuthUser authUser(
            AuthUserStatus status,
            boolean emailVerified,
            Set<Role> roles,
            Set<Permission> directPermissions
    ) {
        AuthUser user = mock(AuthUser.class);
        AuthUserId userId = mock(AuthUserId.class);
        PasswordHash passwordHash = mock(PasswordHash.class);

        when(userId.value())
                .thenReturn(USER_UUID);

        when(user.id())
                .thenReturn(userId);

        when(user.email())
                .thenReturn(new EmailAddress(USER_EMAIL));

        when(user.fullName())
                .thenReturn("EcoFy User");

        when(user.passwordHash())
                .thenReturn(passwordHash);

        when(user.status())
                .thenReturn(status);

        when(user.isEmailVerified())
                .thenReturn(emailVerified);

        when(user.roles())
                .thenReturn(roles);

        when(user.directPermissions())
                .thenReturn(directPermissions);

        return user;
    }

    private Role role(
            String name,
            Set<Permission> permissions
    ) {
        Role role = mock(Role.class);

        when(role.name())
                .thenReturn(name);

        when(role.permissions())
                .thenReturn(permissions);

        return role;
    }

    private Permission permission(String name) {
        Permission permission = mock(Permission.class);

        when(permission.name())
                .thenReturn(name);

        return permission;
    }

    private void prepareAuthentication(
            ClientApplication client,
            AuthUser user,
            boolean passwordMatches
    ) {
        when(
                loadClientApplicationByClientIdPort.loadByClientId(
                        CLIENT_ID
                )
        ).thenReturn(Optional.of(client));

        when(
                loadAuthUserByEmailPort.loadByEmail(
                        any(EmailAddress.class)
                )
        ).thenReturn(Optional.of(user));

        when(
                passwordHashingPort.matches(
                        PASSWORD,
                        user.passwordHash()
                )
        ).thenReturn(passwordMatches);
    }

    private void assertUserCannotAuthenticate(
            AuthUserStatus status,
            boolean emailVerified,
            AuthErrorCode expectedErrorCode
    ) {
        // Arrange
        AuthService service = createService();

        AuthenticateUserUseCase.AuthenticationCommand command =
                authenticationCommand(null);

        ClientApplication client = validPasswordClient(
                ClientType.CONFIDENTIAL
        );

        AuthUser user = authUser(
                status,
                emailVerified,
                Set.of(),
                Set.of()
        );

        prepareAuthentication(
                client,
                user,
                true
        );

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.authenticate(command)
        );

        // Assert
        assertEquals(
                expectedErrorCode,
                exception.getErrorCode()
        );

        verify(user, never())
                .registerSuccessfulLogin();

        verify(saveAuthUserPort, never())
                .save(user);

        verifyNoInteractions(
                jwtTokenProviderPort,
                refreshTokenStorePort,
                publishAuthEventPort
        );
    }

    private void assertRefreshTokenNotFound(
            String token
    ) {
        // Arrange
        AuthService service = createService();

        RefreshTokenUseCase.RefreshTokenCommand command =
                refreshCommand(
                        token,
                        CLIENT_ID
                );

        when(
                refreshTokenStorePort.findByTokenValue(
                        token
                )
        ).thenReturn(Optional.empty());

        // Act
        AuthException exception = assertThrows(
                AuthException.class,
                () -> service.refresh(command)
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        AuthErrorCode.TOKEN_NOT_FOUND,
                        exception.getErrorCode()
                ),
                () -> assertEquals(
                        "Invalid refresh token",
                        exception.getMessage()
                )
        );

        verifyNoInteractions(
                jwtTokenProviderPort,
                loadClientApplicationByClientIdPort
        );
    }

    private AuthException executeRefreshUntilClaimsValidation(
            Map<String, Object> claims
    ) {
        AuthService service = createService();

        RefreshTokenUseCase.RefreshTokenCommand command =
                refreshCommand(
                        REFRESH_TOKEN_VALUE,
                        CLIENT_ID
                );

        RefreshToken stored = storedRefreshToken(
                true,
                REFRESH_TOKEN_VALUE,
                CLIENT_ID
        );

        when(
                refreshTokenStorePort.findByTokenValue(
                        REFRESH_TOKEN_VALUE
                )
        ).thenReturn(Optional.of(stored));

        when(
                jwtTokenProviderPort.parseClaims(
                        REFRESH_TOKEN_VALUE
                )
        ).thenReturn(claims);

        return assertThrows(
                AuthException.class,
                () -> service.refresh(command)
        );
    }

    private RefreshToken storedRefreshToken(
            boolean active,
            String tokenValue,
            String clientId
    ) {
        RefreshToken stored = mock(RefreshToken.class);
        AuthUserId userId = mock(AuthUserId.class);

        when(userId.value())
                .thenReturn(USER_UUID);

        when(stored.userId())
                .thenReturn(userId);

        when(stored.tokenValue())
                .thenReturn(tokenValue);

        when(stored.clientId())
                .thenReturn(clientId);

        when(stored.isActive())
                .thenReturn(active);

        return stored;
    }

    private void prepareValidStoredRefreshToken() {
        RefreshToken stored = storedRefreshToken(
                true,
                REFRESH_TOKEN_VALUE,
                CLIENT_ID
        );

        when(
                refreshTokenStorePort.findByTokenValue(
                        REFRESH_TOKEN_VALUE
                )
        ).thenReturn(Optional.of(stored));

        when(
                jwtTokenProviderPort.parseClaims(
                        REFRESH_TOKEN_VALUE
                )
        ).thenReturn(validRefreshClaims(CLIENT_ID));
    }

    private Map<String, Object> validRefreshClaims(
            String clientId
    ) {
        Map<String, Object> claims = new HashMap<>();

        claims.put(
                "typ",
                TokenType.REFRESH.name()
        );

        claims.put(
                "sub",
                USER_UUID.toString()
        );

        claims.put(
                "client_id",
                clientId
        );

        return claims;
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

    @SuppressWarnings({
            "unchecked",
            "rawtypes"
    })
    private ArgumentCaptor<Map<String, Object>> mapCaptor() {
        return (ArgumentCaptor)
                ArgumentCaptor.forClass(Map.class);
    }
}
