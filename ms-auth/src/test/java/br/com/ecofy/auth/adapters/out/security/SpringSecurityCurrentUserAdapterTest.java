package br.com.ecofy.auth.adapters.out.security;

import br.com.ecofy.auth.adapters.out.persistence.AuthUserJpaAdapter;
import br.com.ecofy.auth.core.domain.AuthUser;
import br.com.ecofy.auth.core.domain.enums.AuthUserStatus;
import br.com.ecofy.auth.core.domain.valueobject.AuthUserId;
import br.com.ecofy.auth.core.domain.valueobject.EmailAddress;
import br.com.ecofy.auth.core.domain.valueobject.PasswordHash;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários do adaptador de usuário atual do Spring Security")
class SpringSecurityCurrentUserAdapterTest {

    private static final UUID USER_ID = UUID.fromString(
            "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
    );
    private static final Instant ISSUED_AT =
            Instant.parse("2026-07-20T10:00:00Z");
    private static final Instant EXPIRES_AT =
            Instant.parse("2026-07-20T11:00:00Z");

    @Mock
    private AuthUserJpaAdapter authUserJpaAdapter;

    @Mock
    private Authentication authentication;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Deve rejeitar adaptador JPA nulo ao construir o provedor")
    void constructor_authUserJpaAdapterNulo_deveLancarNullPointerException() {
        // Arrange
        AuthUserJpaAdapter nullAdapter = null;

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new SpringSecurityCurrentUserAdapter(nullAdapter)
        );

