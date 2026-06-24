package br.com.ecofy.auth.core.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PermissionTest {

    @Test
    void shouldCreatePermissionWithNormalizedNameAndDomain() {
        Permission permission = new Permission(
                "  transactions:read  ",
                "Permite leitura de transações",
                "  AUTH  "
        );

        assertEquals("transactions:read", permission.name());
        assertEquals("Permite leitura de transações", permission.description());
        assertEquals("auth", permission.domain());
        assertFalse(permission.isWildcard());
        assertFalse(permission.isDomainWildcardName());
    }

    @Test
    void shouldUseWildcardDomainWhenDomainIsNull() {
        Permission permission = new Permission(
                "transactions:read",
                null,
                null
        );

        assertEquals("transactions:read", permission.name());
        assertNull(permission.description());
        assertEquals("*", permission.domain());
    }

    @Test
    void shouldUseWildcardDomainWhenDomainIsBlank() {
        Permission permission = new Permission(
                "transactions:read",
                null,
                "   "
        );

        assertEquals("*", permission.domain());
    }

    @Test
    void shouldThrowExceptionWhenNameIsNull() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new Permission(null, null, "auth")
        );

        assertEquals("name must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenNameIsBlank() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Permission("   ", null, "auth")
        );

        assertEquals("name must not be blank", exception.getMessage());
    }

    @Test
    void shouldIdentifyGlobalWildcardPermission() {
        Permission permission = new Permission("*", "Todas as permissões", "*");

        assertTrue(permission.isWildcard());
        assertFalse(permission.isDomainWildcardName());
    }

    @Test
    void shouldIdentifyDomainWildcardNamePermission() {
        Permission permission = new Permission("transactions:*", null, "billing");

        assertFalse(permission.isWildcard());
        assertTrue(permission.isDomainWildcardName());
    }

    @Test
    void shouldNotIdentifyInvalidShortWildcardNameAsDomainWildcardName() {
        Permission permission = new Permission(":*", null, "billing");

        assertFalse(permission.isWildcard());
        assertFalse(permission.isDomainWildcardName());
    }

    @Test
    void shouldReturnTrueWhenGlobalWildcardImpliesAnyPermission() {
        Permission wildcard = new Permission("*", null, "*");
        Permission required = new Permission("transactions:delete", null, "billing");

        assertTrue(wildcard.implies(required));
    }

    @Test
    void shouldReturnFalseWhenDomainsAreSpecificAndDifferent() {
        Permission current = new Permission("transactions:read", null, "billing");
        Permission required = new Permission("transactions:read", null, "auth");

        assertFalse(current.implies(required));
    }

    @Test
    void shouldReturnTrueWhenPermissionNamesAreEqualAndDomainsAreEqualIgnoringCase() {
        Permission current = new Permission("transactions:read", null, "BILLING");
        Permission required = new Permission("transactions:read", null, "billing");

        assertTrue(current.implies(required));
    }

    @Test
    void shouldReturnTrueWhenCurrentDomainIsWildcardAndPermissionNamesAreEqual() {
        Permission current = new Permission("transactions:read", null, "*");
        Permission required = new Permission("transactions:read", null, "billing");

        assertTrue(current.implies(required));
    }

    @Test
    void shouldReturnTrueWhenOtherDomainIsWildcardAndPermissionNamesAreEqual() {
        Permission current = new Permission("transactions:read", null, "billing");
        Permission required = new Permission("transactions:read", null, "*");

        assertTrue(current.implies(required));
    }

    @Test
    void shouldReturnTrueWhenDomainWildcardNameMatchesPermissionPrefix() {
        Permission current = new Permission("transactions:*", null, "billing");
        Permission required = new Permission("transactions:read", null, "billing");

        assertTrue(current.implies(required));
    }

    @Test
    void shouldReturnFalseWhenDomainWildcardNameDoesNotMatchPermissionPrefix() {
        Permission current = new Permission("transactions:*", null, "billing");
        Permission required = new Permission("users:read", null, "billing");

        assertFalse(current.implies(required));
    }

    @Test
    void shouldReturnFalseWhenNameIsDifferentAndPermissionIsNotWildcard() {
        Permission current = new Permission("transactions:read", null, "billing");
        Permission required = new Permission("transactions:write", null, "billing");

        assertFalse(current.implies(required));
    }

    @Test
    void shouldThrowExceptionWhenOtherPermissionIsNull() {
        Permission permission = new Permission("transactions:read", null, "billing");

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> permission.implies((Permission) null)
        );

        assertEquals("other must not be null", exception.getMessage());
    }

    @Test
    void shouldImplyPermissionByRawNameUsingCurrentDomain() {
        Permission permission = new Permission("transactions:*", null, "billing");

        assertTrue(permission.implies("transactions:read"));
    }

    @Test
    void shouldThrowExceptionWhenRawPermissionNameIsNull() {
        Permission permission = new Permission("transactions:*", null, "billing");

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> permission.implies((String) null)
        );

        assertEquals("name must not be null", exception.getMessage());
    }

    @Test
    void shouldComparePermissionsByNameOnly() {
        Permission permission = new Permission("transactions:read", "Descrição A", "billing");
        Permission sameName = new Permission("transactions:read", "Descrição B", "auth");
        Permission differentName = new Permission("transactions:write", "Descrição A", "billing");

        assertEquals(permission, permission);
        assertEquals(permission, sameName);
        assertNotEquals(permission, differentName);
        assertNotEquals(permission, null);
        assertNotEquals(permission, "transactions:read");
    }

    @Test
    void shouldGenerateHashCodeUsingNameOnly() {
        Permission permission = new Permission("transactions:read", "Descrição A", "billing");
        Permission sameName = new Permission("transactions:read", "Descrição B", "auth");

        assertEquals(permission.hashCode(), sameName.hashCode());
    }

    @Test
    void shouldReturnToStringWithoutDescription() {
        Permission permission = new Permission(
                "transactions:read",
                "Descrição sensível ou longa",
                "Billing"
        );

        String result = permission.toString();

        assertTrue(result.contains("Permission{"));
        assertTrue(result.contains("name='transactions:read'"));
        assertTrue(result.contains("domain='billing'"));

        assertFalse(result.contains("Descrição sensível ou longa"));
        assertFalse(result.contains("description"));
    }
}