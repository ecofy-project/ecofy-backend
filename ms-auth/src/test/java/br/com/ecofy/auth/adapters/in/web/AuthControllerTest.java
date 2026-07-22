package br.com.ecofy.auth.adapters.in.web;

import br.com.ecofy.auth.adapters.in.web.dto.request.LoginRequest;
import br.com.ecofy.auth.adapters.in.web.dto.request.RefreshTokenRequest;
import br.com.ecofy.auth.adapters.in.web.dto.request.RevokeTokenRequest;
import br.com.ecofy.auth.adapters.in.web.dto.request.ValidateTokenRequest;
import br.com.ecofy.auth.adapters.in.web.dto.response.TokenResponse;
import br.com.ecofy.auth.adapters.in.web.dto.response.ValidateTokenResponse;
import br.com.ecofy.auth.core.port.in.AuthenticateUserUseCase;
import br.com.ecofy.auth.core.port.in.RefreshTokenUseCase;
import br.com.ecofy.auth.core.port.in.RevokeTokenUseCase;
import br.com.ecofy.auth.core.port.in.ValidateTokenUseCase;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários do controlador de autenticação")
class AuthControllerTest {

    @Mock
    private AuthenticateUserUseCase authenticateUserUseCase;

    @Mock
    private RefreshTokenUseCase refreshTokenUseCase;

    @Mock
    private RevokeTokenUseCase revokeTokenUseCase;