        // Assert
        assertEquals(
                "authUserJpaAdapter must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar contexto sem autenticação")
    void getCurrentUserOrThrow_authenticationNula_deveLancarIllegalStateException() {
        // Arrange
        SecurityContextHolder.clearContext();
        SpringSecurityCurrentUserAdapter adapter = createAdapter();

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                adapter::getCurrentUserOrThrow
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "Authentication token is missing",
                        exception.getMessage()
                ),
                () -> verifyNoInteractions(authUserJpaAdapter)
        );
    }

    @Test
    @DisplayName("Deve rejeitar principal que não seja um JWT")
    void getCurrentUserOrThrow_principalNaoJwt_deveLancarIllegalStateException() {
        // Arrange
        when(authentication.getPrincipal())
                .thenReturn("principal-invalido");

        configureAuthentication(authentication);

        SpringSecurityCurrentUserAdapter adapter = createAdapter();

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                adapter::getCurrentUserOrThrow
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "Authentication token is invalid",
                        exception.getMessage()
                ),
                () -> verifyNoInteractions(authUserJpaAdapter)
        );
    }

    @Test
    @DisplayName("Deve propagar NullPointerException quando o principal for nulo")
    void getCurrentUserOrThrow_principalNulo_deveLancarNullPointerException() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(null);

        configureAuthentication(authentication);

        SpringSecurityCurrentUserAdapter adapter = createAdapter();

        // Act
        assertThrows(
                NullPointerException.class,
                adapter::getCurrentUserOrThrow
        );

        // Assert
        verifyNoInteractions(authUserJpaAdapter);
    }

    @Test
    @DisplayName("Deve rejeitar JWT cujo subject não represente um UUID")
    void getCurrentUserOrThrow_subjectInvalido_deveLancarIllegalStateException() {
        // Arrange
        Jwt jwt = createJwt("identificador-invalido");

        when(authentication.getPrincipal()).thenReturn(jwt);

        configureAuthentication(authentication);

        SpringSecurityCurrentUserAdapter adapter = createAdapter();

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                adapter::getCurrentUserOrThrow
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "JWT subject is not a valid user identifier",
                        exception.getMessage()
                ),
                () -> verifyNoInteractions(authUserJpaAdapter)
        );
    }

    @Test
    @DisplayName("Deve rejeitar JWT cujo subject esteja vazio")
    void getCurrentUserOrThrow_subjectVazio_deveLancarIllegalStateException() {
        // Arrange
        Jwt jwt = createJwt("");

        when(authentication.getPrincipal()).thenReturn(jwt);

        configureAuthentication(authentication);

        SpringSecurityCurrentUserAdapter adapter = createAdapter();

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                adapter::getCurrentUserOrThrow
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "JWT subject is not a valid user identifier",
                        exception.getMessage()
                ),
                () -> verifyNoInteractions(authUserJpaAdapter)
        );
    }

    @Test
    @DisplayName("Deve rejeitar JWT sem subject")
    void getCurrentUserOrThrow_subjectNulo_deveLancarIllegalStateException() {
        // Arrange
        Jwt jwt = createJwtWithoutSubject();

        when(authentication.getPrincipal()).thenReturn(jwt);

        configureAuthentication(authentication);

        SpringSecurityCurrentUserAdapter adapter = createAdapter();

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                adapter::getCurrentUserOrThrow
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "JWT subject is not a valid user identifier",
                        exception.getMessage()
                ),
                () -> verifyNoInteractions(authUserJpaAdapter)
        );
    }

    @Test
    @DisplayName("Deve retornar o usuário correspondente ao subject válido do JWT")
    void getCurrentUserOrThrow_usuarioExistente_deveRetornarUsuarioAutenticado() {
        // Arrange
        Jwt jwt = createJwt(USER_ID.toString());
        AuthUser user = createUser();

        when(authentication.getPrincipal()).thenReturn(jwt);
        when(authUserJpaAdapter.loadById(any(AuthUserId.class)))
                .thenReturn(Optional.of(user));

        configureAuthentication(authentication);

        SpringSecurityCurrentUserAdapter adapter = createAdapter();
        ArgumentCaptor<AuthUserId> idCaptor =
                ArgumentCaptor.forClass(AuthUserId.class);

        // Act
        AuthUser result = adapter.getCurrentUserOrThrow();

        // Assert
        assertSame(user, result);

        verify(authUserJpaAdapter).loadById(idCaptor.capture());

        assertEquals(USER_ID, idCaptor.getValue().value());
    }

    @Test
    @DisplayName("Deve lançar exceção quando o usuário do JWT não existir no banco")
    void getCurrentUserOrThrow_usuarioInexistente_deveLancarIllegalStateException() {
        // Arrange
        Jwt jwt = createJwt(USER_ID.toString());

        when(authentication.getPrincipal()).thenReturn(jwt);
        when(authUserJpaAdapter.loadById(any(AuthUserId.class)))
                .thenReturn(Optional.empty());

        configureAuthentication(authentication);

        SpringSecurityCurrentUserAdapter adapter = createAdapter();
        ArgumentCaptor<AuthUserId> idCaptor =
                ArgumentCaptor.forClass(AuthUserId.class);

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                adapter::getCurrentUserOrThrow
        );

        // Assert
        assertEquals(
                "Usuário autenticado não encontrado",
                exception.getMessage()
        );

        verify(authUserJpaAdapter).loadById(idCaptor.capture());

        assertEquals(USER_ID, idCaptor.getValue().value());
    }

    @Test
    @DisplayName("Deve propagar a exceção quando a consulta do usuário falhar")
    void getCurrentUserOrThrow_consultaLancaExcecao_devePropagarExcecao() {
        // Arrange
        Jwt jwt = createJwt(USER_ID.toString());
        RuntimeException repositoryException =
                new RuntimeException("Falha ao consultar usuário");

        when(authentication.getPrincipal()).thenReturn(jwt);
        when(authUserJpaAdapter.loadById(any(AuthUserId.class)))
                .thenThrow(repositoryException);

        configureAuthentication(authentication);

        SpringSecurityCurrentUserAdapter adapter = createAdapter();

        // Act
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                adapter::getCurrentUserOrThrow
        );

        // Assert
        assertSame(repositoryException, exception);

        verify(authUserJpaAdapter).loadById(
                any(AuthUserId.class)
        );
    }

    @Test
    @DisplayName("Deve ignorar o estado autenticado quando o contexto for limpo")
    void getCurrentUserOrThrow_contextoLimpo_deveLancarAuthenticationMissing() {
        // Arrange
        configureAuthentication(authentication);
        SecurityContextHolder.clearContext();

        SpringSecurityCurrentUserAdapter adapter = createAdapter();

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                adapter::getCurrentUserOrThrow
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "Authentication token is missing",
                        exception.getMessage()
                ),
                () -> verify(
                        authentication,
                        never()
                ).getPrincipal(),
                () -> verifyNoInteractions(authUserJpaAdapter)
        );
    }

    private SpringSecurityCurrentUserAdapter createAdapter() {
        return new SpringSecurityCurrentUserAdapter(
                authUserJpaAdapter
        );
    }

    private void configureAuthentication(
            Authentication configuredAuthentication
    ) {
        SecurityContext context =
                SecurityContextHolder.createEmptyContext();
        context.setAuthentication(configuredAuthentication);
        SecurityContextHolder.setContext(context);
    }

    private Jwt createJwt(String subject) {
        return Jwt.withTokenValue("jwt-token")
                .header("alg", "RS256")
                .subject(subject)
                .claim("jti", "jwt-id")
                .issuedAt(ISSUED_AT)
                .expiresAt(EXPIRES_AT)
                .build();
    }

    private Jwt createJwtWithoutSubject() {
        return Jwt.withTokenValue("jwt-token-without-subject")
                .header("alg", "RS256")
                .claim("scope", "openid")
                .claim("jti", "jwt-id")
                .issuedAt(ISSUED_AT)
                .expiresAt(EXPIRES_AT)
                .build();
    }

    private AuthUser createUser() {
        return new AuthUser(
                new AuthUserId(USER_ID),
                new EmailAddress("usuario@ecofy.com"),
                new PasswordHash("password-hash"),
                AuthUserStatus.ACTIVE,
                true,
                "Matheus",
                "Silva",
                "pt-BR",
                Set.of(),
                Set.of(),
                ISSUED_AT,
                ISSUED_AT,
                null,
                0
        );
    }
}
