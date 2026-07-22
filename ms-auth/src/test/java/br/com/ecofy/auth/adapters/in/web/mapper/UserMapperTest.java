package br.com.ecofy.auth.adapters.in.web.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.ecofy.auth.adapters.in.web.dto.response.UserResponse;
import br.com.ecofy.auth.core.domain.AuthUser;
import br.com.ecofy.auth.core.domain.Permission;
import br.com.ecofy.auth.core.domain.Role;
import br.com.ecofy.auth.core.domain.enums.AuthUserStatus;
import br.com.ecofy.auth.core.domain.valueobject.AuthUserId;
import br.com.ecofy.auth.core.domain.valueobject.EmailAddress;
import br.com.ecofy.auth.core.domain.valueobject.PasswordHash;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Testes unitários do mapeador de usuários")
class UserMapperTest {

    private static final UUID USER_UUID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final Instant CREATED_AT =
            Instant.parse("2026-01-01T10:00:00Z");

    private static final Instant UPDATED_AT =
            Instant.parse("2026-01-02T10:00:00Z");

    private static final Instant LAST_LOGIN_AT =
            Instant.parse("2026-01-03T10:00:00Z");

    @Test
    @DisplayName("Deve converter o usuário preenchido para a resposta correspondente")
    void toResponse_usuarioPreenchido_deveConverterTodosOsCampos() {
        // Arrange
        Permission directPermission = new Permission(
                "transactions:read",
                "Ler transações",
                "billing"
        );

        Role userRole = new Role(
                "ROLE_USER",
                "Usuário comum",
                Set.of(new Permission("profile:read", "Ler perfil", "auth"))
        );

        AuthUser user = new AuthUser(
                new AuthUserId(USER_UUID),
                new EmailAddress("MATHEUS@ECOFY.COM"),
                new PasswordHash("hashed-password"),
                AuthUserStatus.ACTIVE,
                true,
                "Matheus",
                "Lemes",
                "pt-BR",
                Set.of(userRole),
                Set.of(directPermission),
                CREATED_AT,
                UPDATED_AT,
                LAST_LOGIN_AT,
                0
        );

        // Act
        UserResponse response = UserMapper.toResponse(user);

        // Assert
        assertEquals(USER_UUID.toString(), response.id());
        assertEquals("matheus@ecofy.com", response.email());
        assertEquals("Matheus Lemes", response.fullName());
        assertEquals(AuthUserStatus.ACTIVE.name(), response.status());
        assertTrue(response.emailVerified());
        assertEquals(Set.of("ROLE_USER"), response.roles());
        assertEquals(Set.of("transactions:read"), response.permissions());
        assertEquals(CREATED_AT, response.createdAt());
        assertEquals(UPDATED_AT, response.updatedAt());
        assertEquals(LAST_LOGIN_AT, response.lastLoginAt());
    }

    @Test
    @DisplayName("Deve retornar papéis e permissões imutáveis ao converter o usuário")
    void toResponse_usuarioComPapeisEPermissoes_deveRetornarConjuntosImutaveis() {
        // Arrange
        AuthUser user = authUser(
                "Matheus",
                "Lemes",
                Set.of(new Role("ROLE_ADMIN", "Administrador", Set.of())),
                Set.of(new Permission("users:create", "Criar usuários", "auth"))
        );

        // Act
        UserResponse response = UserMapper.toResponse(user);

        // Assert
        assertThrows(
                UnsupportedOperationException.class,
                () -> response.roles().add("ROLE_OTHER")
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> response.permissions().add("users:delete")
        );
    }

    @Test
    @DisplayName("Deve retornar conjuntos vazios quando o usuário não possuir papéis ou permissões")
    void toResponse_usuarioSemPapeisEPermissoes_deveRetornarConjuntosVazios() {
        // Arrange
        AuthUser user = authUser(
                "Matheus",
                "Lemes",
                Set.of(),
                Set.of()
        );

        // Act
        UserResponse response = UserMapper.toResponse(user);

        // Assert
        assertNotNull(response.roles());
        assertNotNull(response.permissions());
        assertTrue(response.roles().isEmpty());
        assertTrue(response.permissions().isEmpty());
    }

    @Test
    @DisplayName("Deve lançar exceção quando o usuário informado for nulo")
    void toResponse_usuarioNulo_deveLancarNullPointerException() {
        // Arrange
        AuthUser user = null;

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> UserMapper.toResponse(user)
        );