    @Mock
    private ValidateTokenUseCase validateTokenUseCase;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(
                authenticateUserUseCase,
                refreshTokenUseCase,
                revokeTokenUseCase,
                validateTokenUseCase
        );
    }

    @Test
    @DisplayName("Deve usar X-Forwarded-For e retornar os tokens quando o cabeçalho estiver presente")
    void token_shouldUseXForwardedFor_whenPresentAndReturnTokenResponse() {
        // Arrange
        LoginRequest request = new LoginRequest(
                "client-1",
                "secret-1",
                "user-1",
                "password-1",
                "openid profile"
        );

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2");
        httpRequest.setRemoteAddr("192.168.0.100");

        var result = mockAuthResult();
        when(authenticateUserUseCase.authenticate(any())).thenReturn(result);

        // Act
        ResponseEntity<TokenResponse> response = controller.token(request, httpRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TokenResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("Bearer", body.tokenType());
        assertEquals("access-token-123", body.accessToken());
        assertEquals("refresh-token-456", body.refreshToken());
        assertEquals(3600L, body.expiresIn());

        assertNoStoreHeaders(response.getHeaders());

        // Captura o comando enviado para validar o mapeamento.
        ArgumentCaptor<AuthenticateUserUseCase.AuthenticationCommand> captor =
                ArgumentCaptor.forClass(AuthenticateUserUseCase.AuthenticationCommand.class);

        verify(authenticateUserUseCase, times(1)).authenticate(captor.capture());
        AuthenticateUserUseCase.AuthenticationCommand cmd = captor.getValue();

        assertEquals("client-1", cmd.clientId());
        assertEquals("secret-1", cmd.clientSecret());
        assertEquals("user-1", cmd.username());
        assertEquals("password-1", cmd.password());
        assertEquals("openid profile", cmd.scope());
    }

    @Test
    @DisplayName("Deve usar X-Real-IP quando X-Forwarded-For estiver ausente")
    void token_shouldUseXRealIp_whenXForwardedForIsMissing() {
        // Arrange
        LoginRequest request = new LoginRequest(
                "client-2",
                "secret-2",
                "user-2",
                "password-2",
                "read"
        );

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.addHeader("X-Real-IP", "172.16.0.10");
        httpRequest.setRemoteAddr("192.168.0.200");

        var result = mockAuthResult();
        when(authenticateUserUseCase.authenticate(any())).thenReturn(result);

        // Act
        ResponseEntity<TokenResponse> response = controller.token(request, httpRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNoStoreHeaders(response.getHeaders());

        // Captura o comando enviado para validar o mapeamento.
        ArgumentCaptor<AuthenticateUserUseCase.AuthenticationCommand> captor =
                ArgumentCaptor.forClass(AuthenticateUserUseCase.AuthenticationCommand.class);

        verify(authenticateUserUseCase).authenticate(captor.capture());
        AuthenticateUserUseCase.AuthenticationCommand cmd = captor.getValue();
        assertEquals("client-2", cmd.clientId());
        assertEquals("secret-2", cmd.clientSecret());
    }

    @Test
    @DisplayName("Deve usar o endereço remoto quando os cabeçalhos de IP estiverem ausentes")
    void token_shouldUseRemoteAddr_whenNoIpHeadersPresent() {
        // Arrange
        LoginRequest request = new LoginRequest(
                "client-3",
                "secret-3",
                "user-3",
                "password-3",
                "write"
        );

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setRemoteAddr("127.0.0.1");

        var result = mockAuthResult();
        when(authenticateUserUseCase.authenticate(any())).thenReturn(result);

        // Act
        ResponseEntity<TokenResponse> response = controller.token(request, httpRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNoStoreHeaders(response.getHeaders());

        ArgumentCaptor<AuthenticateUserUseCase.AuthenticationCommand> captor =
                ArgumentCaptor.forClass(AuthenticateUserUseCase.AuthenticationCommand.class);

        verify(authenticateUserUseCase).authenticate(captor.capture());
        AuthenticateUserUseCase.AuthenticationCommand cmd = captor.getValue();
        assertEquals("client-3", cmd.clientId());
        assertEquals("secret-3", cmd.clientSecret());
    }

    @Test
    @DisplayName("Deve retornar novos tokens e cabeçalhos sem cache durante a renovação")
    void refresh_shouldReturnNewTokenAndNoStoreHeaders() {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest(
                "client-refresh",
                "refresh-token-xyz",
                "openid"
        );

        var result = mockRefreshResult();
        when(refreshTokenUseCase.refresh(any())).thenReturn(result);

        // Act
        ResponseEntity<TokenResponse> response = controller.refresh(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TokenResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("Bearer", body.tokenType());
        assertEquals("new-access-token-123", body.accessToken());
        assertEquals("new-refresh-token-456", body.refreshToken());
        assertEquals(7200L, body.expiresIn());

        assertNoStoreHeaders(response.getHeaders());

        ArgumentCaptor<RefreshTokenUseCase.RefreshTokenCommand> captor =
                ArgumentCaptor.forClass(RefreshTokenUseCase.RefreshTokenCommand.class);

        verify(refreshTokenUseCase).refresh(captor.capture());

        RefreshTokenUseCase.RefreshTokenCommand cmd = captor.getValue();
        assertEquals("client-refresh", cmd.clientId());
        assertEquals("refresh-token-xyz", cmd.refreshToken());
        assertEquals("openid", cmd.scope());
    }

    @Test
    @DisplayName("Deve tratar o indicador nulo como refresh token")
    void revoke_shouldTreatNullRefreshFlagAsRefreshToken() {
        // Arrange
        RevokeTokenRequest request = new RevokeTokenRequest(
                "token-to-revoke-1",
                null
        );

        // Act
        ResponseEntity<Void> response = controller.revoke(request);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNoStoreHeaders(response.getHeaders());

        ArgumentCaptor<RevokeTokenUseCase.RevokeTokenCommand> captor =
                ArgumentCaptor.forClass(RevokeTokenUseCase.RevokeTokenCommand.class);

        verify(revokeTokenUseCase).revoke(captor.capture());
        RevokeTokenUseCase.RevokeTokenCommand cmd = captor.getValue();

        assertEquals("token-to-revoke-1", cmd.token());
        assertTrue(
                cmd.refreshToken(),
                "Quando refreshToken é null deve considerar como refresh token"
        );
    }

    @Test
    @DisplayName("Deve tratar o indicador falso como access token")
    void revoke_shouldTreatFalseRefreshFlagAsAccessToken() {
        // Arrange
        RevokeTokenRequest request = new RevokeTokenRequest(
                "token-to-revoke-2",
                Boolean.FALSE
        );

        // Act
        ResponseEntity<Void> response = controller.revoke(request);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNoStoreHeaders(response.getHeaders());

        ArgumentCaptor<RevokeTokenUseCase.RevokeTokenCommand> captor =
                ArgumentCaptor.forClass(RevokeTokenUseCase.RevokeTokenCommand.class);

        verify(revokeTokenUseCase).revoke(captor.capture());
        RevokeTokenUseCase.RevokeTokenCommand cmd = captor.getValue();

        assertEquals("token-to-revoke-2", cmd.token());
        assertFalse(
                cmd.refreshToken(),
                "Quando refreshToken é false deve ser tratado como access token"
        );
    }

    @Test
    @DisplayName("Deve retornar as claims e os cabeçalhos sem cache quando o token for válido")
    void validate_shouldReturnClaimsAndNoStoreHeaders() {
        // Arrange
        ValidateTokenRequest request = new ValidateTokenRequest("valid-token-123");

        Map<String, Object> claims = Map.of(
                "sub", "user-123",
                "scope", "openid profile"
        );

        when(validateTokenUseCase.validate("valid-token-123")).thenReturn(claims);

        // Act
        ResponseEntity<ValidateTokenResponse> response = controller.validate(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ValidateTokenResponse body = response.getBody();
        assertNotNull(body);

        assertTrue(body.valid());
        assertEquals(claims, body.claims());

        assertNoStoreHeaders(response.getHeaders());

        verify(validateTokenUseCase).validate("valid-token-123");
    }

    private AuthenticateUserUseCase.AuthenticationResult mockAuthResult() {
        AuthenticateUserUseCase.AuthenticationResult result =
                mock(
                        AuthenticateUserUseCase.AuthenticationResult.class,
                        Answers.RETURNS_DEEP_STUBS
                );

        when(result.tokenType()).thenReturn("Bearer");
        when(result.accessToken().value()).thenReturn("access-token-123");
        when(result.refreshToken()).thenReturn("refresh-token-456");
        when(result.expiresInSeconds()).thenReturn(3600L);

        return result;
    }

    private RefreshTokenUseCase.RefreshTokenResult mockRefreshResult() {
        RefreshTokenUseCase.RefreshTokenResult result =
                mock(RefreshTokenUseCase.RefreshTokenResult.class);

        when(result.accessToken()).thenReturn("new-access-token-123");
        when(result.refreshToken()).thenReturn("new-refresh-token-456");
        when(result.expiresInSeconds()).thenReturn(7200L);

        return result;
    }

    private void assertNoStoreHeaders(HttpHeaders headers) {
        assertNotNull(headers.getCacheControl());
        assertEquals("no-cache", headers.getFirst("Pragma"));
    }
}
