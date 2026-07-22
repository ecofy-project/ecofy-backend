package br.com.ecofy.auth.adapters.out.persistence.mapper;

import br.com.ecofy.auth.adapters.out.persistence.entity.AuthUserEntity;
import br.com.ecofy.auth.adapters.out.persistence.entity.ClientApplicationEntity;
import br.com.ecofy.auth.adapters.out.persistence.entity.JwkKeyEntity;
import br.com.ecofy.auth.adapters.out.persistence.entity.PermissionEntity;
import br.com.ecofy.auth.adapters.out.persistence.entity.RefreshTokenEntity;
import br.com.ecofy.auth.adapters.out.persistence.entity.RoleEntity;
import br.com.ecofy.auth.core.domain.AuthUser;
import br.com.ecofy.auth.core.domain.ClientApplication;
import br.com.ecofy.auth.core.domain.JwkKey;
import br.com.ecofy.auth.core.domain.Permission;
import br.com.ecofy.auth.core.domain.RefreshToken;
import br.com.ecofy.auth.core.domain.Role;
import br.com.ecofy.auth.core.domain.enums.AuthUserStatus;
import br.com.ecofy.auth.core.domain.enums.ClientType;
import br.com.ecofy.auth.core.domain.enums.GrantType;
import br.com.ecofy.auth.core.domain.enums.TokenType;
import br.com.ecofy.auth.core.domain.valueobject.AuthUserId;
import br.com.ecofy.auth.core.domain.valueobject.EmailAddress;
import br.com.ecofy.auth.core.domain.valueobject.PasswordHash;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Testes unitários do mapeador de persistência")
class PersistenceMapperTest {

    private static final Instant CREATED_AT =
            Instant.parse("2026-07-20T10:00:00Z");
    private static final Instant UPDATED_AT =
            Instant.parse("2026-07-20T11:00:00Z");
    private static final Instant LAST_LOGIN_AT =
            Instant.parse("2026-07-20T12:00:00Z");
    private static final Instant EXPIRES_AT =
            Instant.parse("2026-07-21T10:00:00Z");

    @Test
    @DisplayName("Deve impedir a instanciação da classe utilitária")
    void constructor_invocacaoPorReflexao_deveLancarAssertionError()
            throws Exception {
        // Arrange
        Constructor<PersistenceMapper> constructor =
                PersistenceMapper.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // Act
        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                constructor::newInstance
        );

        // Assert
        AssertionError cause = assertInstanceOf(
                AssertionError.class,
                exception.getCause()
        );

