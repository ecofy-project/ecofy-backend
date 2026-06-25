package br.com.ecofy.auth.adapters.in.web.mapper;

import br.com.ecofy.auth.adapters.in.web.dto.response.UserResponse;
import br.com.ecofy.auth.core.domain.AuthUser;
import br.com.ecofy.auth.core.domain.Permission;
import br.com.ecofy.auth.core.domain.Role;
import br.com.ecofy.auth.core.domain.enums.AuthUserStatus;
import br.com.ecofy.auth.core.domain.valueobject.AuthUserId;
import br.com.ecofy.auth.core.domain.valueobject.EmailAddress;
import br.com.ecofy.auth.core.domain.valueobject.PasswordHash;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

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
    void shouldMapAuthUserToUserResponse() {
        Permission directPermission = new Permission("transactions:read", "Ler transações", "billing");

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

        UserResponse response = UserMapper.toResponse(user);

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
    void shouldReturnUnmodifiableRolesAndPermissionsInUserResponse() {
        AuthUser user = authUser(
                "Matheus",
                "Lemes",
                Set.of(new Role("ROLE_ADMIN", "Administrador", Set.of())),
                Set.of(new Permission("users:create", "Criar usuários", "auth"))
        );

        UserResponse response = UserMapper.toResponse(user);

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
    void shouldMapAuthUserWithEmptyRolesAndPermissions() {
        AuthUser user = authUser(
                "Matheus",
                "Lemes",
                Set.of(),
                Set.of()
        );

        UserResponse response = UserMapper.toResponse(user);

        assertTrue(response.roles().isEmpty());
        assertTrue(response.permissions().isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenUserIsNull() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> UserMapper.toResponse(null)
        );

        assertEquals("user must not be null", exception.getMessage());
    }

    @Test
    void shouldReturnEmptyListWhenUsersIsNull() {
        List<UserResponse> responses = UserMapper.toResponseList(null);

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    void shouldReturnEmptyListWhenUsersIsEmpty() {
        List<UserResponse> responses = UserMapper.toResponseList(List.of());

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    void shouldMapUserListFilteringNullValues() {
        AuthUser firstUser = authUser(
                "Matheus",
                "Lemes",
                Set.of(new Role("ROLE_USER", "Usuário", Set.of())),
                Set.of(new Permission("profile:read", "Ler perfil", "auth"))
        );

        AuthUser secondUser = new AuthUser(
                new AuthUserId(UUID.fromString("22222222-2222-2222-2222-222222222222")),
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

        List<UserResponse> responses = UserMapper.toResponseList(
                Arrays.asList(firstUser, null, secondUser)
        );

        assertEquals(2, responses.size());

        assertEquals(USER_UUID.toString(), responses.get(0).id());
        assertEquals("matheus@ecofy.com", responses.get(0).email());
        assertEquals("Matheus Lemes", responses.get(0).fullName());
        assertEquals(AuthUserStatus.ACTIVE.name(), responses.get(0).status());
        assertTrue(responses.get(0).emailVerified());
        assertEquals(Set.of("ROLE_USER"), responses.get(0).roles());
        assertEquals(Set.of("profile:read"), responses.get(0).permissions());

        assertEquals("22222222-2222-2222-2222-222222222222", responses.get(1).id());
        assertEquals("outro@ecofy.com", responses.get(1).email());
        assertEquals("Outro Usuário", responses.get(1).fullName());
        assertEquals(AuthUserStatus.PENDING_EMAIL_CONFIRMATION.name(), responses.get(1).status());
        assertFalse(responses.get(1).emailVerified());
        assertEquals(Set.of("ROLE_PENDING"), responses.get(1).roles());
        assertTrue(responses.get(1).permissions().isEmpty());
        assertNull(responses.get(1).lastLoginAt());
    }
    @Test
    void shouldReturnUnmodifiableResponseList() {
        AuthUser user = authUser(
                "Matheus",
                "Lemes",
                Set.of(),
                Set.of()
        );

        List<UserResponse> responses = UserMapper.toResponseList(List.of(user));

        assertEquals(1, responses.size());

        assertThrows(
                UnsupportedOperationException.class,
                () -> responses.add(UserMapper.toResponse(user))
        );
    }

    @Test
    void shouldInstantiatePrivateConstructorForCoverage() throws Exception {
        Constructor<UserMapper> constructor = UserMapper.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        UserMapper instance = constructor.newInstance();

        assertNotNull(instance);
    }

    private static AuthUser authUser(String firstName,
                                     String lastName,
                                     Set<Role> roles,
                                     Set<Permission> directPermissions) {
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