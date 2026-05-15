package br.com.ecofy.auth.core.application.service;

import br.com.ecofy.auth.config.JwtProperties;
import br.com.ecofy.auth.core.application.exception.AuthErrorCode;
import br.com.ecofy.auth.core.application.exception.AuthException;
import br.com.ecofy.auth.core.domain.AuthUser;
import br.com.ecofy.auth.core.domain.ClientApplication;
import br.com.ecofy.auth.core.domain.JwtToken;
import br.com.ecofy.auth.core.domain.RefreshToken;
import br.com.ecofy.auth.core.domain.enums.ClientType;
import br.com.ecofy.auth.core.domain.enums.GrantType;
import br.com.ecofy.auth.core.domain.enums.TokenType;
import br.com.ecofy.auth.core.domain.event.UserAuthenticatedEvent;
import br.com.ecofy.auth.core.domain.event.UserRegisteredEvent;
import br.com.ecofy.auth.core.domain.valueobject.AuthUserId;
import br.com.ecofy.auth.core.domain.valueobject.EmailAddress;
import br.com.ecofy.auth.core.domain.valueobject.PasswordHash;
import br.com.ecofy.auth.core.port.in.AuthenticateUserUseCase;
import br.com.ecofy.auth.core.port.in.RefreshTokenUseCase;
import br.com.ecofy.auth.core.port.out.JwtTokenProviderPort;
import br.com.ecofy.auth.core.port.out.LoadAuthUserByEmailPort;
import br.com.ecofy.auth.core.port.out.LoadClientApplicationByClientIdPort;
import br.com.ecofy.auth.core.port.out.PasswordHashingPort;
import br.com.ecofy.auth.core.port.out.PublishAuthEventPort;
import br.com.ecofy.auth.core.port.out.RefreshTokenStorePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private LoadAuthUserByEmailPort loadAuthUserByEmailPort;

    @Mock
    private LoadClientApplicationByClientIdPort loadClientApplicationByClientIdPort;

    @Mock
    private PasswordHashingPort passwordHashingPort;

    @Mock
    private JwtTokenProviderPort jwtTokenProviderPort;

    @Mock
    private RefreshTokenStorePort refreshTokenStorePort;

    @Mock
    private PublishAuthEventPort publishAuthEventPort;

    private AuthService service(long accessTtl, long refreshTtl) {
        JwtProperties props = mock(JwtProperties.class);
        when(props.getAccessTokenTtlSeconds()).thenReturn(accessTtl);
        when(props.getRefreshTokenTtlSeconds()).thenReturn(refreshTtl);

        return new AuthService(
                loadAuthUserByEmailPort,
                loadClientApplicationByClientIdPort,
                passwordHashingPort,
                jwtTokenProviderPort,
                refreshTokenStorePort,
                publishAuthEventPort,
                props
        );
    }

    @Test
    void constructor_shouldRejectNulls() {
        JwtProperties props = mock(JwtProperties.class);

        assertEquals(
                "loadAuthUserByEmailPort must not be null",
                assertThrows(NullPointerException.class, () -> new AuthService(
                        null,
                        loadClientApplicationByClientIdPort,
                        passwordHashingPort,
                        jwtTokenProviderPort,
                        refreshTokenStorePort,
                        publishAuthEventPort,
                        props
                )).getMessage()
        );

        assertEquals(
                "loadClientApplicationByClientIdPort must not be null",
                assertThrows(NullPointerException.class, () -> new AuthService(
                        loadAuthUserByEmailPort,
                        null,
                        passwordHashingPort,
                        jwtTokenProviderPort,
                        refreshTokenStorePort,
                        publishAuthEventPort,
                        props
                )).getMessage()
        );

        assertEquals(
                "passwordHashingPort must not be null",
                assertThrows(NullPointerException.class, () -> new AuthService(
                        loadAuthUserByEmailPort,
                        loadClientApplicationByClientIdPort,
                        null,
                        jwtTokenProviderPort,
                        refreshTokenStorePort,
                        publishAuthEventPort,
                        props
                )).getMessage()
        );

        assertEquals(
                "jwtTokenProviderPort must not be null",
                assertThrows(NullPointerException.class, () -> new AuthService(
                        loadAuthUserByEmailPort,
                        loadClientApplicationByClientIdPort,
                        passwordHashingPort,
                        null,
                        refreshTokenStorePort,
                        publishAuthEventPort,
                        props
                )).getMessage()
        );

        assertEquals(
                "refreshTokenStorePort must not be null",
                assertThrows(NullPointerException.class, () -> new AuthService(
                        loadAuthUserByEmailPort,
                        loadClientApplicationByClientIdPort,
                        passwordHashingPort,
                        jwtTokenProviderPort,
                        null,
                        publishAuthEventPort,
                        props
                )).getMessage()
        );

        assertEquals(
                "publishAuthEventPort must not be null",
                assertThrows(NullPointerException.class, () -> new AuthService(
                        loadAuthUserByEmailPort,
                        loadClientApplicationByClientIdPort,
                        passwordHashingPort,
                        jwtTokenProviderPort,
                        refreshTokenStorePort,
                        null,
                        props
                )).getMessage()
        );

        assertEquals(
                "jwtProperties must not be null",
                assertThrows(NullPointerException.class, () -> new AuthService(
                        loadAuthUserByEmailPort,
                        loadClientApplicationByClientIdPort,
                        passwordHashingPort,
                        jwtTokenProviderPort,
                        refreshTokenStorePort,
                        publishAuthEventPort,
                        null
                )).getMessage()
        );
    }

    @Test
    void authenticate_shouldRejectNullCommand() {
        AuthService s = service(10, 20);
        assertEquals("command must not be null", assertThrows(NullPointerException.class, () -> s.authenticate(null)).getMessage());
        verifyNoInteractions(loadAuthUserByEmailPort, loadClientApplicationByClientIdPort, passwordHashingPort, jwtTokenProviderPort, refreshTokenStorePort, publishAuthEventPort);
    }

    @Test
    void authenticate_shouldThrowClientNotFound_whenClientMissing() {
        AuthService s = service(10, 20);

        AuthenticateUserUseCase.AuthenticationCommand cmd = mock(AuthenticateUserUseCase.AuthenticationCommand.class);
        when(cmd.clientId()).thenReturn("c1");

        when(loadClientApplicationByClientIdPort.loadByClientId("c1")).thenReturn(Optional.empty());

        AuthException ex = assertThrows(AuthException.class, () -> s.authenticate(cmd));
        assertEquals(AuthErrorCode.CLIENT_NOT_FOUND, ex.getErrorCode());

        verify(loadClientApplicationByClientIdPort).loadByClientId("c1");
        verifyNoMoreInteractions(loadClientApplicationByClientIdPort);
        verifyNoInteractions(loadAuthUserByEmailPort, passwordHashingPort, jwtTokenProviderPort, refreshTokenStorePort, publishAuthEventPort);
    }

    @Test
    void authenticate_shouldRejectClientForPasswordGrant_inactive_notSupported_badType() {
        AuthenticateUserUseCase.AuthenticationCommand cmd = mock(AuthenticateUserUseCase.AuthenticationCommand.class);
        when(cmd.clientId()).thenReturn("c1");

        AuthService s1 = service(10, 20);

        ClientApplication inactive = mock(ClientApplication.class);
        when(inactive.clientId()).thenReturn("c1");
        when(inactive.isActive()).thenReturn(false);

        when(loadClientApplicationByClientIdPort.loadByClientId("c1")).thenReturn(Optional.of(inactive));

        AuthException ex1 = assertThrows(AuthException.class, () -> s1.authenticate(cmd));
        assertEquals(AuthErrorCode.CLIENT_INACTIVE, ex1.getErrorCode());

        clearInvocations(loadClientApplicationByClientIdPort);

        AuthService s2 = service(10, 20);

        ClientApplication noPasswordGrant = mock(ClientApplication.class);
        when(noPasswordGrant.clientId()).thenReturn("c1");
        when(noPasswordGrant.isActive()).thenReturn(true);
        when(noPasswordGrant.supportsGrant(GrantType.PASSWORD)).thenReturn(false);

        when(loadClientApplicationByClientIdPort.loadByClientId("c1")).thenReturn(Optional.of(noPasswordGrant));

        AuthException ex2 = assertThrows(AuthException.class, () -> s2.authenticate(cmd));
        assertEquals(AuthErrorCode.CLIENT_NOT_ALLOWED_FOR_GRANT_TYPE, ex2.getErrorCode());

        clearInvocations(loadClientApplicationByClientIdPort);

        AuthService s3 = service(10, 20);

        ClientApplication badType = mock(ClientApplication.class);
        when(badType.clientId()).thenReturn("c1");
        when(badType.isActive()).thenReturn(true);
        when(badType.supportsGrant(GrantType.PASSWORD)).thenReturn(true);
        when(badType.clientType()).thenReturn(ClientType.PUBLIC);

        when(loadClientApplicationByClientIdPort.loadByClientId("c1")).thenReturn(Optional.of(badType));

        AuthException ex3 = assertThrows(AuthException.class, () -> s3.authenticate(cmd));
        assertEquals(AuthErrorCode.CLIENT_NOT_ALLOWED_FOR_GRANT_TYPE, ex3.getErrorCode());

        verifyNoInteractions(loadAuthUserByEmailPort, passwordHashingPort, jwtTokenProviderPort, refreshTokenStorePort, publishAuthEventPort);
    }

    @Test
    void authenticate_shouldThrowInvalidCredentials_whenUserMissing_orPasswordMismatch() {
        AuthenticateUserUseCase.AuthenticationCommand cmd = mock(AuthenticateUserUseCase.AuthenticationCommand.class);
        when(cmd.clientId()).thenReturn("c1");
        when(cmd.username()).thenReturn("u@e.com");
        when(cmd.password()).thenReturn("p");

        ClientApplication client = mock(ClientApplication.class);
        when(client.clientId()).thenReturn("c1");
        when(client.isActive()).thenReturn(true);
        when(client.supportsGrant(GrantType.PASSWORD)).thenReturn(true);
        when(client.clientType()).thenReturn(ClientType.CONFIDENTIAL);

        when(loadClientApplicationByClientIdPort.loadByClientId("c1")).thenReturn(Optional.of(client));

        AuthService s1 = service(10, 20);

        when(loadAuthUserByEmailPort.loadByEmail(any(EmailAddress.class))).thenReturn(Optional.empty());

        AuthException ex1 = assertThrows(AuthException.class, () -> s1.authenticate(cmd));
        assertEquals(AuthErrorCode.INVALID_CREDENTIALS, ex1.getErrorCode());

        clearInvocations(loadAuthUserByEmailPort);

        AuthService s2 = service(10, 20);

        AuthUser user = mock(AuthUser.class);
        PasswordHash ph = mock(PasswordHash.class);
        when(user.passwordHash()).thenReturn(ph);

        when(loadAuthUserByEmailPort.loadByEmail(any(EmailAddress.class))).thenReturn(Optional.of(user));
        when(passwordHashingPort.matches("p", ph)).thenReturn(false);

        doNothing().when(user).registerFailedLogin(5);
        when(user.failedLoginAttempts()).thenReturn(5);

        AuthException ex2 = assertThrows(AuthException.class, () -> s2.authenticate(cmd));
        assertEquals(AuthErrorCode.INVALID_CREDENTIALS, ex2.getErrorCode());

        verify(user).registerFailedLogin(5);
    }

    @Test
    void authenticate_shouldNotIncludeScopeClaim_whenScopeBlank() {
        AuthService s = service(1, 2);

        AuthenticateUserUseCase.AuthenticationCommand cmd = mock(AuthenticateUserUseCase.AuthenticationCommand.class);
        when(cmd.clientId()).thenReturn("c1");
        when(cmd.username()).thenReturn("u@e.com");
        when(cmd.password()).thenReturn("p");
        when(cmd.scope()).thenReturn("   ");
        when(cmd.ipAddress()).thenReturn("2.2.2.2");

        ClientApplication client = mock(ClientApplication.class);
        when(client.clientId()).thenReturn("c1");
        when(client.isActive()).thenReturn(true);
        when(client.supportsGrant(GrantType.PASSWORD)).thenReturn(true);
        when(client.clientType()).thenReturn(ClientType.CONFIDENTIAL);

        UUID uid = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        AuthUserId userIdVo = mock(AuthUserId.class);
        when(userIdVo.value()).thenReturn(uid);

        EmailAddress email = mock(EmailAddress.class);
        when(email.value()).thenReturn("u@e.com");

        AuthUser user = mock(AuthUser.class);
        PasswordHash ph = mock(PasswordHash.class);
        when(user.id()).thenReturn(userIdVo);
        when(user.email()).thenReturn(email);
        when(user.passwordHash()).thenReturn(ph);
        when(user.fullName()).thenReturn("User Name");

        when(loadClientApplicationByClientIdPort.loadByClientId("c1")).thenReturn(Optional.of(client));
        when(loadAuthUserByEmailPort.loadByEmail(any(EmailAddress.class))).thenReturn(Optional.of(user));
        when(passwordHashingPort.matches("p", ph)).thenReturn(true);

        JwtToken access = mock(JwtToken.class);

        JwtToken refresh = mock(JwtToken.class);
        when(refresh.value()).thenReturn("refresh.jwt");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> accessClaimsCaptor =
                (ArgumentCaptor<Map<String, Object>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(Map.class);

        when(jwtTokenProviderPort.generateAccessToken(eq(uid.toString()), accessClaimsCaptor.capture(), eq(1L)))
                .thenReturn(access);
        when(jwtTokenProviderPort.generateRefreshToken(eq(uid.toString()), anyMap(), eq(2L)))
                .thenReturn(refresh);

        AuthenticateUserUseCase.AuthenticationResult res = s.authenticate(cmd);

        assertNotNull(res);
        assertFalse(accessClaimsCaptor.getValue().containsKey("scope"));

        verify(refreshTokenStorePort).save(any(RefreshToken.class));
        verify(publishAuthEventPort).publish(any(UserAuthenticatedEvent.class));
    }

    @Test
    void refresh_shouldRejectNullCommand() {
        AuthService s = service(10, 20);
        assertEquals("command must not be null", assertThrows(NullPointerException.class, () -> s.refresh(null)).getMessage());
        verifyNoInteractions(refreshTokenStorePort, jwtTokenProviderPort, loadClientApplicationByClientIdPort);
    }

    @Test
    void refresh_shouldThrowTokenNotFound_whenMissing() {
        AuthService s = service(10, 20);

        RefreshTokenUseCase.RefreshTokenCommand cmd = mock(RefreshTokenUseCase.RefreshTokenCommand.class);
        when(cmd.clientId()).thenReturn("c1");
        when(cmd.refreshToken()).thenReturn("r1");

        when(refreshTokenStorePort.findByTokenValue("r1")).thenReturn(Optional.empty());

        AuthException ex = assertThrows(AuthException.class, () -> s.refresh(cmd));
        assertEquals(AuthErrorCode.TOKEN_NOT_FOUND, ex.getErrorCode());

        verify(refreshTokenStorePort).findByTokenValue("r1");
        verifyNoMoreInteractions(refreshTokenStorePort);
        verifyNoInteractions(jwtTokenProviderPort, loadClientApplicationByClientIdPort);
    }

    @Test
    void refresh_shouldThrowTokenExpired_whenStoredInactive() {
        AuthService s = service(10, 20);

        RefreshTokenUseCase.RefreshTokenCommand cmd = mock(RefreshTokenUseCase.RefreshTokenCommand.class);
        when(cmd.clientId()).thenReturn("c1");
        when(cmd.refreshToken()).thenReturn("r1");

        RefreshToken stored = mock(RefreshToken.class);
        when(stored.isActive()).thenReturn(false);

        when(refreshTokenStorePort.findByTokenValue("r1")).thenReturn(Optional.of(stored));

        AuthException ex = assertThrows(AuthException.class, () -> s.refresh(cmd));
        assertEquals(AuthErrorCode.TOKEN_EXPIRED, ex.getErrorCode());

        verify(refreshTokenStorePort).findByTokenValue("r1");
        verifyNoMoreInteractions(refreshTokenStorePort);
        verifyNoInteractions(jwtTokenProviderPort, loadClientApplicationByClientIdPort);
    }

    @Test
    void refresh_shouldThrowTokenTypeNotSupported_whenTypMissing() {
        AuthService s = service(10, 20);

        RefreshTokenUseCase.RefreshTokenCommand cmd = mock(RefreshTokenUseCase.RefreshTokenCommand.class);
        when(cmd.clientId()).thenReturn("c1");
        when(cmd.refreshToken()).thenReturn("old.refresh");

        RefreshToken stored = mock(RefreshToken.class);
        when(stored.isActive()).thenReturn(true);
        when(stored.tokenValue()).thenReturn("old.refresh");

        when(refreshTokenStorePort.findByTokenValue("old.refresh")).thenReturn(Optional.of(stored));
        when(jwtTokenProviderPort.parseClaims("old.refresh")).thenReturn(Map.of());

        AuthException ex = assertThrows(AuthException.class, () -> s.refresh(cmd));
        assertEquals(AuthErrorCode.TOKEN_TYPE_NOT_SUPPORTED_FOR_REVOCATION, ex.getErrorCode());
    }

    @Test
    void refresh_shouldThrowTokenTypeNotSupported_whenTypIsNotRefresh() {
        AuthService s = service(10, 20);

        RefreshTokenUseCase.RefreshTokenCommand cmd = mock(RefreshTokenUseCase.RefreshTokenCommand.class);
        when(cmd.clientId()).thenReturn("c1");
        when(cmd.refreshToken()).thenReturn("old.refresh");

        RefreshToken stored = mock(RefreshToken.class);
        when(stored.isActive()).thenReturn(true);
        when(stored.tokenValue()).thenReturn("old.refresh");

        when(refreshTokenStorePort.findByTokenValue("old.refresh")).thenReturn(Optional.of(stored));
        when(jwtTokenProviderPort.parseClaims("old.refresh")).thenReturn(Map.of("typ", TokenType.ACCESS.name()));

        AuthException ex = assertThrows(AuthException.class, () -> s.refresh(cmd));
        assertEquals(AuthErrorCode.TOKEN_TYPE_NOT_SUPPORTED_FOR_REVOCATION, ex.getErrorCode());
    }

    @Test
    void refresh_shouldThrowInvalidTokenSignature_whenUserIdMissing() {
        AuthService s = service(10, 20);

        RefreshTokenUseCase.RefreshTokenCommand cmd = mock(RefreshTokenUseCase.RefreshTokenCommand.class);
        when(cmd.clientId()).thenReturn("c1");
        when(cmd.refreshToken()).thenReturn("old.refresh");

        RefreshToken stored = mock(RefreshToken.class);
        when(stored.isActive()).thenReturn(true);
        when(stored.tokenValue()).thenReturn("old.refresh");

        when(refreshTokenStorePort.findByTokenValue("old.refresh")).thenReturn(Optional.of(stored));
        when(jwtTokenProviderPort.parseClaims("old.refresh")).thenReturn(Map.of("typ", TokenType.REFRESH.name(), "client_id", "c1"));

        AuthException ex = assertThrows(AuthException.class, () -> s.refresh(cmd));
        assertEquals(AuthErrorCode.INVALID_TOKEN_SIGNATURE, ex.getErrorCode());
    }

    @Test
    void refresh_shouldThrowInvalidTokenSignature_whenClientIdMissing() {
        AuthService s = service(10, 20);

        RefreshTokenUseCase.RefreshTokenCommand cmd = mock(RefreshTokenUseCase.RefreshTokenCommand.class);
        when(cmd.clientId()).thenReturn("c1");
        when(cmd.refreshToken()).thenReturn("old.refresh");

        RefreshToken stored = mock(RefreshToken.class);
        when(stored.isActive()).thenReturn(true);
        when(stored.tokenValue()).thenReturn("old.refresh");

        when(refreshTokenStorePort.findByTokenValue("old.refresh")).thenReturn(Optional.of(stored));
        when(jwtTokenProviderPort.parseClaims("old.refresh")).thenReturn(Map.of("typ", TokenType.REFRESH.name(), "sub", "u1"));

        AuthException ex = assertThrows(AuthException.class, () -> s.refresh(cmd));
        assertEquals(AuthErrorCode.INVALID_TOKEN_SIGNATURE, ex.getErrorCode());
    }

    @Test
    void refresh_shouldThrowTokenOwnerMismatch_whenClaimsClientIdDiffersFromCommand() {
        AuthService s = service(10, 20);

        RefreshTokenUseCase.RefreshTokenCommand cmd = mock(RefreshTokenUseCase.RefreshTokenCommand.class);
        when(cmd.clientId()).thenReturn("c1");
        when(cmd.refreshToken()).thenReturn("old.refresh");

        RefreshToken stored = mock(RefreshToken.class);
        when(stored.isActive()).thenReturn(true);
        when(stored.tokenValue()).thenReturn("old.refresh");
        when(stored.clientId()).thenReturn("c1");

        when(refreshTokenStorePort.findByTokenValue("old.refresh")).thenReturn(Optional.of(stored));
        when(jwtTokenProviderPort.parseClaims("old.refresh")).thenReturn(
                Map.of("typ", TokenType.REFRESH.name(), "sub", "u1", "client_id", "c2")
        );

        AuthException ex = assertThrows(AuthException.class, () -> s.refresh(cmd));
        assertEquals(AuthErrorCode.TOKEN_OWNER_MISMATCH, ex.getErrorCode());
    }

    @Test
    void refresh_shouldThrowTokenOwnerMismatch_whenStoredClientIdDiffersFromCommand() {
        AuthService s = service(10, 20);

        RefreshTokenUseCase.RefreshTokenCommand cmd = mock(RefreshTokenUseCase.RefreshTokenCommand.class);
        when(cmd.clientId()).thenReturn("c1");
        when(cmd.refreshToken()).thenReturn("old.refresh");

        RefreshToken stored = mock(RefreshToken.class);
        when(stored.isActive()).thenReturn(true);
        when(stored.tokenValue()).thenReturn("old.refresh");
        when(stored.clientId()).thenReturn("c2");

        when(refreshTokenStorePort.findByTokenValue("old.refresh")).thenReturn(Optional.of(stored));
        when(jwtTokenProviderPort.parseClaims("old.refresh")).thenReturn(
                Map.of("typ", TokenType.REFRESH.name(), "sub", "u1", "client_id", "c1")
        );

        AuthException ex = assertThrows(AuthException.class, () -> s.refresh(cmd));
        assertEquals(AuthErrorCode.TOKEN_OWNER_MISMATCH, ex.getErrorCode());
    }

    @Test
    void refresh_shouldThrowClientNotFound_whenClientMissing() {
        AuthService s = service(10, 20);

        RefreshTokenUseCase.RefreshTokenCommand cmd = mock(RefreshTokenUseCase.RefreshTokenCommand.class);
        when(cmd.clientId()).thenReturn("c1");
        when(cmd.refreshToken()).thenReturn("old.refresh");

        RefreshToken stored = mock(RefreshToken.class);
        when(stored.isActive()).thenReturn(true);
        when(stored.tokenValue()).thenReturn("old.refresh");
        when(stored.clientId()).thenReturn("c1");

        when(refreshTokenStorePort.findByTokenValue("old.refresh")).thenReturn(Optional.of(stored));
        when(jwtTokenProviderPort.parseClaims("old.refresh")).thenReturn(
                Map.of("typ", TokenType.REFRESH.name(), "sub", "u1", "client_id", "c1")
        );

        when(loadClientApplicationByClientIdPort.loadByClientId("c1")).thenReturn(Optional.empty());

        AuthException ex = assertThrows(AuthException.class, () -> s.refresh(cmd));
        assertEquals(AuthErrorCode.CLIENT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void refresh_shouldThrowClientInactive_whenClientInactive() {
        AuthService s = service(10, 20);

        RefreshTokenUseCase.RefreshTokenCommand cmd = mock(RefreshTokenUseCase.RefreshTokenCommand.class);
        when(cmd.clientId()).thenReturn("c1");
        when(cmd.refreshToken()).thenReturn("old.refresh");

        RefreshToken stored = mock(RefreshToken.class);
        when(stored.isActive()).thenReturn(true);
        when(stored.tokenValue()).thenReturn("old.refresh");
        when(stored.clientId()).thenReturn("c1");

        when(refreshTokenStorePort.findByTokenValue("old.refresh")).thenReturn(Optional.of(stored));
        when(jwtTokenProviderPort.parseClaims("old.refresh")).thenReturn(
                Map.of("typ", TokenType.REFRESH.name(), "sub", "u1", "client_id", "c1")
        );

        ClientApplication inactive = mock(ClientApplication.class);
        when(inactive.isActive()).thenReturn(false);

        when(loadClientApplicationByClientIdPort.loadByClientId("c1")).thenReturn(Optional.of(inactive));

        AuthException ex = assertThrows(AuthException.class, () -> s.refresh(cmd));
        assertEquals(AuthErrorCode.CLIENT_INACTIVE, ex.getErrorCode());
    }

    @Test
    void refresh_shouldThrowClientNotAllowedForGrant_whenClientDoesNotSupportRefreshGrant() {
        AuthService s = service(10, 20);

        RefreshTokenUseCase.RefreshTokenCommand cmd = mock(RefreshTokenUseCase.RefreshTokenCommand.class);
        when(cmd.clientId()).thenReturn("c1");
        when(cmd.refreshToken()).thenReturn("old.refresh");

        RefreshToken stored = mock(RefreshToken.class);
        when(stored.isActive()).thenReturn(true);
        when(stored.tokenValue()).thenReturn("old.refresh");
        when(stored.clientId()).thenReturn("c1");

        when(refreshTokenStorePort.findByTokenValue("old.refresh")).thenReturn(Optional.of(stored));
        when(jwtTokenProviderPort.parseClaims("old.refresh")).thenReturn(
                Map.of("typ", TokenType.REFRESH.name(), "sub", "u1", "client_id", "c1")
        );

        ClientApplication noGrant = mock(ClientApplication.class);
        when(noGrant.isActive()).thenReturn(true);
        when(noGrant.supportsGrant(GrantType.REFRESH_TOKEN)).thenReturn(false);

        when(loadClientApplicationByClientIdPort.loadByClientId("c1")).thenReturn(Optional.of(noGrant));

        AuthException ex = assertThrows(AuthException.class, () -> s.refresh(cmd));
        assertEquals(AuthErrorCode.CLIENT_NOT_ALLOWED_FOR_GRANT_TYPE, ex.getErrorCode());
    }

    @Test
    void refresh_shouldRotateTokens_saveNew_revokeOld_andReturnResult() {
        AuthService s = service(10, 20);

        RefreshTokenUseCase.RefreshTokenCommand cmd = mock(RefreshTokenUseCase.RefreshTokenCommand.class);
        when(cmd.clientId()).thenReturn("c1");
        when(cmd.refreshToken()).thenReturn("old.refresh");

        AuthUserId uid = mock(AuthUserId.class);
        when(uid.value()).thenReturn(UUID.fromString("99999999-8888-7777-6666-555555555555"));

        RefreshToken stored = mock(RefreshToken.class);
        when(stored.isActive()).thenReturn(true);
        when(stored.tokenValue()).thenReturn("old.refresh");
        when(stored.clientId()).thenReturn("c1");
        when(stored.userId()).thenReturn(uid);

        when(refreshTokenStorePort.findByTokenValue("old.refresh")).thenReturn(Optional.of(stored));

        Map<String, Object> parsedClaims = Map.of(
                "typ", TokenType.REFRESH.name(),
                "sub", "user-sub",
                "client_id", "c1"
        );
        when(jwtTokenProviderPort.parseClaims("old.refresh")).thenReturn(parsedClaims);

        ClientApplication okClient = mock(ClientApplication.class);
        when(okClient.isActive()).thenReturn(true);
        when(okClient.supportsGrant(GrantType.REFRESH_TOKEN)).thenReturn(true);

        when(loadClientApplicationByClientIdPort.loadByClientId("c1")).thenReturn(Optional.of(okClient));

        JwtToken newAccess = mock(JwtToken.class);
        when(newAccess.value()).thenReturn("new.access");

        JwtToken newRefresh = mock(JwtToken.class);
        when(newRefresh.value()).thenReturn("new.refresh");

        when(jwtTokenProviderPort.generateAccessToken(eq("user-sub"), eq(parsedClaims), eq(10L))).thenReturn(newAccess);
        when(jwtTokenProviderPort.generateRefreshToken(eq("user-sub"), eq(Map.of("client_id", "c1")), eq(20L))).thenReturn(newRefresh);

        RefreshTokenUseCase.RefreshTokenResult res = s.refresh(cmd);

        assertNotNull(res);
        assertEquals("new.access", res.accessToken());
        assertEquals("new.refresh", res.refreshToken());
        assertEquals(10L, res.expiresInSeconds());

        verify(refreshTokenStorePort).save(any(RefreshToken.class));
        verify(refreshTokenStorePort).revoke("old.refresh");
    }

    @Test
    void maskToken_shouldCoverAllBranches() throws Exception {
        AuthService s = service(1, 1);

        assertEquals("***", invokeMaskToken(s, null));
        assertEquals("***", invokeMaskToken(s, "   "));
        assertEquals("***", invokeMaskToken(s, "123456789012"));
        assertEquals("123456789012...", invokeMaskToken(s, "1234567890123"));
    }

    @Test
    void authenticate_shouldAllowPasswordGrant_whenClientTypeIsSpa_coverTypeNotSpaFalseBranch() {
        AuthService s = service(10, 20);

        AuthenticateUserUseCase.AuthenticationCommand cmd = mock(AuthenticateUserUseCase.AuthenticationCommand.class);
        when(cmd.clientId()).thenReturn("c1");
        when(cmd.username()).thenReturn("u@e.com");
        when(cmd.password()).thenReturn("p");
        when(cmd.scope()).thenReturn(null);
        when(cmd.ipAddress()).thenReturn("1.1.1.1");

        ClientApplication client = mock(ClientApplication.class);
        when(client.clientId()).thenReturn("c1");
        when(client.isActive()).thenReturn(true);
        when(client.supportsGrant(GrantType.PASSWORD)).thenReturn(true);
        when(client.clientType()).thenReturn(ClientType.SPA);

        UUID uid = UUID.fromString("11111111-2222-3333-4444-555555555555");
        AuthUserId userIdVo = mock(AuthUserId.class);
        when(userIdVo.value()).thenReturn(uid);

        EmailAddress email = mock(EmailAddress.class);
        when(email.value()).thenReturn("u@e.com");

        AuthUser user = mock(AuthUser.class);
        PasswordHash ph = mock(PasswordHash.class);
        when(user.id()).thenReturn(userIdVo);
        when(user.email()).thenReturn(email);
        when(user.passwordHash()).thenReturn(ph);
        when(user.fullName()).thenReturn("User Name");

        when(loadClientApplicationByClientIdPort.loadByClientId("c1")).thenReturn(Optional.of(client));
        when(loadAuthUserByEmailPort.loadByEmail(any(EmailAddress.class))).thenReturn(Optional.of(user));
        when(passwordHashingPort.matches("p", ph)).thenReturn(true);

        JwtToken access = mock(JwtToken.class);
        JwtToken refresh = mock(JwtToken.class);
        when(refresh.value()).thenReturn("refresh.jwt");

        when(jwtTokenProviderPort.generateAccessToken(eq(uid.toString()), anyMap(), eq(10L))).thenReturn(access);
        when(jwtTokenProviderPort.generateRefreshToken(eq(uid.toString()), anyMap(), eq(20L))).thenReturn(refresh);

        AuthenticateUserUseCase.AuthenticationResult res = s.authenticate(cmd);

        assertNotNull(res);
        assertSame(access, res.accessToken());
        assertEquals("refresh.jwt", res.refreshToken());

        verify(loadClientApplicationByClientIdPort).loadByClientId("c1");
        verify(loadAuthUserByEmailPort).loadByEmail(any(EmailAddress.class));
        verify(passwordHashingPort).matches("p", ph);
        verify(refreshTokenStorePort).save(any(RefreshToken.class));
        verify(publishAuthEventPort).publish(any(UserAuthenticatedEvent.class));
    }

    @Test
    void authenticate_shouldIncludeScopeClaim_whenScopeNonBlank() {
        AuthService s = service(10, 20);

        AuthenticateUserUseCase.AuthenticationCommand cmd = mock(AuthenticateUserUseCase.AuthenticationCommand.class);
        when(cmd.clientId()).thenReturn("c1");
        when(cmd.username()).thenReturn("u@e.com");
        when(cmd.password()).thenReturn("p");
        when(cmd.scope()).thenReturn("read write");
        when(cmd.ipAddress()).thenReturn("1.1.1.1");

        ClientApplication client = mock(ClientApplication.class);
        when(client.clientId()).thenReturn("c1");
        when(client.isActive()).thenReturn(true);
        when(client.supportsGrant(GrantType.PASSWORD)).thenReturn(true);
        when(client.clientType()).thenReturn(ClientType.CONFIDENTIAL);

        UUID uid = UUID.fromString("11111111-2222-3333-4444-555555555555");
        AuthUserId userIdVo = mock(AuthUserId.class);
        when(userIdVo.value()).thenReturn(uid);

        EmailAddress email = mock(EmailAddress.class);
        when(email.value()).thenReturn("u@e.com");

        AuthUser user = mock(AuthUser.class);
        PasswordHash ph = mock(PasswordHash.class);
        when(user.id()).thenReturn(userIdVo);
        when(user.email()).thenReturn(email);
        when(user.passwordHash()).thenReturn(ph);
        when(user.fullName()).thenReturn("User Name");

        when(loadClientApplicationByClientIdPort.loadByClientId("c1")).thenReturn(Optional.of(client));
        when(loadAuthUserByEmailPort.loadByEmail(any(EmailAddress.class))).thenReturn(Optional.of(user));
        when(passwordHashingPort.matches("p", ph)).thenReturn(true);

        JwtToken access = mock(JwtToken.class);
        JwtToken refresh = mock(JwtToken.class);
        when(refresh.value()).thenReturn("refresh.jwt");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> accessClaimsCaptor =
                (ArgumentCaptor<Map<String, Object>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(Map.class);

        when(jwtTokenProviderPort.generateAccessToken(eq(uid.toString()), accessClaimsCaptor.capture(), eq(10L)))
                .thenReturn(access);
        when(jwtTokenProviderPort.generateRefreshToken(eq(uid.toString()), anyMap(), eq(20L)))
                .thenReturn(refresh);

        AuthenticateUserUseCase.AuthenticationResult res = s.authenticate(cmd);

        assertNotNull(res);
        assertEquals("read write", accessClaimsCaptor.getValue().get("scope"));
    }

    @Test
    void authenticate_shouldNotIncludeScopeClaim_whenScopeIsNull() {
        AuthService s = service(10, 20);

        AuthenticateUserUseCase.AuthenticationCommand cmd = mock(AuthenticateUserUseCase.AuthenticationCommand.class);
        when(cmd.clientId()).thenReturn("c1");
        when(cmd.username()).thenReturn("u@e.com");
        when(cmd.password()).thenReturn("p");
        when(cmd.scope()).thenReturn(null);
        when(cmd.ipAddress()).thenReturn("1.1.1.1");

        ClientApplication client = mock(ClientApplication.class);
        when(client.clientId()).thenReturn("c1");
        when(client.isActive()).thenReturn(true);
        when(client.supportsGrant(GrantType.PASSWORD)).thenReturn(true);
        when(client.clientType()).thenReturn(ClientType.CONFIDENTIAL);

        UUID uid = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        AuthUserId userIdVo = mock(AuthUserId.class);
        when(userIdVo.value()).thenReturn(uid);

        EmailAddress email = mock(EmailAddress.class);
        when(email.value()).thenReturn("u@e.com");

        AuthUser user = mock(AuthUser.class);
        PasswordHash ph = mock(PasswordHash.class);
        when(user.id()).thenReturn(userIdVo);
        when(user.email()).thenReturn(email);
        when(user.passwordHash()).thenReturn(ph);
        when(user.fullName()).thenReturn("User Name");

        when(loadClientApplicationByClientIdPort.loadByClientId("c1")).thenReturn(Optional.of(client));
        when(loadAuthUserByEmailPort.loadByEmail(any(EmailAddress.class))).thenReturn(Optional.of(user));
        when(passwordHashingPort.matches("p", ph)).thenReturn(true);

        JwtToken access = mock(JwtToken.class);
        JwtToken refresh = mock(JwtToken.class);
        when(refresh.value()).thenReturn("refresh.jwt");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> accessClaimsCaptor =
                (ArgumentCaptor<Map<String, Object>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(Map.class);

        when(jwtTokenProviderPort.generateAccessToken(eq(uid.toString()), accessClaimsCaptor.capture(), eq(10L)))
                .thenReturn(access);
        when(jwtTokenProviderPort.generateRefreshToken(eq(uid.toString()), anyMap(), eq(20L)))
                .thenReturn(refresh);

        AuthenticateUserUseCase.AuthenticationResult res = s.authenticate(cmd);

        assertNotNull(res);
        assertFalse(accessClaimsCaptor.getValue().containsKey("scope"));
    }

    @Test
    void authenticate_shouldNotIncludeScopeClaim_whenScopeIsBlank() {
        AuthService s = service(10, 20);

        AuthenticateUserUseCase.AuthenticationCommand cmd = mock(AuthenticateUserUseCase.AuthenticationCommand.class);
        when(cmd.clientId()).thenReturn("c1");
        when(cmd.username()).thenReturn("u@e.com");
        when(cmd.password()).thenReturn("p");
        when(cmd.scope()).thenReturn("   ");
        when(cmd.ipAddress()).thenReturn("1.1.1.1");

        ClientApplication client = mock(ClientApplication.class);
        when(client.clientId()).thenReturn("c1");
        when(client.isActive()).thenReturn(true);
        when(client.supportsGrant(GrantType.PASSWORD)).thenReturn(true);
        when(client.clientType()).thenReturn(ClientType.CONFIDENTIAL);

        UUID uid = UUID.fromString("99999999-8888-7777-6666-555555555555");
        AuthUserId userIdVo = mock(AuthUserId.class);
        when(userIdVo.value()).thenReturn(uid);

        EmailAddress email = mock(EmailAddress.class);
        when(email.value()).thenReturn("u@e.com");

        AuthUser user = mock(AuthUser.class);
        PasswordHash ph = mock(PasswordHash.class);
        when(user.id()).thenReturn(userIdVo);
        when(user.email()).thenReturn(email);
        when(user.passwordHash()).thenReturn(ph);
        when(user.fullName()).thenReturn("User Name");

        when(loadClientApplicationByClientIdPort.loadByClientId("c1")).thenReturn(Optional.of(client));
        when(loadAuthUserByEmailPort.loadByEmail(any(EmailAddress.class))).thenReturn(Optional.of(user));
        when(passwordHashingPort.matches("p", ph)).thenReturn(true);

        JwtToken access = mock(JwtToken.class);
        JwtToken refresh = mock(JwtToken.class);
        when(refresh.value()).thenReturn("refresh.jwt");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> accessClaimsCaptor =
                (ArgumentCaptor<Map<String, Object>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(Map.class);

        when(jwtTokenProviderPort.generateAccessToken(eq(uid.toString()), accessClaimsCaptor.capture(), eq(10L)))
                .thenReturn(access);
        when(jwtTokenProviderPort.generateRefreshToken(eq(uid.toString()), anyMap(), eq(20L)))
                .thenReturn(refresh);

        AuthenticateUserUseCase.AuthenticationResult res = s.authenticate(cmd);

        assertNotNull(res);
        assertFalse(accessClaimsCaptor.getValue().containsKey("scope"));
    }


    private static String invokeMaskToken(AuthService service, String token) throws Exception {
        Method m = AuthService.class.getDeclaredMethod("maskToken", String.class);
        m.setAccessible(true);
        return (String) m.invoke(service, token);
    }
}