        assertEquals(
                "PersistenceMapper is a utility class and should not be instantiated",
                cause.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar usuário nulo ao converter para entidade")
    void toEntity_authUserNulo_deveLancarNullPointerException() {
        // Arrange
        AuthUser user = null;

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> PersistenceMapper.toEntity(user)
        );

        // Assert
        assertEquals("user must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("Deve converter usuário de domínio para entidade com todos os campos")
    void toEntity_authUserValido_deveMapearTodosOsCampos() {
        // Arrange
        UUID id = UUID.fromString(
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
        );

        AuthUser user = new AuthUser(
                new AuthUserId(id),
                new EmailAddress("usuario@ecofy.com"),
                new PasswordHash("password-hash"),
                AuthUserStatus.ACTIVE,
                true,
                "Matheus",
                "Silva",
                "pt-BR",
                Set.of(),
                Set.of(),
                CREATED_AT,
                UPDATED_AT,
                LAST_LOGIN_AT,
                3
        );

        // Act
        AuthUserEntity result = PersistenceMapper.toEntity(user);

        // Assert
        assertAll(
                () -> assertEquals(id, result.getId()),
                () -> assertEquals(
                        "usuario@ecofy.com",
                        result.getEmail()
                ),
                () -> assertEquals(
                        "password-hash",
                        result.getPasswordHash()
                ),
                () -> assertEquals(
                        AuthUserStatus.ACTIVE,
                        result.getStatus()
                ),
                () -> assertTrue(result.isEmailVerified()),
                () -> assertEquals("Matheus", result.getFirstName()),
                () -> assertEquals("Silva", result.getLastName()),
                () -> assertEquals("pt-BR", result.getLocale()),
                () -> assertEquals(CREATED_AT, result.getCreatedAt()),
                () -> assertEquals(UPDATED_AT, result.getUpdatedAt()),
                () -> assertEquals(
                        LAST_LOGIN_AT,
                        result.getLastLoginAt()
                ),
                () -> assertEquals(
                        3,
                        result.getFailedLoginAttempts()
                )
        );
    }

    @Test
    @DisplayName("Deve rejeitar entidade de usuário nula ao converter para domínio")
    void toDomain_authUserEntityNula_deveLancarNullPointerException() {
        // Arrange
        AuthUserEntity entity = null;

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> PersistenceMapper.toDomain(
                        entity,
                        Set.of(),
                        Set.of()
                )
        );

        // Assert
        assertEquals(
                "AuthUserEntity must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve converter relacionamentos nulos do usuário em conjuntos vazios")
    void toDomain_relacionamentosDoUsuarioNulos_deveRetornarConjuntosVazios() {
        // Arrange
        UUID id = UUID.fromString(
                "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"
        );
        AuthUserEntity entity = createAuthUserEntity(id);

        // Act
        AuthUser result = PersistenceMapper.toDomain(
                entity,
                null,
                null
        );

        // Assert
        assertAll(
                () -> assertEquals(id, result.id().value()),
                () -> assertEquals(
                        "usuario@ecofy.com",
                        result.email().value()
                ),
                () -> assertEquals(
                        "password-hash",
                        result.passwordHash().value()
                ),
                () -> assertEquals(
                        AuthUserStatus.ACTIVE,
                        result.status()
                ),
                () -> assertTrue(result.isEmailVerified()),
                () -> assertEquals("Matheus", result.firstName()),
                () -> assertEquals("Silva", result.lastName()),
                () -> assertEquals("pt-BR", result.locale()),
                () -> assertEquals(CREATED_AT, result.createdAt()),
                () -> assertEquals(UPDATED_AT, result.updatedAt()),
                () -> assertEquals(
                        LAST_LOGIN_AT,
                        result.lastLoginAt()
                ),
                () -> assertEquals(
                        2,
                        result.failedLoginAttempts()
                ),
                () -> assertNotNull(result.roles()),
                () -> assertTrue(result.roles().isEmpty()),
                () -> assertNotNull(result.directPermissions()),
                () -> assertTrue(
                        result.directPermissions().isEmpty()
                )
        );
    }

    @Test
    @DisplayName("Deve filtrar relacionamentos nulos e converter funções e permissões do usuário")
    void toDomain_relacionamentosComElementosNulos_deveFiltrarEMapearValores() {
        // Arrange
        UUID id = UUID.fromString(
                "cccccccc-cccc-cccc-cccc-cccccccccccc"
        );
        AuthUserEntity entity = createAuthUserEntity(id);

        PermissionEntity rolePermission = createPermissionEntity(
                "user.read",
                "Permite consultar usuários",
                "auth"
        );
        PermissionEntity directPermission = createPermissionEntity(
                "user.write",
                "Permite alterar usuários",
                "auth"
        );

        Set<PermissionEntity> rolePermissions = new HashSet<>();
        rolePermissions.add(rolePermission);
        rolePermissions.add(null);

        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setName("ROLE_ADMIN");
        roleEntity.setDescription("Administrador");
        roleEntity.setPermissions(rolePermissions);

        Set<RoleEntity> roleEntities = new HashSet<>();
        roleEntities.add(roleEntity);
        roleEntities.add(null);

        Set<PermissionEntity> permissionEntities = new HashSet<>();
        permissionEntities.add(directPermission);
        permissionEntities.add(null);

        // Act
        AuthUser result = PersistenceMapper.toDomain(
                entity,
                roleEntities,
                permissionEntities
        );

        // Assert
        assertAll(
                () -> assertEquals(1, result.roles().size()),
                () -> assertEquals(
                        1,
                        result.directPermissions().size()
                ),
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> result.roles().add(
                                new Role(
                                        "ROLE_USER",
                                        "Usuário",
                                        Set.of()
                                )
                        )
                ),
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> result.directPermissions().add(
                                new Permission(
                                        "other",
                                        "Outra",
                                        "auth"
                                )
                        )
                )
        );

        Role mappedRole = result.roles().iterator().next();
        Permission mappedRolePermission =
                mappedRole.permissions().iterator().next();
        Permission mappedDirectPermission =
                result.directPermissions().iterator().next();

        assertAll(
                () -> assertEquals(
                        "ROLE_ADMIN",
                        mappedRole.name()
                ),
                () -> assertEquals(
                        "Administrador",
                        mappedRole.description()
                ),
                () -> assertEquals(
                        1,
                        mappedRole.permissions().size()
                ),
                () -> assertEquals(
                        "user.read",
                        mappedRolePermission.name()
                ),
                () -> assertEquals(
                        "Permite consultar usuários",
                        mappedRolePermission.description()
                ),
                () -> assertEquals(
                        "auth",
                        mappedRolePermission.domain()
                ),
                () -> assertEquals(
                        "user.write",
                        mappedDirectPermission.name()
                ),
                () -> assertEquals(
                        "Permite alterar usuários",
                        mappedDirectPermission.description()
                ),
                () -> assertEquals(
                        "auth",
                        mappedDirectPermission.domain()
                )
        );
    }

