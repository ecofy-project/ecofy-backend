package br.com.ecofy.auth.core.domain;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RoleTest {

    @Test
    void shouldCreateRoleWithNormalizedNameAndPermissions() {
        Permission permission = new Permission("transactions:read", "Ler transações", "billing");

        Role role = new Role(
                "  ROLE_USER  ",
                "Usuário comum",
                Set.of(permission)
        );

        assertEquals("ROLE_USER", role.name());
        assertEquals("Usuário comum", role.description());
        assertEquals(1, role.permissions().size());
        assertTrue(role.permissions().contains(permission));
    }

    @Test
    void shouldCreateRoleWithEmptyPermissionsWhenPermissionsIsNull() {
        Role role = new Role(
                "ROLE_USER",
                null,
                null
        );

        assertEquals("ROLE_USER", role.name());
        assertNull(role.description());
        assertTrue(role.permissions().isEmpty());
    }

    @Test
    void shouldProtectInternalPermissionsFromExternalMutation() {
        Permission permission = new Permission("transactions:read", null, "billing");

        Set<Permission> permissions = new HashSet<>();
        permissions.add(permission);

        Role role = new Role(
                "ROLE_USER",
                "Usuário comum",
                permissions
        );

        permissions.clear();

        assertEquals(1, role.permissions().size());
        assertTrue(role.permissions().contains(permission));
    }

    @Test
    void shouldReturnUnmodifiablePermissions() {
        Permission permission = new Permission("transactions:read", null, "billing");

        Role role = new Role(
                "ROLE_USER",
                "Usuário comum",
                Set.of(permission)
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> role.permissions().add(new Permission("transactions:write", null, "billing"))
        );
    }

    @Test
    void shouldThrowExceptionWhenNameIsNull() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new Role(null, "Descrição", Set.of())
        );

        assertEquals("name must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenNameIsBlank() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Role("   ", "Descrição", Set.of())
        );

        assertEquals("name must not be blank", exception.getMessage());
    }

    @Test
    void shouldReturnTrueWhenHasExactPermission() {
        Permission permission = new Permission("transactions:read", null, "billing");

        Role role = new Role(
                "ROLE_USER",
                null,
                Set.of(permission)
        );

        assertTrue(role.hasExactPermission("transactions:read"));
    }

    @Test
    void shouldReturnFalseWhenDoesNotHaveExactPermission() {
        Permission permission = new Permission("transactions:read", null, "billing");

        Role role = new Role(
                "ROLE_USER",
                null,
                Set.of(permission)
        );

        assertFalse(role.hasExactPermission("transactions:write"));
    }

    @Test
    void shouldNotUseWildcardLogicInHasExactPermission() {
        Permission wildcardPermission = new Permission("transactions:*", null, "billing");

        Role role = new Role(
                "ROLE_USER",
                null,
                Set.of(wildcardPermission)
        );

        assertFalse(role.hasExactPermission("transactions:read"));
        assertTrue(role.hasExactPermission("transactions:*"));
    }

    @Test
    void shouldThrowExceptionWhenExactPermissionNameIsNull() {
        Role role = new Role(
                "ROLE_USER",
                null,
                Set.of(new Permission("transactions:read", null, "billing"))
        );

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> role.hasExactPermission(null)
        );

        assertEquals("permissionName must not be null", exception.getMessage());
    }

    @Test
    void shouldReturnTrueWhenHasPermissionByExactPermissionName() {
        Permission permission = new Permission("transactions:read", null, "*");

        Role role = new Role(
                "ROLE_USER",
                null,
                Set.of(permission)
        );

        assertTrue(role.hasPermission("transactions:read"));
    }

    @Test
    void shouldReturnTrueWhenHasPermissionByWildcardPermissionName() {
        Permission permission = new Permission("transactions:*", null, "*");

        Role role = new Role(
                "ROLE_USER",
                null,
                Set.of(permission)
        );

        assertTrue(role.hasPermission("transactions:read"));
        assertTrue(role.hasPermission("transactions:write"));
    }

    @Test
    void shouldReturnTrueWhenHasGlobalWildcardPermission() {
        Permission permission = new Permission("*", null, "*");

        Role role = new Role(
                "ROLE_ADMIN",
                null,
                Set.of(permission)
        );

        assertTrue(role.hasPermission("transactions:delete"));
        assertTrue(role.hasPermission("users:create"));
    }

    @Test
    void shouldReturnFalseWhenDoesNotHavePermission() {
        Permission permission = new Permission("transactions:read", null, "*");

        Role role = new Role(
                "ROLE_USER",
                null,
                Set.of(permission)
        );

        assertFalse(role.hasPermission("transactions:write"));
    }

    @Test
    void shouldThrowExceptionWhenPermissionNameIsNull() {
        Role role = new Role(
                "ROLE_USER",
                null,
                Set.of(new Permission("transactions:read", null, "*"))
        );

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> role.hasPermission(null)
        );

        assertEquals("permissionName must not be null", exception.getMessage());
    }

    @Test
    void shouldReturnTrueWhenRoleImpliesPermission() {
        Permission wildcardPermission = new Permission("transactions:*", null, "billing");
        Permission requestedPermission = new Permission("transactions:read", null, "billing");

        Role role = new Role(
                "ROLE_USER",
                null,
                Set.of(wildcardPermission)
        );

        assertTrue(role.implies(requestedPermission));
    }

    @Test
    void shouldReturnFalseWhenRoleDoesNotImplyPermission() {
        Permission permission = new Permission("transactions:read", null, "billing");
        Permission requestedPermission = new Permission("users:read", null, "auth");

        Role role = new Role(
                "ROLE_USER",
                null,
                Set.of(permission)
        );

        assertFalse(role.implies(requestedPermission));
    }

    @Test
    void shouldThrowExceptionWhenPermissionIsNullInImplies() {
        Role role = new Role(
                "ROLE_USER",
                null,
                Set.of(new Permission("transactions:read", null, "*"))
        );

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> role.implies(null)
        );

        assertEquals("permission must not be null", exception.getMessage());
    }

    @Test
    void shouldReturnNewRoleWithPermissionAddedWithoutChangingOriginalRole() {
        Permission readPermission = new Permission("transactions:read", null, "billing");
        Permission writePermission = new Permission("transactions:write", null, "billing");

        Role originalRole = new Role(
                "ROLE_USER",
                "Usuário comum",
                Set.of(readPermission)
        );

        Role newRole = originalRole.withPermission(writePermission);

        assertNotSame(originalRole, newRole);

        assertEquals("ROLE_USER", newRole.name());
        assertEquals("Usuário comum", newRole.description());

        assertTrue(originalRole.permissions().contains(readPermission));
        assertFalse(originalRole.permissions().contains(writePermission));
        assertEquals(1, originalRole.permissions().size());

        assertTrue(newRole.permissions().contains(readPermission));
        assertTrue(newRole.permissions().contains(writePermission));
        assertEquals(2, newRole.permissions().size());
    }

    @Test
    void shouldThrowExceptionWhenPermissionIsNullInWithPermission() {
        Role role = new Role(
                "ROLE_USER",
                null,
                Set.of(new Permission("transactions:read", null, "billing"))
        );

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> role.withPermission(null)
        );

        assertEquals("permission must not be null", exception.getMessage());
    }

    @Test
    void shouldReturnNewRoleWithoutPermissionWithoutChangingOriginalRole() {
        Permission readPermission = new Permission("transactions:read", null, "billing");
        Permission writePermission = new Permission("transactions:write", null, "billing");

        Role originalRole = new Role(
                "ROLE_USER",
                "Usuário comum",
                Set.of(readPermission, writePermission)
        );

        Role newRole = originalRole.withoutPermission(writePermission);

        assertNotSame(originalRole, newRole);

        assertEquals("ROLE_USER", newRole.name());
        assertEquals("Usuário comum", newRole.description());

        assertTrue(originalRole.permissions().contains(readPermission));
        assertTrue(originalRole.permissions().contains(writePermission));
        assertEquals(2, originalRole.permissions().size());

        assertTrue(newRole.permissions().contains(readPermission));
        assertFalse(newRole.permissions().contains(writePermission));
        assertEquals(1, newRole.permissions().size());
    }

    @Test
    void shouldReturnNewRoleEvenWhenRemovingNonExistingPermission() {
        Permission readPermission = new Permission("transactions:read", null, "billing");
        Permission writePermission = new Permission("transactions:write", null, "billing");

        Role originalRole = new Role(
                "ROLE_USER",
                "Usuário comum",
                Set.of(readPermission)
        );

        Role newRole = originalRole.withoutPermission(writePermission);

        assertNotSame(originalRole, newRole);
        assertEquals(originalRole.permissions(), newRole.permissions());
        assertEquals(1, newRole.permissions().size());
    }

    @Test
    void shouldThrowExceptionWhenPermissionIsNullInWithoutPermission() {
        Role role = new Role(
                "ROLE_USER",
                null,
                Set.of(new Permission("transactions:read", null, "billing"))
        );

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> role.withoutPermission(null)
        );

        assertEquals("permission must not be null", exception.getMessage());
    }

    @Test
    void shouldCompareRolesByNameOnly() {
        Role role = new Role(
                "ROLE_USER",
                "Descrição A",
                Set.of(new Permission("transactions:read", null, "billing"))
        );

        Role sameName = new Role(
                "ROLE_USER",
                "Descrição B",
                Set.of(new Permission("users:read", null, "auth"))
        );

        Role differentName = new Role(
                "ROLE_ADMIN",
                "Descrição A",
                Set.of(new Permission("transactions:read", null, "billing"))
        );

        assertEquals(role, role);
        assertEquals(role, sameName);
        assertNotEquals(role, differentName);
        assertNotEquals(role, null);
        assertNotEquals(role, "ROLE_USER");
    }

    @Test
    void shouldGenerateHashCodeUsingNameOnly() {
        Role role = new Role(
                "ROLE_USER",
                "Descrição A",
                Set.of(new Permission("transactions:read", null, "billing"))
        );

        Role sameName = new Role(
                "ROLE_USER",
                "Descrição B",
                Set.of(new Permission("users:read", null, "auth"))
        );

        assertEquals(role.hashCode(), sameName.hashCode());
    }

    @Test
    void shouldReturnToStringWithoutDescriptionOrPermissionDetails() {
        Role role = new Role(
                "ROLE_USER",
                "Descrição administrativa sensível",
                Set.of(
                        new Permission("transactions:read", "Permissão sensível A", "billing"),
                        new Permission("users:read", "Permissão sensível B", "auth")
                )
        );

        String result = role.toString();

        assertTrue(result.contains("Role{"));
        assertTrue(result.contains("name='ROLE_USER'"));
        assertTrue(result.contains("permissionsCount=2"));

        assertFalse(result.contains("Descrição administrativa sensível"));
        assertFalse(result.contains("transactions:read"));
        assertFalse(result.contains("users:read"));
        assertFalse(result.contains("Permissão sensível A"));
        assertFalse(result.contains("Permissão sensível B"));
    }
}