package br.com.ecofy.auth.adapters.out.persistence;

import br.com.ecofy.auth.adapters.out.persistence.entity.*;
import br.com.ecofy.auth.adapters.out.persistence.mapper.PersistenceMapper;
import br.com.ecofy.auth.core.domain.*;
import br.com.ecofy.auth.core.domain.enums.AuthUserStatus;
import br.com.ecofy.auth.core.domain.enums.ClientType;
import br.com.ecofy.auth.core.domain.enums.GrantType;
import br.com.ecofy.auth.core.domain.enums.TokenType;
import br.com.ecofy.auth.core.domain.valueobject.AuthUserId;
import br.com.ecofy.auth.core.domain.valueobject.EmailAddress;
import br.com.ecofy.auth.core.domain.valueobject.PasswordHash;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PersistenceMapperTest {

    @Test
    void constructor_shouldThrowAssertionError() throws Exception {
        Constructor<PersistenceMapper> c = PersistenceMapper.class.getDeclaredConstructor();
        c.setAccessible(true);

        AssertionError ex = assertThrows(AssertionError.class, () -> {
            try {
                c.newInstance();
            } catch (InvocationTargetException ite) {
                Throwable target = ite.getTargetException();
                if (target instanceof AssertionError ae) throw ae;
                if (target instanceof RuntimeException re) throw re;
                throw new RuntimeException(target);
            }
        });

        assertEquals("PersistenceMapper is a utility class and should not be instantiated", ex.getMessage());
    }

    @Test
    void toEntity_authUser_shouldRejectNull() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> PersistenceMapper.toEntity((AuthUser) null));
        assertEquals("user must not be null", ex.getMessage());
    }

    @Test
    void toDomain_authUser_shouldRejectNullEntity() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> PersistenceMapper.toDomain(null, Set.of(), Set.of()));
        assertEquals("AuthUserEntity must not be null", ex.getMessage());
    }

    @Test
    void toDomain_role_shouldRejectNull() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> PersistenceMapper.toDomain((RoleEntity) null));
        assertEquals("RoleEntity must not be null", ex.getMessage());
    }

    @Test
    void toDomain_permission_shouldRejectNull() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> PersistenceMapper.toDomain((PermissionEntity) null));
        assertEquals("PermissionEntity must not be null", ex.getMessage());
    }

    @Test
    void toDomain_clientApplication_shouldRejectNull() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> PersistenceMapper.toDomain((ClientApplicationEntity) null));
        assertEquals("ClientApplicationEntity must not be null", ex.getMessage());
    }

    @Test
    void toEntity_clientApplication_shouldRejectNull() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> PersistenceMapper.toEntity((ClientApplication) null));
        assertEquals("clientApplication must not be null", ex.getMessage());
    }

    @Test
    void toDomain_refreshToken_shouldRejectNull() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> PersistenceMapper.toDomain((RefreshTokenEntity) null));
        assertEquals("RefreshTokenEntity must not be null", ex.getMessage());
    }

    @Test
    void toEntity_refreshToken_shouldRejectNull() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> PersistenceMapper.toEntity((RefreshToken) null));
        assertEquals("refreshToken must not be null", ex.getMessage());
    }

    @Test
    void toDomain_jwk_shouldRejectNull() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> PersistenceMapper.toDomain((JwkKeyEntity) null));
        assertEquals("JwkKeyEntity must not be null", ex.getMessage());
    }

    @Test
    void toEntity_authUser_shouldMapAllScalarFields() {
        UUID id = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2024-01-02T00:00:00Z");
        Instant lastLoginAt = Instant.parse("2024-01-03T00:00:00Z");

        AuthUser user = mock(AuthUser.class);

        AuthUserId authUserId = mock(AuthUserId.class);
        when(authUserId.value()).thenReturn(id);

        EmailAddress email = mock(EmailAddress.class);
        when(email.value()).thenReturn("u@ecofy.com");

        PasswordHash ph = mock(PasswordHash.class);
        when(ph.value()).thenReturn("hash");

        when(user.id()).thenReturn(authUserId);
        when(user.email()).thenReturn(email);
        when(user.passwordHash()).thenReturn(ph);
        when(user.status()).thenReturn(AuthUserStatus.ACTIVE);
        when(user.isEmailVerified()).thenReturn(true);
        when(user.firstName()).thenReturn("Matheus");
        when(user.lastName()).thenReturn("Silva");
        when(user.locale()).thenReturn("pt-BR");
        when(user.createdAt()).thenReturn(createdAt);
        when(user.updatedAt()).thenReturn(updatedAt);
        when(user.lastLoginAt()).thenReturn(lastLoginAt);
        when(user.failedLoginAttempts()).thenReturn(3);

        AuthUserEntity e = PersistenceMapper.toEntity(user);

        assertEquals(id, e.getId());
        assertEquals("u@ecofy.com", e.getEmail());
        assertEquals("hash", e.getPasswordHash());
        assertEquals(AuthUserStatus.ACTIVE, e.getStatus());
        assertTrue(e.isEmailVerified());
        assertEquals("Matheus", e.getFirstName());
        assertEquals("Silva", e.getLastName());
        assertEquals("pt-BR", e.getLocale());
        assertEquals(createdAt, e.getCreatedAt());
        assertEquals(updatedAt, e.getUpdatedAt());
        assertEquals(lastLoginAt, e.getLastLoginAt());
        assertEquals(3, e.getFailedLoginAttempts());
    }

    @Test
    void toDomain_authUser_shouldHandleNullRoleAndPermSets_asEmpty_andMapScalars() {
        UUID id = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

        AuthUserEntity e = new AuthUserEntity();
        e.setId(id);
        e.setEmail("x@ecofy.com");
        e.setPasswordHash("ph");
        e.setStatus(AuthUserStatus.ACTIVE);
        e.setEmailVerified(false);
        e.setFirstName("A");
        e.setLastName("B");
        e.setLocale("pt-BR");
        e.setCreatedAt(Instant.parse("2024-02-01T00:00:00Z"));
        e.setUpdatedAt(Instant.parse("2024-02-02T00:00:00Z"));
        e.setLastLoginAt(null);
        e.setFailedLoginAttempts(0);

        AuthUser d = PersistenceMapper.toDomain(e, null, null);

        Object idVo = callNoArg(d, "id");
        Object uuid = callNoArg(idVo, "value");
        assertEquals(id, uuid);

        Object emailVo = callNoArg(d, "email");
        Object emailValue = callNoArg(emailVo, "value");
        assertEquals("x@ecofy.com", emailValue);

        Object phVo = callNoArg(d, "passwordHash");
        Object phValue = callNoArg(phVo, "value");
        assertEquals("ph", phValue);

        assertEquals(AuthUserStatus.ACTIVE, callNoArg(d, "status"));
        assertEquals(false, callNoArg(d, "isEmailVerified"));
        assertEquals("A", callNoArg(d, "firstName"));
        assertEquals("B", callNoArg(d, "lastName"));
        assertEquals("pt-BR", callNoArg(d, "locale"));
        assertEquals(e.getCreatedAt(), callNoArg(d, "createdAt"));
        assertEquals(e.getUpdatedAt(), callNoArg(d, "updatedAt"));
        assertNull(callNoArg(d, "lastLoginAt"));
        assertEquals(0, callNoArg(d, "failedLoginAttempts"));

        Set<?> roles = (Set<?>) callAnyNoArg(d, "roles", "getRoles", "authorities", "getAuthorities");
        assertNotNull(roles);
        assertTrue(roles.isEmpty());

        Set<?> perms = extractPermissionsFromAuthUser(d);
        assertNotNull(perms);
        assertTrue(perms.isEmpty());
    }


    @Test
    void toDomain_authUser_shouldFilterNulls_andReturnUnmodifiableSets_forRolesAndPermissions() {
        UUID id = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

        PermissionEntity p1 = new PermissionEntity();
        p1.setName("perm.read");
        p1.setDescription("Read");
        p1.setDomain("auth");

        PermissionEntity p2 = new PermissionEntity();
        p2.setName("perm.write");
        p2.setDescription("Write");
        p2.setDomain("auth");

        Set<PermissionEntity> rolePerms = new java.util.HashSet<>();
        rolePerms.add(p1);
        rolePerms.add(null);
        rolePerms.add(p2);

        RoleEntity r1 = new RoleEntity();
        r1.setName("ROLE_ADMIN");
        r1.setDescription("Admin");
        r1.setPermissions(rolePerms);

        AuthUserEntity e = new AuthUserEntity();
        e.setId(id);
        e.setEmail("y@ecofy.com");
        e.setPasswordHash("ph2");
        e.setStatus(AuthUserStatus.ACTIVE);
        e.setEmailVerified(true);
        e.setFirstName("C");
        e.setLastName("D");
        e.setLocale("pt-BR");
        e.setCreatedAt(Instant.parse("2024-03-01T00:00:00Z"));
        e.setUpdatedAt(Instant.parse("2024-03-02T00:00:00Z"));
        e.setLastLoginAt(Instant.parse("2024-03-03T00:00:00Z"));
        e.setFailedLoginAttempts(1);

        Set<RoleEntity> roleEntities = new java.util.HashSet<>();
        roleEntities.add(r1);
        roleEntities.add(null);

        Set<PermissionEntity> permEntities = new java.util.HashSet<>();
        permEntities.add(p2);
        permEntities.add(null);

        AuthUser d = PersistenceMapper.toDomain(e, roleEntities, permEntities);

        Set<?> roles = (Set<?>) callAnyNoArg(d, "roles", "authorities");
        assertEquals(1, roles.size());
        assertThrows(UnsupportedOperationException.class, () -> ((Set<Object>) roles).add(new Object()));

        Set<?> perms = extractPermissionsFromAuthUser(d);
        assertNotNull(perms);
        assertFalse(perms.isEmpty());

        assertThrows(UnsupportedOperationException.class, () -> ((Set<Object>) perms).add(new Object()));
    }

    @Test
    void toDomain_role_shouldHandleNullPermissions_asEmpty_andReturnUnmodifiableSet() {
        RoleEntity r = new RoleEntity();
        r.setName("ROLE_USER");
        r.setDescription("User");
        r.setPermissions(null);

        Role d = PersistenceMapper.toDomain(r);

        assertEquals("ROLE_USER", callAnyNoArg(d, "name", "getName"));
        assertEquals("User", callAnyNoArg(d, "description", "getDescription"));

        Set<?> perms = (Set<?>) callAnyNoArg(d, "permissions", "perms", "getPermissions");
        assertNotNull(perms);
        assertTrue(perms.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> ((Set<Object>) perms).add(new Object()));
    }

    @Test
    void toDomain_permission_shouldMapAllFields() {
        PermissionEntity p = new PermissionEntity();
        p.setName("perm.x");
        p.setDescription("DX");
        p.setDomain("auth");

        Permission d = PersistenceMapper.toDomain(p);

        assertEquals("perm.x", callAnyNoArg(d, "name", "getName"));
        assertEquals("DX", callAnyNoArg(d, "description", "getDescription"));
        assertEquals("auth", callAnyNoArg(d, "domain", "getDomain"));
    }

    @Test
    void toDomain_clientApplication_shouldSanitizeRedirectUrisAndScopes_whenSetsHaveNullOrBlank() {
        Set<String> redirectUris = new java.util.HashSet<>();
        redirectUris.add("  https://a/cb  ");
        redirectUris.add("");
        redirectUris.add("   ");
        redirectUris.add(null);
        redirectUris.add("https://b/cb");

        Set<String> scopes = new java.util.HashSet<>();
        scopes.add("  openid  ");
        scopes.add("");
        scopes.add("   ");
        scopes.add(null);
        scopes.add("profile");

        ClientApplicationEntity e = ClientApplicationEntity.builder()
                .id("10")
                .clientId("client-1")
                .clientSecretHash("secret")
                .name("App")
                .clientType(ClientType.values()[0])
                .grantTypes(null)
                .redirectUris(redirectUris)
                .scopes(scopes)
                .firstParty(true)
                .active(true)
                .createdAt(Instant.parse("2024-04-01T00:00:00Z"))
                .updatedAt(Instant.parse("2024-04-02T00:00:00Z"))
                .build();

        ClientApplication d = PersistenceMapper.toDomain(e);

        assertEquals("10", String.valueOf(callAnyNoArg(d, "id", "getId")));
        assertEquals("client-1", callAnyNoArg(d, "clientId", "getClientId"));
        assertEquals("secret", callAnyNoArg(d, "clientSecretHash", "getClientSecretHash"));
        assertEquals("App", callAnyNoArg(d, "name", "getName"));
        assertEquals(e.getClientType(), callAnyNoArg(d, "clientType", "getClientType"));

        Set<?> mappedRedirectUris = (Set<?>) callAnyNoArg(d, "redirectUris", "getRedirectUris");
        Set<?> mappedScopes = (Set<?>) callAnyNoArg(d, "scopes", "getScopes");
        Set<?> grantTypes = (Set<?>) callAnyNoArg(d, "grantTypes", "getGrantTypes");

        assertEquals(Set.of("https://a/cb", "https://b/cb"), mappedRedirectUris);
        assertEquals(Set.of("openid", "profile"), mappedScopes);
        assertNotNull(grantTypes);
        assertTrue(grantTypes.isEmpty());

        assertThrows(UnsupportedOperationException.class, () -> ((Set<Object>) mappedRedirectUris).add(new Object()));
        assertThrows(UnsupportedOperationException.class, () -> ((Set<Object>) mappedScopes).add(new Object()));
        assertThrows(UnsupportedOperationException.class, () -> ((Set<Object>) grantTypes).add(new Object()));
    }


    @Test
    void toEntity_clientApplication_shouldMapAllFields_usingMocks() {
        ClientApplication d = mock(ClientApplication.class);

        when(d.id()).thenReturn("20");
        when(d.clientId()).thenReturn("client-2");
        when(d.clientSecretHash()).thenReturn("sec2");
        when(d.name()).thenReturn("App2");
        when(d.clientType()).thenReturn(ClientType.values()[0]);
        when(d.grantTypes()).thenReturn(Set.of());
        when(d.redirectUris()).thenReturn(Set.of("https://cb"));
        when(d.scopes()).thenReturn(Set.of("openid"));
        when(d.isFirstParty()).thenReturn(false);
        when(d.isActive()).thenReturn(true);
        when(d.createdAt()).thenReturn(Instant.parse("2024-05-01T00:00:00Z"));
        when(d.updatedAt()).thenReturn(Instant.parse("2024-05-02T00:00:00Z"));

        ClientApplicationEntity e = PersistenceMapper.toEntity(d);

        assertEquals("20", e.getId());
        assertEquals("client-2", e.getClientId());
        assertEquals("sec2", e.getClientSecretHash());
        assertEquals("App2", e.getName());
        assertEquals(d.clientType(), e.getClientType());
        assertEquals(Set.of(), e.getGrantTypes());
        assertEquals(Set.of("https://cb"), e.getRedirectUris());
        assertEquals(Set.of("openid"), e.getScopes());
        assertFalse(e.isFirstParty());
        assertTrue(e.isActive());
        assertEquals(Instant.parse("2024-05-01T00:00:00Z"), e.getCreatedAt());
        assertEquals(Instant.parse("2024-05-02T00:00:00Z"), e.getUpdatedAt());
    }

    @Test
    void toDomain_refreshToken_shouldMapAllFields() {
        UUID id = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        UUID userId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");

        RefreshTokenEntity e = RefreshTokenEntity.builder()
                .id(id)
                .tokenValue("rt")
                .userId(userId)
                .clientId("client-x")
                .issuedAt(Instant.parse("2024-06-01T00:00:00Z"))
                .expiresAt(Instant.parse("2024-06-02T00:00:00Z"))
                .revoked(true)
                .type(TokenType.REFRESH)
                .build();

        RefreshToken d = PersistenceMapper.toDomain(e);

        assertEquals(id, callAnyNoArg(d, "id", "getId"));
        assertEquals("rt", callAnyNoArg(d, "tokenValue", "getTokenValue"));
        Object userIdVo = callAnyNoArg(d, "userId", "getUserId");
        assertEquals(userId, callNoArg(userIdVo, "value"));
        assertEquals("client-x", callAnyNoArg(d, "clientId", "getClientId"));
        assertEquals(e.getIssuedAt(), callAnyNoArg(d, "issuedAt", "getIssuedAt"));
        assertEquals(e.getExpiresAt(), callAnyNoArg(d, "expiresAt", "getExpiresAt"));
        assertEquals(true, callAnyNoArg(d, "isRevoked", "revoked", "getRevoked"));
        assertEquals(TokenType.REFRESH, callAnyNoArg(d, "type", "getType"));
    }

    @Test
    void toEntity_refreshToken_shouldMapAllFields_usingMocks() {
        UUID id = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        UUID userId = UUID.fromString("99999999-9999-9999-9999-999999999999");

        RefreshToken d = mock(RefreshToken.class);

        AuthUserId userIdVo = mock(AuthUserId.class);
        when(userIdVo.value()).thenReturn(userId);

        when(d.id()).thenReturn(id);
        when(d.tokenValue()).thenReturn("rt2");
        when(d.userId()).thenReturn(userIdVo);
        when(d.clientId()).thenReturn("client-y");
        when(d.issuedAt()).thenReturn(Instant.parse("2024-07-01T00:00:00Z"));
        when(d.expiresAt()).thenReturn(Instant.parse("2024-07-02T00:00:00Z"));
        when(d.isRevoked()).thenReturn(false);
        when(d.type()).thenReturn(TokenType.REFRESH);

        RefreshTokenEntity e = PersistenceMapper.toEntity(d);

        assertEquals(id, e.getId());
        assertEquals("rt2", e.getTokenValue());
        assertEquals(userId, e.getUserId());
        assertEquals("client-y", e.getClientId());
        assertEquals(d.issuedAt(), e.getIssuedAt());
        assertEquals(d.expiresAt(), e.getExpiresAt());
        assertFalse(e.isRevoked());
        assertEquals(TokenType.REFRESH, e.getType());
    }

    @Test
    void toDomain_jwk_shouldMapAllFields() {
        JwkKeyEntity e = new JwkKeyEntity();
        e.setKeyId("kid-1");
        e.setPublicKeyPem("pem");
        e.setAlgorithm("RS256");
        e.setUse("sig");
        e.setCreatedAt(Instant.parse("2024-08-01T00:00:00Z"));
        e.setActive(true);

        JwkKey d = PersistenceMapper.toDomain(e);

        assertEquals("kid-1", callAnyNoArg(d, "keyId", "getKeyId"));
        assertEquals("pem", callAnyNoArg(d, "publicKeyPem", "getPublicKeyPem"));
        assertEquals("RS256", callAnyNoArg(d, "algorithm", "getAlgorithm"));
        assertEquals("sig", callAnyNoArg(d, "use", "getUse"));
        assertEquals(e.getCreatedAt(), callAnyNoArg(d, "createdAt", "getCreatedAt"));
        assertEquals(true, callAnyNoArg(d, "active", "isActive", "getActive"));
    }

    @Test
    void toDomain_clientApplication_shouldMapGrantTypes_whenNotNull_andFilterNulls_andReturnUnmodifiableSet() {
        java.util.Set<GrantType> grantTypes = new java.util.HashSet<>();
        grantTypes.add(GrantType.values()[0]);
        grantTypes.add(null);

        ClientApplicationEntity e = ClientApplicationEntity.builder()
                .id("11")
                .clientId("client-gt")
                .clientSecretHash("secret")
                .name("App")
                .clientType(ClientType.values()[0])
                .grantTypes(grantTypes)
                .redirectUris(null)
                .scopes(null)
                .firstParty(false)
                .active(true)
                .createdAt(Instant.parse("2024-04-01T00:00:00Z"))
                .updatedAt(Instant.parse("2024-04-02T00:00:00Z"))
                .build();

        ClientApplication d = PersistenceMapper.toDomain(e);

        Set<?> mappedGrantTypes = (Set<?>) callAnyNoArg(d, "grantTypes", "getGrantTypes");

        assertNotNull(mappedGrantTypes);
        assertEquals(1, mappedGrantTypes.size());
        assertTrue(mappedGrantTypes.contains(GrantType.values()[0]));
        assertThrows(UnsupportedOperationException.class, () -> ((Set<Object>) mappedGrantTypes).add(new Object()));
    }


    private static Object callNoArg(Object target, String name) {
        try {
            return target.getClass().getMethod(name).invoke(target);
        } catch (Exception e) {
            throw new AssertionError("Method not found or not invokable: " + target.getClass().getName() + "." + name, e);
        }
    }

    private static Object callAnyNoArg(Object target, String... names) {
        for (String name : names) {
            try {
                return target.getClass().getMethod(name).invoke(target);
            } catch (Exception ignored) {
            }
            String cap = Character.toUpperCase(name.charAt(0)) + name.substring(1);
            for (String alt : new String[]{"get" + cap, "is" + cap}) {
                try {
                    return target.getClass().getMethod(alt).invoke(target);
                } catch (Exception ignored) {
                }
            }
        }
        throw new AssertionError("No matching no-arg method found on " + target.getClass().getName() + " for: " + String.join(", ", names));
    }

    private static Set<?> extractPermissionsFromAuthUser(AuthUser user) {
        String[] candidates = {
                "permissions", "perms", "privileges", "authorities", "grants",
                "getPermissions", "getPerms", "getPrivileges", "getAuthorities", "getGrants"
        };

        for (String c : candidates) {
            try {
                Object v = user.getClass().getMethod(c).invoke(user);
                if (v instanceof Set<?> s) return s;
            } catch (Exception ignored) {
            }
        }

        Set<?> roles = (Set<?>) callAnyNoArg(user, "roles", "getRoles", "authorities", "getAuthorities");
        java.util.Set<Object> aggregated = new java.util.HashSet<>();

        for (Object role : roles) {
            Set<?> rolePerms = null;
            try {
                Object rp = callAnyNoArg(role, "permissions", "perms", "privileges", "getPermissions", "getPerms", "getPrivileges");
                if (rp instanceof Set<?> s) rolePerms = s;
            } catch (AssertionError ignored) {
            }
            if (rolePerms != null) aggregated.addAll(rolePerms);
        }

        return java.util.Collections.unmodifiableSet(aggregated);
    }


}
