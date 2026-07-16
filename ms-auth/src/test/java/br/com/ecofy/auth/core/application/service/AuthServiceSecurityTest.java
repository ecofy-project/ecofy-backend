package br.com.ecofy.auth.core.application.service;

import br.com.ecofy.auth.config.JwtProperties;
import br.com.ecofy.auth.core.application.exception.AuthErrorCode;
import br.com.ecofy.auth.core.application.exception.AuthException;
import br.com.ecofy.auth.core.domain.AuthUser;
import br.com.ecofy.auth.core.domain.ClientApplication;
import br.com.ecofy.auth.core.domain.JwtToken;
import br.com.ecofy.auth.core.domain.Permission;
import br.com.ecofy.auth.core.domain.Role;
import br.com.ecofy.auth.core.domain.enums.AuthUserStatus;
import br.com.ecofy.auth.core.domain.enums.ClientType;
import br.com.ecofy.auth.core.domain.enums.GrantType;
import br.com.ecofy.auth.core.domain.valueobject.AuthUserId;
import br.com.ecofy.auth.core.domain.valueobject.EmailAddress;
import br.com.ecofy.auth.core.domain.valueobject.PasswordHash;
import br.com.ecofy.auth.core.port.in.AuthenticateUserUseCase;
import br.com.ecofy.auth.core.port.out.JwtTokenProviderPort;
import br.com.ecofy.auth.core.port.out.LoadAuthUserByEmailPort;
import br.com.ecofy.auth.core.port.out.LoadClientApplicationByClientIdPort;
import br.com.ecofy.auth.core.port.out.PasswordHashingPort;
import br.com.ecofy.auth.core.port.out.PublishAuthEventPort;
import br.com.ecofy.auth.core.port.out.RefreshTokenStorePort;
import br.com.ecofy.auth.core.port.out.SaveAuthUserPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Cobre as regras de SEGURANÇA do login adicionadas no Dia 1-2:
 * - persistência de tentativas falhas e estado de login;
 * - bloqueio de contas pendentes/locked/blocked/deleted/não verificadas;
 * - emissão de claims de roles/permissions no access token.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceSecurityTest {

    @Mock private LoadAuthUserByEmailPort loadAuthUserByEmailPort;
    @Mock private LoadClientApplicationByClientIdPort loadClientApplicationByClientIdPort;
    @Mock private PasswordHashingPort passwordHashingPort;
    @Mock private JwtTokenProviderPort jwtTokenProviderPort;
    @Mock private RefreshTokenStorePort refreshTokenStorePort;
    @Mock private PublishAuthEventPort publishAuthEventPort;
    @Mock private SaveAuthUserPort saveAuthUserPort;

    private AuthService service() {
        JwtProperties props = mock(JwtProperties.class);
        when(props.getAccessTokenTtlSeconds()).thenReturn(900L);
        when(props.getRefreshTokenTtlSeconds()).thenReturn(3600L);
        return new AuthService(
                loadAuthUserByEmailPort,
                loadClientApplicationByClientIdPort,
                passwordHashingPort,
                jwtTokenProviderPort,
                refreshTokenStorePort,
                publishAuthEventPort,
                saveAuthUserPort,
                props
        );
    }

    private AuthenticateUserUseCase.AuthenticationCommand command() {
        AuthenticateUserUseCase.AuthenticationCommand cmd = mock(AuthenticateUserUseCase.AuthenticationCommand.class);
        when(cmd.clientId()).thenReturn("c1");
        when(cmd.username()).thenReturn("u@e.com");
        when(cmd.password()).thenReturn("p");
        return cmd;
    }

    private ClientApplication validClient() {
        ClientApplication client = mock(ClientApplication.class);
        when(client.clientId()).thenReturn("c1");
        when(client.isActive()).thenReturn(true);
        when(client.supportsGrant(GrantType.PASSWORD)).thenReturn(true);
        when(client.clientType()).thenReturn(ClientType.CONFIDENTIAL);
        return client;
    }

    private AuthUser userWith(AuthUserStatus status, boolean emailVerified) {
        AuthUser user = mock(AuthUser.class);
        PasswordHash ph = mock(PasswordHash.class);
        when(user.passwordHash()).thenReturn(ph);
        AuthUserId idVo = mock(AuthUserId.class);
        lenient().when(idVo.value()).thenReturn(UUID.fromString("11111111-2222-3333-4444-555555555555"));
        lenient().when(user.id()).thenReturn(idVo);
        lenient().when(user.status()).thenReturn(status);
        lenient().when(user.isEmailVerified()).thenReturn(emailVerified);
        return user;
    }

    @Test
    void authenticate_shouldPersistFailedLogin_whenPasswordWrong() {
        AuthService s = service();
        AuthenticateUserUseCase.AuthenticationCommand cmd = command();

        ClientApplication client = validClient();
        when(loadClientApplicationByClientIdPort.loadByClientId("c1")).thenReturn(Optional.of(client));
        AuthUser user = userWith(AuthUserStatus.ACTIVE, true);
        when(loadAuthUserByEmailPort.loadByEmail(any(EmailAddress.class))).thenReturn(Optional.of(user));
        when(passwordHashingPort.matches("p", user.passwordHash())).thenReturn(false);

        AuthException ex = assertThrows(AuthException.class, () -> s.authenticate(cmd));
        assertEquals(AuthErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());

        // Regra crítica: tentativa falha registrada E persistida (para o lock funcionar).
        verify(user).registerFailedLogin(5);
        verify(saveAuthUserPort).save(user);
    }

    @Test
    void authenticate_shouldPersistSuccessfulLogin_andResetAttempts_whenOk() {
        AuthService s = service();
        AuthenticateUserUseCase.AuthenticationCommand cmd = command();
        when(cmd.scope()).thenReturn(null);
        when(cmd.ipAddress()).thenReturn("1.1.1.1");

        ClientApplication client = validClient();
        when(loadClientApplicationByClientIdPort.loadByClientId("c1")).thenReturn(Optional.of(client));
        AuthUser user = userWith(AuthUserStatus.ACTIVE, true);
        when(user.email()).thenReturn(new EmailAddress("u@e.com"));
        when(user.fullName()).thenReturn("U E");
        when(loadAuthUserByEmailPort.loadByEmail(any(EmailAddress.class))).thenReturn(Optional.of(user));
        when(passwordHashingPort.matches("p", user.passwordHash())).thenReturn(true);

        JwtToken access = mock(JwtToken.class);
        JwtToken refresh = mock(JwtToken.class);
        when(refresh.value()).thenReturn("refresh.jwt");
        when(jwtTokenProviderPort.generateAccessToken(anyString(), anyMap(), eq(900L))).thenReturn(access);
        when(jwtTokenProviderPort.generateRefreshToken(anyString(), anyMap(), eq(3600L))).thenReturn(refresh);

        s.authenticate(cmd);

        verify(user).registerSuccessfulLogin();
        verify(saveAuthUserPort).save(user);
    }

    @Test
    void authenticate_shouldBlockPendingEmailConfirmation() {
        assertBlocked(AuthUserStatus.PENDING_EMAIL_CONFIRMATION, true, AuthErrorCode.EMAIL_NOT_VERIFIED);
    }

    @Test
    void authenticate_shouldBlockActiveButUnverifiedEmail() {
        assertBlocked(AuthUserStatus.ACTIVE, false, AuthErrorCode.EMAIL_NOT_VERIFIED);
    }

    @Test
    void authenticate_shouldBlockLockedUser() {
        assertBlocked(AuthUserStatus.LOCKED, true, AuthErrorCode.USER_LOCKED);
    }

    @Test
    void authenticate_shouldBlockBlockedUser() {
        assertBlocked(AuthUserStatus.BLOCKED, true, AuthErrorCode.USER_BLOCKED);
    }

    @Test
    void authenticate_shouldBlockDeletedUser() {
        assertBlocked(AuthUserStatus.DELETED, true, AuthErrorCode.USER_BLOCKED);
    }

    private void assertBlocked(AuthUserStatus status, boolean emailVerified, AuthErrorCode expected) {
        AuthService s = service();
        AuthenticateUserUseCase.AuthenticationCommand cmd = command();

        ClientApplication client = validClient();
        when(loadClientApplicationByClientIdPort.loadByClientId("c1")).thenReturn(Optional.of(client));
        AuthUser user = userWith(status, emailVerified);
        when(loadAuthUserByEmailPort.loadByEmail(any(EmailAddress.class))).thenReturn(Optional.of(user));
        when(passwordHashingPort.matches("p", user.passwordHash())).thenReturn(true);

        AuthException ex = assertThrows(AuthException.class, () -> s.authenticate(cmd));
        assertEquals(expected, ex.getErrorCode());

        // Não emite tokens nem marca login bem-sucedido para conta inválida.
        verify(user, never()).registerSuccessfulLogin();
        verifyNoInteractions(jwtTokenProviderPort, refreshTokenStorePort, publishAuthEventPort);
    }

    @Test
    void authenticate_shouldEmitRolesAndPermissionsClaims() {
        AuthService s = service();
        AuthenticateUserUseCase.AuthenticationCommand cmd = command();
        when(cmd.scope()).thenReturn(null);
        when(cmd.ipAddress()).thenReturn("1.1.1.1");

        ClientApplication client = validClient();
        when(loadClientApplicationByClientIdPort.loadByClientId("c1")).thenReturn(Optional.of(client));

        AuthUser user = userWith(AuthUserStatus.ACTIVE, true);
        when(user.email()).thenReturn(new EmailAddress("u@e.com"));
        when(user.fullName()).thenReturn("U E");
        Role admin = new Role("ROLE_ADMIN", null, Set.of(new Permission("auth:user:admin", null, "auth")));
        when(user.roles()).thenReturn(Set.of(admin));
        when(user.directPermissions()).thenReturn(Set.of());
        when(loadAuthUserByEmailPort.loadByEmail(any(EmailAddress.class))).thenReturn(Optional.of(user));
        when(passwordHashingPort.matches("p", user.passwordHash())).thenReturn(true);

        JwtToken access = mock(JwtToken.class);
        JwtToken refresh = mock(JwtToken.class);
        when(refresh.value()).thenReturn("refresh.jwt");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> claimsCaptor =
                (ArgumentCaptor<Map<String, Object>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(Map.class);

        when(jwtTokenProviderPort.generateAccessToken(anyString(), claimsCaptor.capture(), eq(900L))).thenReturn(access);
        when(jwtTokenProviderPort.generateRefreshToken(anyString(), anyMap(), eq(3600L))).thenReturn(refresh);

        s.authenticate(cmd);

        Map<String, Object> claims = claimsCaptor.getValue();
        assertEquals(List.of("ROLE_ADMIN"), claims.get("roles"));
        assertEquals(List.of("auth:user:admin"), claims.get("permissions"));
    }
}