        // Assert
        assertEquals("user must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando a lista de usuários for nula")
    void toResponseList_listaNula_deveRetornarListaVazia() {
        // Arrange
        List<AuthUser> users = null;

        // Act
        List<UserResponse> responses = UserMapper.toResponseList(users);

        // Assert
        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não houver usuários")
    void toResponseList_listaVazia_deveRetornarListaVazia() {
        // Arrange
        List<AuthUser> users = List.of();

        // Act
        List<UserResponse> responses = UserMapper.toResponseList(users);

        // Assert
        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    @DisplayName("Deve converter a lista de usuários ignorando elementos nulos")
    void toResponseList_listaComElementoNulo_deveConverterUsuariosValidos() {
        // Arrange
        AuthUser firstUser = authUser(
                "Matheus",
                "Lemes",
                Set.of(new Role("ROLE_USER", "Usuário", Set.of())),
                Set.of(new Permission("profile:read", "Ler perfil", "auth"))
        );

        AuthUser secondUser = new AuthUser(
                new AuthUserId(UUID.fromString(
                        "22222222-2222-2222-2222-222222222222"
                )),
                new EmailAddress("OUTRO@ECOFY.COM"),
                new PasswordHash("hashed-password"),
                AuthUserStatus.PENDING_EMAIL_CONFIRMATION,
                false,
                "Outro",
                "Usuário",
                "pt-BR",
                Set.of(new Role("ROLE_PENDING", "Pendente", Set.of())),
                Set.of(),
                CREATED_AT,
                UPDATED_AT,
                null,
                0
        );

        List<AuthUser> users = Arrays.asList(firstUser, null, secondUser);

        // Act
        List<UserResponse> responses = UserMapper.toResponseList(users);

        // Assert
        assertEquals(2, responses.size());

        UserResponse firstResponse = responses.get(0);
        assertEquals(USER_UUID.toString(), firstResponse.id());
        assertEquals("matheus@ecofy.com", firstResponse.email());
        assertEquals("Matheus Lemes", firstResponse.fullName());
        assertEquals(AuthUserStatus.ACTIVE.name(), firstResponse.status());
        assertTrue(firstResponse.emailVerified());
        assertEquals(Set.of("ROLE_USER"), firstResponse.roles());
        assertEquals(Set.of("profile:read"), firstResponse.permissions());
        assertEquals(CREATED_AT, firstResponse.createdAt());
        assertEquals(UPDATED_AT, firstResponse.updatedAt());
        assertEquals(LAST_LOGIN_AT, firstResponse.lastLoginAt());

        UserResponse secondResponse = responses.get(1);
        assertEquals(
                "22222222-2222-2222-2222-222222222222",
                secondResponse.id()
        );
        assertEquals("outro@ecofy.com", secondResponse.email());
        assertEquals("Outro Usuário", secondResponse.fullName());
        assertEquals(
                AuthUserStatus.PENDING_EMAIL_CONFIRMATION.name(),
                secondResponse.status()
        );
        assertFalse(secondResponse.emailVerified());
        assertEquals(Set.of("ROLE_PENDING"), secondResponse.roles());
        assertTrue(secondResponse.permissions().isEmpty());
        assertEquals(CREATED_AT, secondResponse.createdAt());
        assertEquals(UPDATED_AT, secondResponse.updatedAt());
        assertNull(secondResponse.lastLoginAt());
    }

    @Test
    @DisplayName("Deve retornar uma lista imutável após converter os usuários")
    void toResponseList_listaValida_deveRetornarListaImutavel() {
        // Arrange
        AuthUser user = authUser(
                "Matheus",
                "Lemes",
                Set.of(),
                Set.of()
        );

        // Act
        List<UserResponse> responses =
                UserMapper.toResponseList(List.of(user));

        // Assert
        assertEquals(1, responses.size());
        assertThrows(
                UnsupportedOperationException.class,
                () -> responses.add(UserMapper.toResponse(user))
        );
    }

    @Test
    @DisplayName("Deve manter o construtor privado da classe utilitária")
    void constructor_classeUtilitaria_deveSerPrivadoEExecutavelPorReflexao()
            throws Exception {
        // Arrange
        Constructor<UserMapper> constructor =
                UserMapper.class.getDeclaredConstructor();

        // Act
        constructor.setAccessible(true);
        UserMapper instance = constructor.newInstance();

        // Assert
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        assertNotNull(instance);
    }

    private static AuthUser authUser(
            String firstName,
            String lastName,
            Set<Role> roles,
            Set<Permission> directPermissions
    ) {
        return new AuthUser(
                new AuthUserId(USER_UUID),
                new EmailAddress("MATHEUS@ECOFY.COM"),
                new PasswordHash("hashed-password"),
                AuthUserStatus.ACTIVE,
                true,
                firstName,
                lastName,
                "pt-BR",
                roles,
                directPermissions,
                CREATED_AT,
                UPDATED_AT,
                LAST_LOGIN_AT,
                0
        );
    }
}
