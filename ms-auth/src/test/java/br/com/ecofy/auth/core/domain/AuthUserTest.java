package br.com.ecofy.auth.core.domain;

import br.com.ecofy.auth.core.domain.enums.AuthUserStatus;
import br.com.ecofy.auth.core.domain.valueobject.AuthUserId;
import br.com.ecofy.auth.core.domain.valueobject.EmailAddress;
import br.com.ecofy.auth.core.domain.valueobject.PasswordHash;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthUserTest {

    private final AuthUserId id = mock(AuthUserId.class);
    private final EmailAddress email = mock(EmailAddress.class);
    private final PasswordHash passwordHash = mock(PasswordHash.class);

    @Test
    void shouldReconstructAuthUserWithAllFields() {
        Role role = mock(Role.class);
        Permission permission = mock(Permission.class);
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T10:00:00Z");
        Instant lastLoginAt = Instant.parse("2026-01-03T10:00:00Z");

        AuthUser user = new AuthUser(
                id,
                email,
                passwordHash,
                AuthUserStatus.ACTIVE,
                true,
                "Matheus",
                "Lemes",
                "en-US",
                Set.of(role),
                Set.of(permission),
                createdAt,
                updatedAt,
                lastLoginAt,
                2
        );

        assertSame(id, user.id());
        assertSame(email, user.email());
        assertSame(passwordHash, user.passwordHash());
        assertEquals(AuthUserStatus.ACTIVE, user.status());
        assertTrue(user.isEmailVerified());
        assertEquals("Matheus", user.firstName());
        assertEquals("Lemes", user.lastName());
        assertEquals("en-US", user.locale());
        assertEquals(createdAt, user.createdAt());
        assertEquals(updatedAt, user.updatedAt());
        assertEquals(lastLoginAt, user.lastLoginAt());
        assertEquals(2, user.failedLoginAttempts());
        assertTrue(user.roles().contains(role));
        assertTrue(user.directPermissions().contains(permission));
    }

    @Test
    void shouldUseDefaultLocaleWhenLocaleIsNullOrBlank() {
        AuthUser userWithNullLocale = new AuthUser(
                id,
                email,
                passwordHash,
                AuthUserStatus.ACTIVE,
                false,
                "Matheus",
                "Lemes",
                null,
                null,
                null,
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T10:00:00Z"),
                null,
                0
        );

        AuthUser userWithBlankLocale = new AuthUser(
                id,
                email,
                passwordHash,
                AuthUserStatus.ACTIVE,
                false,
                "Matheus",
                "Lemes",
                "   ",
                null,
                null,
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T10:00:00Z"),
                null,
                0
        );

        assertEquals("pt-BR", userWithNullLocale.locale());
        assertEquals("pt-BR", userWithBlankLocale.locale());
    }

    @Test
    void shouldInitializeEmptyCollectionsWhenRolesAndPermissionsAreNull() {
        AuthUser user = baseUser(
                AuthUserStatus.ACTIVE,
                false,
                null,
                null,
                0
        );

        assertTrue(user.roles().isEmpty());
        assertTrue(user.directPermissions().isEmpty());
    }

    @Test
    void shouldProtectInternalCollectionsFromExternalMutation() {
        Role role = mock(Role.class);
        Permission permission = mock(Permission.class);

        Set<Role> roles = new HashSet<>();
        roles.add(role);

        Set<Permission> permissions = new HashSet<>();
        permissions.add(permission);

        AuthUser user = new AuthUser(
                id,
                email,
                passwordHash,
                AuthUserStatus.ACTIVE,
                false,
                "Matheus",
                "Lemes",
                "pt-BR",
                roles,
                permissions,
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T10:00:00Z"),
                null,
                0
        );

        roles.clear();
        permissions.clear();

        assertEquals(1, user.roles().size());
        assertEquals(1, user.directPermissions().size());
        assertTrue(user.roles().contains(role));
        assertTrue(user.directPermissions().contains(permission));
    }

    @Test
    void shouldReturnUnmodifiableRolesAndDirectPermissions() {
        AuthUser user = baseUser(
                AuthUserStatus.ACTIVE,
                false,
                Set.of(mock(Role.class)),
                Set.of(mock(Permission.class)),
                0
        );

        assertThrows(UnsupportedOperationException.class, () -> user.roles().add(mock(Role.class)));
        assertThrows(UnsupportedOperationException.class, () -> user.directPermissions().add(mock(Permission.class)));
    }

    @Test
    void shouldNormalizeNegativeFailedLoginAttemptsToZero() {
        AuthUser user = baseUser(
                AuthUserStatus.ACTIVE,
                false,
                null,
                null,
                -10
        );

        assertEquals(0, user.failedLoginAttempts());
    }

    @Test
    void shouldCreateNewPendingUserWithDefaultValues() {
        Role role = mock(Role.class);

        AuthUser user = AuthUser.newPendingUser(
                email,
                passwordHash,
                "Matheus",
                "Lemes",
                "   ",
                Set.of(role)
        );

        assertNotNull(user.id());
        assertSame(email, user.email());
        assertSame(passwordHash, user.passwordHash());
        assertEquals(AuthUserStatus.PENDING_EMAIL_CONFIRMATION, user.status());
        assertFalse(user.isEmailVerified());
        assertEquals("Matheus", user.firstName());
        assertEquals("Lemes", user.lastName());
        assertEquals("pt-BR", user.locale());
        assertTrue(user.roles().contains(role));
        assertTrue(user.directPermissions().isEmpty());
        assertNotNull(user.createdAt());
        assertNotNull(user.updatedAt());
        assertNull(user.lastLoginAt());
        assertEquals(0, user.failedLoginAttempts());
    }

    @Test
    void shouldCreateNewPendingUserWithEmptyRolesWhenRolesAreNull() {
        AuthUser user = AuthUser.newPendingUser(
                email,
                passwordHash,
                "Matheus",
                "Lemes",
                "en-US",
                null
        );

        assertEquals("en-US", user.locale());
        assertTrue(user.roles().isEmpty());
        assertTrue(user.directPermissions().isEmpty());
    }

    @Test
    void shouldReturnFullNameTrimmed() {
        AuthUser user = new AuthUser(
                id,
                email,
                passwordHash,
                AuthUserStatus.ACTIVE,
                true,
                "  Matheus  ",
                "  Lemes  ",
                "pt-BR",
                null,
                null,
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T10:00:00Z"),
                null,
                0
        );

        assertEquals("Matheus Lemes", user.fullName());
    }

    @Test
    void shouldReturnEmptyFullNameWhenFirstNameAndLastNameAreNull() {
        AuthUser user = new AuthUser(
                id,
                email,
                passwordHash,
                AuthUserStatus.ACTIVE,
                true,
                null,
                null,
                "pt-BR",
                null,
                null,
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T10:00:00Z"),
                null,
                0
        );

        assertEquals("", user.fullName());
    }

    @Test
    void shouldConfirmEmailAndActivatePendingUser() {
        AuthUser user = baseUser(
                AuthUserStatus.PENDING_EMAIL_CONFIRMATION,
                false,
                null,
                null,
                0
        );

        Instant previousUpdatedAt = user.updatedAt();

        user.confirmEmail();

        assertTrue(user.isEmailVerified());
        assertEquals(AuthUserStatus.ACTIVE, user.status());
        assertTrue(user.updatedAt().isAfter(previousUpdatedAt));
    }

    @Test
    void shouldConfirmEmailWithoutChangingStatusWhenUserIsAlreadyActive() {
        AuthUser user = baseUser(
                AuthUserStatus.ACTIVE,
                false,
                null,
                null,
                0
        );

        user.confirmEmail();

        assertTrue(user.isEmailVerified());
        assertEquals(AuthUserStatus.ACTIVE, user.status());
    }

    @Test
    void shouldThrowExceptionWhenBlockedUserTriesToConfirmEmail() {
        AuthUser user = baseUser(
                AuthUserStatus.BLOCKED,
                false,
                null,
                null,
                0
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                user::confirmEmail
        );

        assertEquals("User is not eligible to confirm email", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenDeletedUserTriesToConfirmEmail() {
        AuthUser user = baseUser(
                AuthUserStatus.DELETED,
                false,
                null,
                null,
                0
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                user::confirmEmail
        );

        assertEquals("User is not eligible to confirm email", exception.getMessage());
    }

    @Test
    void shouldChangePasswordAndResetFailedAttempts() {
        PasswordHash newPasswordHash = mock(PasswordHash.class);

        AuthUser user = baseUser(
                AuthUserStatus.ACTIVE,
                true,
                null,
                null,
                3
        );

        Instant previousUpdatedAt = user.updatedAt();

        user.changePassword(newPasswordHash);

        assertSame(newPasswordHash, user.passwordHash());
        assertEquals(0, user.failedLoginAttempts());
        assertTrue(user.updatedAt().isAfter(previousUpdatedAt));
    }

    @Test
    void shouldThrowExceptionWhenChangingPasswordToNull() {
        AuthUser user = baseUser(
                AuthUserStatus.ACTIVE,
                true,
                null,
                null,
                0
        );

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> user.changePassword(null)
        );

        assertEquals("newPasswordHash must not be null", exception.getMessage());
    }

    @Test
    void shouldRegisterSuccessfulLogin() {
        AuthUser user = baseUser(
                AuthUserStatus.ACTIVE,
                true,
                null,
                null,
                4
        );

        Instant previousUpdatedAt = user.updatedAt();

        user.registerSuccessfulLogin();

        assertEquals(0, user.failedLoginAttempts());
        assertNotNull(user.lastLoginAt());
        assertTrue(user.updatedAt().isAfter(previousUpdatedAt));
    }

    @Test
    void shouldRegisterFailedLoginWithoutLockingBeforeLimit() {
        AuthUser user = baseUser(
                AuthUserStatus.ACTIVE,
                true,
                null,
                null,
                0
        );

        user.registerFailedLogin(3);

        assertEquals(1, user.failedLoginAttempts());
        assertEquals(AuthUserStatus.ACTIVE, user.status());
    }

    @Test
    void shouldLockUserWhenFailedLoginAttemptsReachLimit() {
        AuthUser user = baseUser(
                AuthUserStatus.ACTIVE,
                true,
                null,
                null,
                2
        );

        user.registerFailedLogin(3);

        assertEquals(3, user.failedLoginAttempts());
        assertEquals(AuthUserStatus.LOCKED, user.status());
    }

    @Test
    void shouldThrowExceptionWhenMaxAttemptsBeforeLockIsZeroOrNegative() {
        AuthUser user = baseUser(
                AuthUserStatus.ACTIVE,
                true,
                null,
                null,
                0
        );

        IllegalArgumentException zeroException = assertThrows(
                IllegalArgumentException.class,
                () -> user.registerFailedLogin(0)
        );

        IllegalArgumentException negativeException = assertThrows(
                IllegalArgumentException.class,
                () -> user.registerFailedLogin(-1)
        );

        assertEquals("maxAttemptsBeforeLock must be greater than zero", zeroException.getMessage());
        assertEquals("maxAttemptsBeforeLock must be greater than zero", negativeException.getMessage());
    }

    @Test
    void shouldReturnTrueWhenRoleImpliesPermission() {
        Role role = mock(Role.class);
        when(role.implies(any(Permission.class))).thenReturn(true);

        AuthUser user = baseUser(
                AuthUserStatus.ACTIVE,
                true,
                Set.of(role),
                Set.of(),
                0
        );

        assertTrue(user.hasPermission("USER_READ"));
        verify(role).implies(any(Permission.class));
    }

    @Test
    void shouldReturnTrueWhenDirectPermissionImpliesPermission() {
        Role role = mock(Role.class);
        Permission directPermission = mock(Permission.class);

        when(role.implies(any(Permission.class))).thenReturn(false);
        when(directPermission.implies(any(Permission.class))).thenReturn(true);

        AuthUser user = baseUser(
                AuthUserStatus.ACTIVE,
                true,
                Set.of(role),
                Set.of(directPermission),
                0
        );

        assertTrue(user.hasPermission("USER_WRITE"));
        verify(role).implies(any(Permission.class));
        verify(directPermission).implies(any(Permission.class));
    }

    @Test
    void shouldReturnFalseWhenNoRoleOrDirectPermissionImpliesPermission() {
        Role role = mock(Role.class);
        Permission directPermission = mock(Permission.class);

        when(role.implies(any(Permission.class))).thenReturn(false);
        when(directPermission.implies(any(Permission.class))).thenReturn(false);

        AuthUser user = baseUser(
                AuthUserStatus.ACTIVE,
                true,
                Set.of(role),
                Set.of(directPermission),
                0
        );

        assertFalse(user.hasPermission("ADMIN_DELETE"));
    }

    @Test
    void shouldAddRole() {
        AuthUser user = baseUser(
                AuthUserStatus.ACTIVE,
                true,
                null,
                null,
                0
        );

        Role role = mock(Role.class);

        user.addRole(role);

        assertTrue(user.roles().contains(role));
    }

    @Test
    void shouldThrowExceptionWhenAddingNullRole() {
        AuthUser user = baseUser(
                AuthUserStatus.ACTIVE,
                true,
                null,
                null,
                0
        );

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> user.addRole(null)
        );

        assertEquals("role must not be null", exception.getMessage());
    }

    @Test
    void shouldAddDirectPermission() {
        AuthUser user = baseUser(
                AuthUserStatus.ACTIVE,
                true,
                null,
                null,
                0
        );

        Permission permission = mock(Permission.class);

        user.addDirectPermission(permission);

        assertTrue(user.directPermissions().contains(permission));
    }

    @Test
    void shouldThrowExceptionWhenAddingNullDirectPermission() {
        AuthUser user = baseUser(
                AuthUserStatus.ACTIVE,
                true,
                null,
                null,
                0
        );

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> user.addDirectPermission(null)
        );

        assertEquals("permission must not be null", exception.getMessage());
    }

    @Test
    void shouldBlockUser() {
        AuthUser user = baseUser(
                AuthUserStatus.ACTIVE,
                true,
                null,
                null,
                0
        );

        user.block();

        assertEquals(AuthUserStatus.BLOCKED, user.status());
    }

    @Test
    void shouldDeleteUser() {
        AuthUser user = baseUser(
                AuthUserStatus.ACTIVE,
                true,
                null,
                null,
                0
        );

        user.delete();

        assertEquals(AuthUserStatus.DELETED, user.status());
    }

    @Test
    void shouldThrowExceptionWhenRequiredConstructorArgumentsAreNull() {
        Instant now = Instant.parse("2026-01-01T10:00:00Z");

        assertThrows(
                NullPointerException.class,
                () -> new AuthUser(
                        null,
                        email,
                        passwordHash,
                        AuthUserStatus.ACTIVE,
                        false,
                        "Matheus",
                        "Lemes",
                        "pt-BR",
                        null,
                        null,
                        now,
                        now,
                        null,
                        0
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> new AuthUser(
                        id,
                        null,
                        passwordHash,
                        AuthUserStatus.ACTIVE,
                        false,
                        "Matheus",
                        "Lemes",
                        "pt-BR",
                        null,
                        null,
                        now,
                        now,
                        null,
                        0
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> new AuthUser(
                        id,
                        email,
                        null,
                        AuthUserStatus.ACTIVE,
                        false,
                        "Matheus",
                        "Lemes",
                        "pt-BR",
                        null,
                        null,
                        now,
                        now,
                        null,
                        0
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> new AuthUser(
                        id,
                        email,
                        passwordHash,
                        null,
                        false,
                        "Matheus",
                        "Lemes",
                        "pt-BR",
                        null,
                        null,
                        now,
                        now,
                        null,
                        0
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> new AuthUser(
                        id,
                        email,
                        passwordHash,
                        AuthUserStatus.ACTIVE,
                        false,
                        "Matheus",
                        "Lemes",
                        "pt-BR",
                        null,
                        null,
                        null,
                        now,
                        null,
                        0
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> new AuthUser(
                        id,
                        email,
                        passwordHash,
                        AuthUserStatus.ACTIVE,
                        false,
                        "Matheus",
                        "Lemes",
                        "pt-BR",
                        null,
                        null,
                        now,
                        null,
                        null,
                        0
                )
        );
    }

    private AuthUser baseUser(AuthUserStatus status,
                              boolean emailVerified,
                              Set<Role> roles,
                              Set<Permission> directPermissions,
                              int failedLoginAttempts) {
        return new AuthUser(
                id,
                email,
                passwordHash,
                status,
                emailVerified,
                "Matheus",
                "Lemes",
                "pt-BR",
                roles,
                directPermissions,
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T10:00:00Z"),
                null,
                failedLoginAttempts
        );
    }
}