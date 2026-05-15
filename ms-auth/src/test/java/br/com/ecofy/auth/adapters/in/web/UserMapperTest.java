package br.com.ecofy.auth.adapters.in.web;

import br.com.ecofy.auth.adapters.in.web.dto.response.UserResponse;
import br.com.ecofy.auth.adapters.in.web.mapper.UserMapper;
import br.com.ecofy.auth.core.domain.AuthUser;
import br.com.ecofy.auth.core.domain.Permission;
import br.com.ecofy.auth.core.domain.Role;
import br.com.ecofy.auth.core.domain.enums.AuthUserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserMapperTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AuthUser authUser;


    @Test
    void constructor_shouldNotBePublic_andCallableViaReflection() throws Exception {
        Constructor<UserMapper> ctor = UserMapper.class.getDeclaredConstructor();

        assertFalse(Modifier.isPublic(ctor.getModifiers()), "Construtor não deve ser public");

        ctor.setAccessible(true);
        UserMapper instance = ctor.newInstance();
        assertNotNull(instance);
    }

    @Test
    void toResponse_shouldMapAllFields_andCollectRolesAndPermissions() {

        // Arrange
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2025-01-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2025-01-02T11:00:00Z");
        Instant lastLoginAt = Instant.parse("2025-01-03T12:00:00Z");

        when(authUser.id().value()).thenReturn(id);
        when(authUser.email().value()).thenReturn("user@example.com");
        when(authUser.fullName()).thenReturn("John Doe");
        when(authUser.status()).thenReturn(AuthUserStatus.ACTIVE);
        when(authUser.isEmailVerified()).thenReturn(true);
        when(authUser.createdAt()).thenReturn(createdAt);
        when(authUser.updatedAt()).thenReturn(updatedAt);
        when(authUser.lastLoginAt()).thenReturn(lastLoginAt);

        // roles
        Role roleUser = mock(Role.class);
        Role roleAdmin = mock(Role.class);
        when(roleUser.name()).thenReturn("AUTH_USER");
        when(roleAdmin.name()).thenReturn("AUTH_ADMIN");
        when(authUser.roles()).thenReturn(Set.of(roleUser, roleAdmin));

        // direct permissions
        Permission permRead = mock(Permission.class);
        Permission permWrite = mock(Permission.class);
        when(permRead.name()).thenReturn("USER_READ");
        when(permWrite.name()).thenReturn("USER_WRITE");
        when(authUser.directPermissions()).thenReturn(Set.of(permRead, permWrite));

        // Act
        UserResponse response = UserMapper.toResponse(authUser);

        // Assert básicos
        assertNotNull(response);
        assertEquals(id.toString(), response.id());
        assertEquals("user@example.com", response.email());
        assertEquals("John Doe", response.fullName());
        assertEquals(AuthUserStatus.ACTIVE.name(), response.status());
        assertTrue(response.emailVerified());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
        assertEquals(lastLoginAt, response.lastLoginAt());

        // roles e permissions: coletados e imutáveis
        assertEquals(Set.of("AUTH_USER", "AUTH_ADMIN"), response.roles());
        assertEquals(Set.of("USER_READ", "USER_WRITE"), response.permissions());

        assertThrows(UnsupportedOperationException.class,
                () -> response.roles().add("OTHER_ROLE"));
        assertThrows(UnsupportedOperationException.class,
                () -> response.permissions().add("OTHER_PERMISSION"));

    }

    @Test
    void toResponse_shouldThrowNullPointerException_whenUserIsNull() {

        assertThrows(NullPointerException.class,
                () -> UserMapper.toResponse(null));

    }

    @Test
    void toResponseList_shouldReturnEmptyList_whenInputIsNullOrEmpty() {
        List<UserResponse> fromNull = UserMapper.toResponseList(null);
        assertNotNull(fromNull);
        assertTrue(fromNull.isEmpty());

        List<UserResponse> fromEmpty = UserMapper.toResponseList(List.of());
        assertNotNull(fromEmpty);
        assertTrue(fromEmpty.isEmpty());
    }

    @Test
    void toResponseList_shouldMapUsers_filterNullsAndReturnUnmodifiableList() {

        // Arrange
        AuthUser user1 = mock(AuthUser.class, Answers.RETURNS_DEEP_STUBS);
        AuthUser user2 = mock(AuthUser.class, Answers.RETURNS_DEEP_STUBS);

        UUID id1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID id2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

        when(user1.id().value()).thenReturn(id1);
        when(user1.email().value()).thenReturn("user1@example.com");
        when(user1.fullName()).thenReturn("User One");
        when(user1.status()).thenReturn(AuthUserStatus.ACTIVE);
        when(user1.isEmailVerified()).thenReturn(true);

        Role role1 = mock(Role.class);
        when(role1.name()).thenReturn("AUTH_USER");
        when(user1.roles()).thenReturn(Set.of(role1));

        Permission perm1 = mock(Permission.class);
        when(perm1.name()).thenReturn("USER_READ");
        when(user1.directPermissions()).thenReturn(Set.of(perm1));

        when(user2.id().value()).thenReturn(id2);
        when(user2.email().value()).thenReturn("user2@example.com");
        when(user2.fullName()).thenReturn("User Two");
        when(user2.status()).thenReturn(AuthUserStatus.BLOCKED);
        when(user2.isEmailVerified()).thenReturn(false);

        when(user2.roles()).thenReturn(Set.of()); // vazio
        Permission perm2 = mock(Permission.class);
        when(perm2.name()).thenReturn("USER_WRITE");
        when(user2.directPermissions()).thenReturn(Set.of(perm2));

        List<AuthUser> input = Arrays.asList(
                user1,
                null,
                user2
        );

        // Act
        List<UserResponse> responses = UserMapper.toResponseList(input);

        // Assert
        assertEquals(2, responses.size());

        UserResponse r1 = responses.get(0);
        UserResponse r2 = responses.get(1);

        assertEquals(id1.toString(), r1.id());
        assertEquals("user1@example.com", r1.email());
        assertEquals("User One", r1.fullName());
        assertEquals(Set.of("AUTH_USER"), r1.roles());
        assertEquals(Set.of("USER_READ"), r1.permissions());

        assertEquals(id2.toString(), r2.id());
        assertEquals("user2@example.com", r2.email());
        assertEquals("User Two", r2.fullName());
        assertEquals(Set.of(), r2.roles());
        assertEquals(Set.of("USER_WRITE"), r2.permissions());

        // lista imutável
        assertThrows(UnsupportedOperationException.class,
                () -> responses.add(r1));

    }

}