    @Test
    @DisplayName("Deve rejeitar entidade de função nula ao converter para domínio")
    void toDomain_roleEntityNula_deveLancarNullPointerException() {
        // Arrange
        RoleEntity entity = null;

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> PersistenceMapper.toDomain(entity)
        );

        // Assert
        assertEquals(
                "RoleEntity must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve converter função sem permissões para conjunto vazio")
    void toDomain_roleSemPermissoes_deveRetornarConjuntoVazio() {
        // Arrange
        RoleEntity entity = new RoleEntity();
        entity.setName("ROLE_USER");
        entity.setDescription("Usuário");
        entity.setPermissions(null);

        // Act
        Role result = PersistenceMapper.toDomain(entity);

        // Assert
        assertAll(
                () -> assertEquals("ROLE_USER", result.name()),
                () -> assertEquals("Usuário", result.description()),
                () -> assertNotNull(result.permissions()),
                () -> assertTrue(result.permissions().isEmpty()),
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> result.permissions().add(
                                new Permission(
                                        "user.read",
                                        "Leitura",
                                        "auth"
                                )
                        )
                )
        );
    }

    @Test
    @DisplayName("Deve filtrar permissões nulas ao converter uma função")
    void toDomain_roleComPermissaoNula_deveFiltrarElementosNulos() {
        // Arrange
        PermissionEntity permission = createPermissionEntity(
                "profile.read",
                "Permite consultar o perfil",
                "users"
        );

        Set<PermissionEntity> permissions = new HashSet<>();
        permissions.add(permission);
        permissions.add(null);

        RoleEntity entity = new RoleEntity();
        entity.setName("ROLE_MANAGER");
        entity.setDescription("Gerente");
        entity.setPermissions(permissions);

        // Act
        Role result = PersistenceMapper.toDomain(entity);

        // Assert
        Permission mappedPermission =
                result.permissions().iterator().next();

        assertAll(
                () -> assertEquals(1, result.permissions().size()),
                () -> assertEquals(
                        "profile.read",
                        mappedPermission.name()
                ),
                () -> assertEquals(
                        "Permite consultar o perfil",
                        mappedPermission.description()
                ),
                () -> assertEquals(
                        "users",
                        mappedPermission.domain()
                )
        );
    }

    @Test
    @DisplayName("Deve rejeitar entidade de permissão nula ao converter para domínio")
    void toDomain_permissionEntityNula_deveLancarNullPointerException() {
        // Arrange
        PermissionEntity entity = null;

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> PersistenceMapper.toDomain(entity)
        );

        // Assert
        assertEquals(
                "PermissionEntity must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve converter entidade de permissão com todos os campos")
    void toDomain_permissionEntityValida_deveMapearTodosOsCampos() {
        // Arrange
        PermissionEntity entity = createPermissionEntity(
                "budget.read",
                "Permite consultar orçamentos",
                "budgeting"
        );

        // Act
        Permission result = PersistenceMapper.toDomain(entity);

        // Assert
        assertAll(
                () -> assertEquals("budget.read", result.name()),
                () -> assertEquals(
                        "Permite consultar orçamentos",
                        result.description()
                ),
                () -> assertEquals("budgeting", result.domain())
        );
    }

    @Test
    @DisplayName("Deve rejeitar entidade de aplicação cliente nula ao converter para domínio")
    void toDomain_clientApplicationEntityNula_deveLancarNullPointerException() {
        // Arrange
        ClientApplicationEntity entity = null;

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> PersistenceMapper.toDomain(entity)
        );

        // Assert
        assertEquals(
                "ClientApplicationEntity must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve converter conjuntos nulos da aplicação cliente em conjuntos vazios")
    void toDomain_conjuntosDaAplicacaoNulos_deveRetornarConjuntosVazios() {
        // Arrange
        ClientApplicationEntity entity =
                ClientApplicationEntity.builder()
                        .id("application-1")
                        .clientId("client-1")
                        .clientSecretHash("secret-hash")
                        .name("Aplicação EcoFy")
                        .clientType(ClientType.values()[0])
                        .grantTypes(null)
                        .redirectUris(null)
                        .scopes(null)
                        .firstParty(true)
                        .active(true)
                        .createdAt(CREATED_AT)
                        .updatedAt(UPDATED_AT)
                        .build();

        // Act
        ClientApplication result =
                PersistenceMapper.toDomain(entity);

        // Assert
        assertAll(
                () -> assertEquals(
                        "application-1",
                        result.id()
                ),
                () -> assertEquals(
                        "client-1",
                        result.clientId()
                ),
                () -> assertEquals(
                        "secret-hash",
                        result.clientSecretHash()
                ),
                () -> assertEquals(
                        "Aplicação EcoFy",
                        result.name()
                ),
                () -> assertEquals(
                        ClientType.values()[0],
                        result.clientType()
                ),
                () -> assertNotNull(result.grantTypes()),
                () -> assertTrue(result.grantTypes().isEmpty()),
                () -> assertNotNull(result.redirectUris()),
                () -> assertTrue(result.redirectUris().isEmpty()),
                () -> assertNotNull(result.scopes()),
                () -> assertTrue(result.scopes().isEmpty()),
                () -> assertTrue(result.isFirstParty()),
                () -> assertTrue(result.isActive()),
                () -> assertEquals(
                        CREATED_AT,
                        result.createdAt()
                ),
                () -> assertEquals(
                        UPDATED_AT,
                        result.updatedAt()
                )
        );
    }

    @Test
    @DisplayName("Deve filtrar valores nulos e em branco dos conjuntos da aplicação cliente")
    void toDomain_conjuntosComValoresInvalidos_deveFiltrarENormalizarValores() {
        // Arrange
        GrantType grantType = GrantType.values()[0];

        Set<GrantType> grantTypes = new HashSet<>();
        grantTypes.add(grantType);
        grantTypes.add(null);

        Set<String> redirectUris = new HashSet<>();
        redirectUris.add("  https://ecofy.com/callback  ");
        redirectUris.add("https://app.ecofy.com/callback");
        redirectUris.add("");
        redirectUris.add("   ");
        redirectUris.add(null);

        Set<String> scopes = new HashSet<>();
        scopes.add("  openid  ");
        scopes.add("profile");
        scopes.add("");
        scopes.add("   ");
        scopes.add(null);

        ClientApplicationEntity entity =
                ClientApplicationEntity.builder()
                        .id("application-2")
                        .clientId("client-2")
                        .clientSecretHash("another-secret")
                        .name("Aplicação externa")
                        .clientType(ClientType.values()[0])
                        .grantTypes(grantTypes)
                        .redirectUris(redirectUris)
                        .scopes(scopes)
                        .firstParty(false)
                        .active(true)
                        .createdAt(CREATED_AT)
                        .updatedAt(UPDATED_AT)
                        .build();

        // Act
        ClientApplication result =
                PersistenceMapper.toDomain(entity);

        // Assert
        assertAll(
                () -> assertEquals(
                        Set.of(grantType),
                        result.grantTypes()
                ),
                () -> assertEquals(
                        Set.of(
                                "https://ecofy.com/callback",
                                "https://app.ecofy.com/callback"
                        ),
                        result.redirectUris()
                ),
                () -> assertEquals(
                        Set.of("openid", "profile"),
                        result.scopes()
                ),
                () -> assertFalse(result.isFirstParty()),
                () -> assertTrue(result.isActive()),
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> result.grantTypes().add(grantType)
                ),
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> result.redirectUris().add(
                                "https://other.ecofy.com/callback"
                        )
                ),
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> result.scopes().add("email")
                )
        );
    }

    @Test
    @DisplayName("Deve rejeitar aplicação cliente nula ao converter para entidade")
    void toEntity_clientApplicationNula_deveLancarNullPointerException() {
        // Arrange
        ClientApplication clientApplication = null;

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> PersistenceMapper.toEntity(clientApplication)
        );

        // Assert
        assertEquals(
                "clientApplication must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve converter aplicação cliente de domínio para entidade com todos os campos")
    void toEntity_clientApplicationValida_deveMapearTodosOsCampos() {
        // Arrange
        GrantType grantType = GrantType.values()[0];
        ClientType clientType = ClientType.values()[0];

        ClientApplication clientApplication =
                new ClientApplication(
                        "application-3",
                        "client-3",
                        "secret-hash",
                        "Aplicação interna",
                        clientType,
                        Set.of(grantType),
                        Set.of("https://ecofy.com/callback"),
                        Set.of("openid", "profile"),
                        true,
                        false,
                        CREATED_AT,
                        UPDATED_AT
                );

        // Act
        ClientApplicationEntity result =
                PersistenceMapper.toEntity(clientApplication);

        // Assert
        assertAll(
                () -> assertEquals(
                        "application-3",
                        result.getId()
                ),
                () -> assertEquals(
                        "client-3",
                        result.getClientId()
                ),
                () -> assertEquals(
                        "secret-hash",
                        result.getClientSecretHash()
                ),
                () -> assertEquals(
                        "Aplicação interna",
                        result.getName()
                ),
                () -> assertEquals(
                        clientType,
                        result.getClientType()
                ),
                () -> assertEquals(
                        Set.of(grantType),
                        result.getGrantTypes()
                ),
                () -> assertEquals(
                        Set.of("https://ecofy.com/callback"),
                        result.getRedirectUris()
                ),
                () -> assertEquals(
                        Set.of("openid", "profile"),
                        result.getScopes()
                ),
                () -> assertTrue(result.isFirstParty()),
                () -> assertFalse(result.isActive()),
                () -> assertEquals(
                        CREATED_AT,
                        result.getCreatedAt()
                ),
                () -> assertEquals(
                        UPDATED_AT,
                        result.getUpdatedAt()
                )
        );
    }

    @Test
    @DisplayName("Deve rejeitar entidade de refresh token nula ao converter para domínio")
    void toDomain_refreshTokenEntityNula_deveLancarNullPointerException() {
        // Arrange
        RefreshTokenEntity entity = null;

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> PersistenceMapper.toDomain(entity)
        );

        // Assert
        assertEquals(
                "RefreshTokenEntity must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve converter entidade de refresh token com todos os campos")
    void toDomain_refreshTokenEntityValida_deveMapearTodosOsCampos() {
        // Arrange
        UUID tokenId = UUID.fromString(
                "dddddddd-dddd-dddd-dddd-dddddddddddd"
        );
        UUID userId = UUID.fromString(
                "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"
        );

        RefreshTokenEntity entity = RefreshTokenEntity.builder()
                .id(tokenId)
                .tokenValue("refresh-token")
                .userId(userId)
                .clientId("client-4")
                .issuedAt(CREATED_AT)
                .expiresAt(EXPIRES_AT)
                .revoked(true)
                .type(TokenType.REFRESH)
                .build();

        // Act
        RefreshToken result = PersistenceMapper.toDomain(entity);

        // Assert
        assertAll(
                () -> assertEquals(tokenId, result.id()),
                () -> assertEquals(
                        "refresh-token",
                        result.tokenValue()
                ),
                () -> assertEquals(
                        userId,
                        result.userId().value()
                ),
                () -> assertEquals(
                        "client-4",
                        result.clientId()
                ),
                () -> assertEquals(
                        CREATED_AT,
                        result.issuedAt()
                ),
                () -> assertEquals(
                        EXPIRES_AT,
                        result.expiresAt()
                ),
                () -> assertTrue(result.isRevoked()),
                () -> assertEquals(
                        TokenType.REFRESH,
                        result.type()
                )
        );
    }

    @Test
    @DisplayName("Deve rejeitar refresh token nulo ao converter para entidade")
    void toEntity_refreshTokenNulo_deveLancarNullPointerException() {
        // Arrange
        RefreshToken refreshToken = null;

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> PersistenceMapper.toEntity(refreshToken)
        );

        // Assert
        assertEquals(
                "refreshToken must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve converter refresh token de domínio para entidade com todos os campos")
    void toEntity_refreshTokenValido_deveMapearTodosOsCampos() {
        // Arrange
        UUID tokenId = UUID.fromString(
                "ffffffff-ffff-ffff-ffff-ffffffffffff"
        );
        UUID userId = UUID.fromString(
                "99999999-9999-9999-9999-999999999999"
        );

        RefreshToken refreshToken = new RefreshToken(
                tokenId,
                "refresh-token-value",
                new AuthUserId(userId),
                "client-5",
                CREATED_AT,
                EXPIRES_AT,
                false,
                TokenType.REFRESH
        );

        // Act
        RefreshTokenEntity result =
                PersistenceMapper.toEntity(refreshToken);

        // Assert
        assertAll(
                () -> assertEquals(tokenId, result.getId()),
                () -> assertEquals(
                        "refresh-token-value",
                        result.getTokenValue()
                ),
                () -> assertEquals(userId, result.getUserId()),
                () -> assertEquals(
                        "client-5",
                        result.getClientId()
                ),
                () -> assertEquals(
                        CREATED_AT,
                        result.getIssuedAt()
                ),
                () -> assertEquals(
                        EXPIRES_AT,
                        result.getExpiresAt()
                ),
                () -> assertFalse(result.isRevoked()),
                () -> assertEquals(
                        TokenType.REFRESH,
                        result.getType()
                )
        );
    }

    @Test
    @DisplayName("Deve rejeitar entidade de chave JWK nula ao converter para domínio")
    void toDomain_jwkKeyEntityNula_deveLancarNullPointerException() {
        // Arrange
        JwkKeyEntity entity = null;

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> PersistenceMapper.toDomain(entity)
        );

        // Assert
        assertEquals(
                "JwkKeyEntity must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve converter entidade de chave JWK com todos os campos")
    void toDomain_jwkKeyEntityValida_deveMapearTodosOsCampos() {
        // Arrange
        JwkKeyEntity entity = new JwkKeyEntity();
        entity.setKeyId("key-id-1");
        entity.setPublicKeyPem("public-key-pem");
        entity.setAlgorithm("RS256");
        entity.setUse("sig");
        entity.setCreatedAt(CREATED_AT);
        entity.setActive(true);

        // Act
        JwkKey result = PersistenceMapper.toDomain(entity);

        // Assert
        assertAll(
                () -> assertEquals("key-id-1", result.keyId()),
                () -> assertEquals(
                        "public-key-pem",
                        result.publicKeyPem()
                ),
                () -> assertEquals("RS256", result.algorithm()),
                () -> assertEquals("sig", result.use()),
                () -> assertEquals(
                        CREATED_AT,
                        result.createdAt()
                ),
                () -> assertTrue(result.active())
        );
    }

    private AuthUserEntity createAuthUserEntity(UUID id) {
        AuthUserEntity entity = new AuthUserEntity();
        entity.setId(id);
        entity.setEmail("usuario@ecofy.com");
        entity.setPasswordHash("password-hash");
        entity.setStatus(AuthUserStatus.ACTIVE);
        entity.setEmailVerified(true);
        entity.setFirstName("Matheus");
        entity.setLastName("Silva");
        entity.setLocale("pt-BR");
        entity.setCreatedAt(CREATED_AT);
        entity.setUpdatedAt(UPDATED_AT);
        entity.setLastLoginAt(LAST_LOGIN_AT);
        entity.setFailedLoginAttempts(2);
        return entity;
    }

    private PermissionEntity createPermissionEntity(
            String name,
            String description,
            String domain
    ) {
        PermissionEntity entity = new PermissionEntity();
        entity.setName(name);
        entity.setDescription(description);
        entity.setDomain(domain);
        return entity;
    }
}
