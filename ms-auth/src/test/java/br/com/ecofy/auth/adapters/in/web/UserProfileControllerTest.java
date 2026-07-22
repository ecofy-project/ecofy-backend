package br.com.ecofy.auth.adapters.in.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.ecofy.auth.adapters.in.web.dto.response.UserResponse;
import br.com.ecofy.auth.core.domain.AuthUser;
import br.com.ecofy.auth.core.domain.Permission;
import br.com.ecofy.auth.core.domain.Role;
import br.com.ecofy.auth.core.domain.enums.AuthUserStatus;
import br.com.ecofy.auth.core.domain.valueobject.AuthUserId;
import br.com.ecofy.auth.core.domain.valueobject.EmailAddress;
import br.com.ecofy.auth.core.domain.valueobject.PasswordHash;
import br.com.ecofy.auth.core.port.in.GetCurrentUserProfileUseCase;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários do controlador de perfil do usuário")
class UserProfileControllerTest {

    private static final UUID USER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final Instant CREATED_AT =
            Instant.parse("2026-01-01T10:00:00Z");

    private static final Instant UPDATED_AT =
            Instant.parse("2026-01-02T10:00:00Z");

    private static final Instant LAST_LOGIN_AT =
            Instant.parse("2026-01-03T10:00:00Z");

    @Mock
    private GetCurrentUserProfileUseCase getCurrentUserProfileUseCase;

    private UserProfileController controller;

    @BeforeEach
    void setUp() {
        controller = new UserProfileController(
                getCurrentUserProfileUseCase
        );
    }

    @Test
    @DisplayName("Deve retornar o perfil quando o usuário autenticado existir")
    void me_usuarioAutenticado_deveRetornarPerfil() {
        // Arrange
        AuthUser user = authUser();

        when(getCurrentUserProfileUseCase.getCurrentUser())
                .thenReturn(user);

        // Act
        ResponseEntity<UserResponse> response = controller.me();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());

        UserResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(USER_ID.toString(), body.id());
        assertEquals("user@ecofy.com", body.email());
        assertEquals("Matheus Lemes", body.fullName());
        assertEquals(AuthUserStatus.ACTIVE.name(), body.status());
        assertEquals(true, body.emailVerified());
        assertEquals(Set.of("ROLE_USER"), body.roles());
        assertEquals(Set.of("profile:read"), body.permissions());
        assertEquals(CREATED_AT, body.createdAt());
        assertEquals(UPDATED_AT, body.updatedAt());
        assertEquals(LAST_LOGIN_AT, body.lastLoginAt());

        verify(getCurrentUserProfileUseCase).getCurrentUser();
    }

    @Test
    @DisplayName("Deve propagar a exceção quando a consulta do perfil falhar")
    void me_falhaNoCasoDeUso_devePropagarExcecao() {
        // Arrange
        IllegalStateException expectedException =
                new IllegalStateException("Falha ao recuperar perfil");

        when(getCurrentUserProfileUseCase.getCurrentUser())
                .thenThrow(expectedException);

        // Act
        IllegalStateException actualException = assertThrows(
                IllegalStateException.class,
                () -> controller.me()
        );

        // Assert
        assertSame(expectedException, actualException);
        verify(getCurrentUserProfileUseCase).getCurrentUser();
    }

    @Test
    @DisplayName("Deve lançar exceção quando o caso de uso retornar usuário nulo")
    void me_usuarioNulo_deveLancarNullPointerException() {
        // Arrange
        when(getCurrentUserProfileUseCase.getCurrentUser())
                .thenReturn(null);

        // Act
        assertThrows(
                NullPointerException.class,
                () -> controller.me()
        );

        // Assert
        verify(getCurrentUserProfileUseCase).getCurrentUser();
    }

    private AuthUser authUser() {
        Permission permission = new Permission(
                "profile:read",
                "Ler perfil",
                "auth"
        );

        Role role = new Role(
                "ROLE_USER",
                "Usuário",
                Set.of()
        );

        return new AuthUser(
                new AuthUserId(USER_ID),
                new EmailAddress("user@ecofy.com"),
                new PasswordHash("hashed-password"),
                AuthUserStatus.ACTIVE,
                true,
                "Matheus",
                "Lemes",
                "pt-BR",
                Set.of(role),
                Set.of(permission),
                CREATED_AT,
                UPDATED_AT,
                LAST_LOGIN_AT,
                0
        );
    }
}
