package br.com.ecofy.auth.core.application.service;

import br.com.ecofy.auth.core.application.exception.AuthErrorCode;
import br.com.ecofy.auth.core.application.exception.AuthException;
import br.com.ecofy.auth.core.domain.ClientApplication;
import br.com.ecofy.auth.core.domain.enums.ClientType;
import br.com.ecofy.auth.core.domain.enums.GrantType;
import br.com.ecofy.auth.core.domain.valueobject.PasswordHash;
import br.com.ecofy.auth.core.port.in.RegisterClientApplicationUseCase;
import br.com.ecofy.auth.core.port.out.PasswordHashingPort;
import br.com.ecofy.auth.core.port.out.SaveClientApplicationPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientApplicationServiceTest {

    @Mock
    private SaveClientApplicationPort saveClientApplicationPort;

    @Mock
    private PasswordHashingPort passwordHashingPort;

    private ClientApplicationService service() {
        return new ClientApplicationService(saveClientApplicationPort, passwordHashingPort);
    }

    @Test
    void constructor_shouldRejectNullDependencies() {
        assertEquals(
                "saveClientApplicationPort must not be null",
                assertThrows(NullPointerException.class, () -> new ClientApplicationService(null, passwordHashingPort)).getMessage()
        );
        assertEquals(
                "passwordHashingPort must not be null",
                assertThrows(NullPointerException.class, () -> new ClientApplicationService(saveClientApplicationPort, null)).getMessage()
        );
    }

    @Test
    void register_shouldRejectNullCommand() {
        ClientApplicationService s = service();
        assertEquals("command must not be null", assertThrows(NullPointerException.class, () -> s.register(null)).getMessage());
        verifyNoInteractions(saveClientApplicationPort, passwordHashingPort);
    }

    @Test
    void register_shouldCreateConfidentialClient_withSecretHash_defaultGrants_andRequireRedirectUris() {
        ClientApplicationService s = service();

        RegisterClientApplicationUseCase.RegisterClientCommand cmd = mock(RegisterClientApplicationUseCase.RegisterClientCommand.class);
        when(cmd.name()).thenReturn("App");
        when(cmd.clientType()).thenReturn(ClientType.CONFIDENTIAL);
        when(cmd.grantTypes()).thenReturn(null);
        when(cmd.redirectUris()).thenReturn(Set.of("https://app/cb"));
        when(cmd.scopes()).thenReturn(Set.of("openid"));
        when(cmd.firstParty()).thenReturn(true);

        PasswordHash ph = mock(PasswordHash.class);
        when(ph.value()).thenReturn("hash-1");
        when(passwordHashingPort.hash(any(String.class))).thenReturn(ph);

        when(saveClientApplicationPort.save(any(ClientApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        ClientApplication saved = s.register(cmd);

        assertNotNull(saved);
        assertEquals(ClientType.CONFIDENTIAL, saved.clientType());
        assertNotNull(saved.clientId());
        assertTrue(saved.clientId().startsWith("eco_"));
        assertNotNull(saved.grantTypes());
        assertTrue(saved.grantTypes().contains(GrantType.AUTHORIZATION_CODE));
        assertTrue(saved.grantTypes().contains(GrantType.REFRESH_TOKEN));
        assertTrue(saved.grantTypes().contains(GrantType.CLIENT_CREDENTIALS));

        verify(passwordHashingPort).hash(any(String.class));
        verify(saveClientApplicationPort).save(any(ClientApplication.class));
    }

    @Test
    void register_shouldCreatePublicClient_withoutSecret_defaultGrants_andRequireRedirectUris() {
        ClientApplicationService s = service();

        RegisterClientApplicationUseCase.RegisterClientCommand cmd = mock(RegisterClientApplicationUseCase.RegisterClientCommand.class);
        when(cmd.name()).thenReturn("Pub");
        when(cmd.clientType()).thenReturn(ClientType.PUBLIC);
        when(cmd.grantTypes()).thenReturn(null);
        when(cmd.redirectUris()).thenReturn(Set.of("https://pub/cb"));
        when(cmd.scopes()).thenReturn(Set.of("openid"));
        when(cmd.firstParty()).thenReturn(false);

        when(saveClientApplicationPort.save(any(ClientApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        ClientApplication saved = s.register(cmd);

        assertNotNull(saved);
        assertEquals(ClientType.PUBLIC, saved.clientType());
        assertNotNull(saved.clientId());
        assertTrue(saved.clientId().startsWith("eco_"));
        assertTrue(saved.grantTypes().contains(GrantType.AUTHORIZATION_CODE));
        assertTrue(saved.grantTypes().contains(GrantType.REFRESH_TOKEN));
        assertFalse(saved.grantTypes().contains(GrantType.CLIENT_CREDENTIALS));

        verifyNoInteractions(passwordHashingPort);
        verify(saveClientApplicationPort).save(any(ClientApplication.class));
    }

    @Test
    void register_shouldCreateM2MClient_withSecretHash_defaultGrants_andNotRequireRedirectUris() {
        ClientApplicationService s = service();

        RegisterClientApplicationUseCase.RegisterClientCommand cmd = mock(RegisterClientApplicationUseCase.RegisterClientCommand.class);
        when(cmd.name()).thenReturn("M2M");
        when(cmd.clientType()).thenReturn(ClientType.MACHINE_TO_MACHINE);
        when(cmd.grantTypes()).thenReturn(null);
        when(cmd.redirectUris()).thenReturn(null);
        when(cmd.scopes()).thenReturn(Set.of("svc"));
        when(cmd.firstParty()).thenReturn(true);

        PasswordHash ph = mock(PasswordHash.class);
        when(ph.value()).thenReturn("hash-2");
        when(passwordHashingPort.hash(any(String.class))).thenReturn(ph);

        when(saveClientApplicationPort.save(any(ClientApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        ClientApplication saved = s.register(cmd);

        assertNotNull(saved);
        assertEquals(ClientType.MACHINE_TO_MACHINE, saved.clientType());
        assertNotNull(saved.clientId());
        assertTrue(saved.clientId().startsWith("eco_"));
        assertTrue(saved.grantTypes().contains(GrantType.CLIENT_CREDENTIALS));
        assertFalse(saved.grantTypes().contains(GrantType.AUTHORIZATION_CODE));

        verify(passwordHashingPort).hash(any(String.class));
        verify(saveClientApplicationPort).save(any(ClientApplication.class));
    }

    @Test
    void register_shouldThrowInvalidRedirectUri_whenAuthCodeGrantAndRedirectUrisNull() {
        ClientApplicationService s = service();

        RegisterClientApplicationUseCase.RegisterClientCommand cmd = mock(RegisterClientApplicationUseCase.RegisterClientCommand.class);
        when(cmd.name()).thenReturn("Spa");
        when(cmd.clientType()).thenReturn(ClientType.SPA);
        when(cmd.grantTypes()).thenReturn(Set.of(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN));
        when(cmd.redirectUris()).thenReturn(null);

        AuthException ex = assertThrows(AuthException.class, () -> s.register(cmd));
        assertEquals(AuthErrorCode.INVALID_REDIRECT_URI, ex.getErrorCode());

        verifyNoInteractions(saveClientApplicationPort, passwordHashingPort);
    }

    @Test
    void register_shouldThrowInvalidRedirectUri_whenAuthCodeGrantAndRedirectUrisEmpty() {
        ClientApplicationService s = service();

        RegisterClientApplicationUseCase.RegisterClientCommand cmd = mock(RegisterClientApplicationUseCase.RegisterClientCommand.class);
        when(cmd.name()).thenReturn("Spa");
        when(cmd.clientType()).thenReturn(ClientType.SPA);
        when(cmd.grantTypes()).thenReturn(Set.of(GrantType.AUTHORIZATION_CODE, GrantType.REFRESH_TOKEN));
        when(cmd.redirectUris()).thenReturn(Set.of());

        AuthException ex = assertThrows(AuthException.class, () -> s.register(cmd));
        assertEquals(AuthErrorCode.INVALID_REDIRECT_URI, ex.getErrorCode());

        verifyNoInteractions(saveClientApplicationPort, passwordHashingPort);
    }

    @Test
    void register_shouldThrowClientNotAllowedForGrantType_whenSpaHasClientCredentials() {
        ClientApplicationService s = service();

        RegisterClientApplicationUseCase.RegisterClientCommand cmd = mock(RegisterClientApplicationUseCase.RegisterClientCommand.class);
        when(cmd.name()).thenReturn("Spa");
        when(cmd.clientType()).thenReturn(ClientType.SPA);
        when(cmd.grantTypes()).thenReturn(Set.of(GrantType.CLIENT_CREDENTIALS));

        AuthException ex = assertThrows(AuthException.class, () -> s.register(cmd));
        assertEquals(AuthErrorCode.CLIENT_NOT_ALLOWED_FOR_GRANT_TYPE, ex.getErrorCode());

        verifyNoInteractions(saveClientApplicationPort, passwordHashingPort);
    }

    @Test
    void register_shouldThrowClientNotAllowedForGrantType_whenM2MMissingClientCredentials() {
        ClientApplicationService s = service();

        RegisterClientApplicationUseCase.RegisterClientCommand cmd = mock(RegisterClientApplicationUseCase.RegisterClientCommand.class);
        when(cmd.name()).thenReturn("M2M");
        when(cmd.clientType()).thenReturn(ClientType.MACHINE_TO_MACHINE);
        when(cmd.grantTypes()).thenReturn(Set.of(GrantType.REFRESH_TOKEN));

        PasswordHash ph = mock(PasswordHash.class);
        when(ph.value()).thenReturn("hash");
        when(passwordHashingPort.hash(anyString())).thenReturn(ph);

        AuthException ex = assertThrows(AuthException.class, () -> s.register(cmd));
        assertEquals(AuthErrorCode.CLIENT_NOT_ALLOWED_FOR_GRANT_TYPE, ex.getErrorCode());

        verify(passwordHashingPort).hash(anyString());
        verifyNoInteractions(saveClientApplicationPort);
    }

    @Test
    void register_shouldThrowClientNotAllowedForGrantType_whenM2MHasAuthorizationCode() {
        ClientApplicationService s = service();

        RegisterClientApplicationUseCase.RegisterClientCommand cmd = mock(RegisterClientApplicationUseCase.RegisterClientCommand.class);
        when(cmd.name()).thenReturn("M2M");
        when(cmd.clientType()).thenReturn(ClientType.MACHINE_TO_MACHINE);
        when(cmd.grantTypes()).thenReturn(Set.of(GrantType.CLIENT_CREDENTIALS, GrantType.AUTHORIZATION_CODE));

        PasswordHash ph = mock(PasswordHash.class);
        when(ph.value()).thenReturn("hash-m2m");
        when(passwordHashingPort.hash(anyString())).thenReturn(ph);

        AuthException ex = assertThrows(AuthException.class, () -> s.register(cmd));
        assertEquals(AuthErrorCode.CLIENT_NOT_ALLOWED_FOR_GRANT_TYPE, ex.getErrorCode());

        verify(passwordHashingPort).hash(anyString());
        verifyNoInteractions(saveClientApplicationPort);
    }

    @Test
    void register_shouldThrowClientNotAllowedForGrantType_whenM2MHasPasswordGrant() {
        ClientApplicationService s = service();

        RegisterClientApplicationUseCase.RegisterClientCommand cmd = mock(RegisterClientApplicationUseCase.RegisterClientCommand.class);
        when(cmd.name()).thenReturn("M2M");
        when(cmd.clientType()).thenReturn(ClientType.MACHINE_TO_MACHINE);
        when(cmd.grantTypes()).thenReturn(Set.of(GrantType.CLIENT_CREDENTIALS, GrantType.PASSWORD));

        PasswordHash ph = mock(PasswordHash.class);
        when(ph.value()).thenReturn("hash-m2m");
        when(passwordHashingPort.hash(anyString())).thenReturn(ph);

        AuthException ex = assertThrows(AuthException.class, () -> s.register(cmd));
        assertEquals(AuthErrorCode.CLIENT_NOT_ALLOWED_FOR_GRANT_TYPE, ex.getErrorCode());

        verify(passwordHashingPort).hash(anyString());
        verifyNoInteractions(saveClientApplicationPort);
    }

    @Test
    void register_shouldUseRequestedGrantsAsIs_whenProvided_andNotRequireRedirectUrisIfNoAuthCode() {
        ClientApplicationService s = service();

        Set<GrantType> requested = Set.of(GrantType.REFRESH_TOKEN);

        RegisterClientApplicationUseCase.RegisterClientCommand cmd = mock(RegisterClientApplicationUseCase.RegisterClientCommand.class);
        when(cmd.name()).thenReturn("Conf");
        when(cmd.clientType()).thenReturn(ClientType.CONFIDENTIAL);
        when(cmd.grantTypes()).thenReturn(requested);
        when(cmd.redirectUris()).thenReturn(null);
        when(cmd.scopes()).thenReturn(Set.of("openid"));
        when(cmd.firstParty()).thenReturn(true);

        PasswordHash ph = mock(PasswordHash.class);
        when(ph.value()).thenReturn("hash-req");
        when(passwordHashingPort.hash(any(String.class))).thenReturn(ph);

        when(saveClientApplicationPort.save(any(ClientApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<ClientApplication> captor = ArgumentCaptor.forClass(ClientApplication.class);

        ClientApplication saved = s.register(cmd);

        assertNotNull(saved);

        verify(saveClientApplicationPort).save(captor.capture());
        ClientApplication passed = captor.getValue();
        assertTrue(passed.grantTypes().contains(GrantType.REFRESH_TOKEN));
        assertFalse(passed.grantTypes().contains(GrantType.AUTHORIZATION_CODE));
    }
}