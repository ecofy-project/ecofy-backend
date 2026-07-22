package br.com.ecofy.auth.adapters.out.persistence;

import br.com.ecofy.auth.adapters.out.persistence.entity.AuthUserEntity;
import br.com.ecofy.auth.adapters.out.persistence.entity.PermissionEntity;
import br.com.ecofy.auth.adapters.out.persistence.entity.RoleEntity;
import br.com.ecofy.auth.adapters.out.persistence.repository.AuthUserRepository;
import br.com.ecofy.auth.adapters.out.persistence.repository.RoleRepository;
import br.com.ecofy.auth.core.domain.AuthUser;
import br.com.ecofy.auth.core.domain.Permission;
import br.com.ecofy.auth.core.domain.Role;
import br.com.ecofy.auth.core.domain.enums.AuthUserStatus;
import br.com.ecofy.auth.core.domain.valueobject.AuthUserId;
import br.com.ecofy.auth.core.domain.valueobject.EmailAddress;
import br.com.ecofy.auth.core.domain.valueobject.PasswordHash;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários do adaptador JPA de usuários autenticáveis")
class AuthUserJpaAdapterTest {

    private static final UUID USER_ID = UUID.fromString(
            "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
    );
    private static final String EMAIL = "usuario@ecofy.com";
    private static final String PASSWORD_HASH = "password-hash";
    private static final Instant CREATED_AT =
            Instant.parse("2026-07-20T10:00:00Z");
    private static final Instant UPDATED_AT =
            Instant.parse("2026-07-20T11:00:00Z");
    private static final Instant LAST_LOGIN_AT =
            Instant.parse("2026-07-20T12:00:00Z");

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private RoleRepository roleRepository;

    @Test
    @DisplayName("Deve rejeitar repositório de usuários nulo ao construir o adaptador")
    void constructor_authUserRepositoryNulo_deveLancarNullPointerException() {
        // Arrange
        AuthUserRepository nullRepository = null;

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new AuthUserJpaAdapter(
                        nullRepository,
                        roleRepository
                )
        );

        // Assert
        assertEquals(
                "authUserRepository must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar repositório de funções nulo ao construir o adaptador")
    void constructor_roleRepositoryNulo_deveLancarNullPointerException() {
        // Arrange
        RoleRepository nullRepository = null;

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new AuthUserJpaAdapter(
                        authUserRepository,
                        nullRepository
                )
        );

        // Assert
        assertEquals(
                "roleRepository must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar usuário nulo sem acessar os repositórios")
    void save_usuarioNulo_deveLancarNullPointerException() {
        // Arrange
        AuthUserJpaAdapter adapter = createAdapter();

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.save(null)
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "user must not be null",
                        exception.getMessage()
                ),
                () -> verifyNoInteractions(
                        authUserRepository,
                        roleRepository
                )
        );
    }

    @Test
    @DisplayName("Deve criar e persistir um novo usuário com funções existentes")
    void save_usuarioNovo_deveCriarEPersistirEntidade() {
        // Arrange
        Role existingRole = new Role(
                "ROLE_USER",
                "Usuário",
                Set.of()
        );
        Role missingRole = new Role(
                "ROLE_MISSING",
                "Função inexistente",
                Set.of()
        );

        Set<Role> roles = new HashSet<>();
        roles.add(existingRole);
        roles.add(missingRole);
        roles.add(null);

        AuthUser user = createDomainUser(roles);
        RoleEntity existingRoleEntity = createRoleEntity(
                "ROLE_USER",
                "Usuário"
        );

        when(authUserRepository.findById(USER_ID))
                .thenReturn(Optional.empty());
        when(roleRepository.findById("ROLE_USER"))
                .thenReturn(Optional.of(existingRoleEntity));
        when(roleRepository.findById("ROLE_MISSING"))
                .thenReturn(Optional.empty());
        when(authUserRepository.save(any(AuthUserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuthUserJpaAdapter adapter = createAdapter();
        ArgumentCaptor<AuthUserEntity> captor =
                ArgumentCaptor.forClass(AuthUserEntity.class);
        Instant beforeSave = Instant.now();

        // Act
        AuthUser result = adapter.save(user);

        Instant afterSave = Instant.now();

        // Assert
        verify(authUserRepository).save(captor.capture());
        AuthUserEntity savedEntity = captor.getValue();

        assertAll(
                () -> assertEquals(USER_ID, savedEntity.getId()),
                () -> assertEquals(EMAIL, savedEntity.getEmail()),
                () -> assertEquals(
                        PASSWORD_HASH,
                        savedEntity.getPasswordHash()
                ),
                () -> assertEquals(
                        AuthUserStatus.ACTIVE,
                        savedEntity.getStatus()
                ),
                () -> assertTrue(savedEntity.isEmailVerified()),
                () -> assertEquals(
                        "Matheus",
                        savedEntity.getFirstName()
                ),
                () -> assertEquals(
                        "Silva",
                        savedEntity.getLastName()
                ),
                () -> assertEquals(
                        "pt-BR",
                        savedEntity.getLocale()
                ),
                () -> assertEquals(
                        LAST_LOGIN_AT,
                        savedEntity.getLastLoginAt()
                ),
                () -> assertEquals(
                        2,
                        savedEntity.getFailedLoginAttempts()
                ),
                () -> assertNotNull(savedEntity.getCreatedAt()),
                () -> assertNotNull(savedEntity.getUpdatedAt()),
                () -> assertFalse(
                        savedEntity.getCreatedAt().isBefore(beforeSave)
                ),
                () -> assertFalse(
                        savedEntity.getCreatedAt().isAfter(afterSave)
                ),
                () -> assertEquals(
                        savedEntity.getCreatedAt(),
                        savedEntity.getUpdatedAt()
                ),
                () -> assertEquals(1, savedEntity.getRoles().size()),
                () -> assertTrue(
                        savedEntity.getRoles().contains(existingRoleEntity)
                )
        );

        assertAll(
                () -> assertEquals(USER_ID, result.id().value()),
                () -> assertEquals(EMAIL, result.email().value()),
                () -> assertEquals(
                        PASSWORD_HASH,
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
                () -> assertEquals(1, result.roles().size()),
                () -> assertTrue(
                        result.directPermissions().isEmpty()
                )
        );

        verify(authUserRepository).findById(USER_ID);
        verify(roleRepository).findById("ROLE_USER");
        verify(roleRepository).findById("ROLE_MISSING");
        verify(roleRepository, never()).findById(null);
    }

    @Test
    @DisplayName("Deve atualizar usuário existente preservando a data de criação")
    void save_usuarioExistenteComCreatedAt_deveAtualizarEPreservarCriacao() {
        // Arrange
        AuthUser user = createDomainUser(Set.of());
        AuthUserEntity existingEntity = createAuthUserEntity(USER_ID);
        existingEntity.setCreatedAt(CREATED_AT);
        existingEntity.setUpdatedAt(UPDATED_AT);

        when(authUserRepository.findById(USER_ID))
                .thenReturn(Optional.of(existingEntity));
        when(authUserRepository.save(existingEntity))
                .thenReturn(existingEntity);

        AuthUserJpaAdapter adapter = createAdapter();
        Instant beforeSave = Instant.now();

        // Act
        AuthUser result = adapter.save(user);

        Instant afterSave = Instant.now();

        // Assert
        assertAll(
                () -> assertSame(
                        existingEntity,
                        captureSavedEntity()
                ),
                () -> assertEquals(
                        CREATED_AT,
                        existingEntity.getCreatedAt()
                ),
                () -> assertFalse(
                        existingEntity.getUpdatedAt().isBefore(beforeSave)
                ),
                () -> assertFalse(
                        existingEntity.getUpdatedAt().isAfter(afterSave)
                ),
                () -> assertEquals(EMAIL, existingEntity.getEmail()),
                () -> assertEquals(
                        PASSWORD_HASH,
                        existingEntity.getPasswordHash()
                ),
                () -> assertTrue(existingEntity.getRoles().isEmpty()),
                () -> assertEquals(USER_ID, result.id().value())
        );

        verify(authUserRepository).findById(USER_ID);
        verifyNoInteractions(roleRepository);
    }

    @Test
    @DisplayName("Deve preencher a data de criação quando a entidade existente não possuir esse valor")
    void save_usuarioExistenteSemCreatedAt_devePreencherDatasTemporais() {
        // Arrange
        AuthUser user = createDomainUser(Set.of());
        AuthUserEntity existingEntity = createAuthUserEntity(USER_ID);
        existingEntity.setCreatedAt(null);
        existingEntity.setUpdatedAt(null);

        when(authUserRepository.findById(USER_ID))
                .thenReturn(Optional.of(existingEntity));
        when(authUserRepository.save(existingEntity))
                .thenReturn(existingEntity);

        AuthUserJpaAdapter adapter = createAdapter();
        Instant beforeSave = Instant.now();

        // Act
        AuthUser result = adapter.save(user);

        Instant afterSave = Instant.now();

        // Assert
        assertAll(
                () -> assertNotNull(existingEntity.getCreatedAt()),
                () -> assertNotNull(existingEntity.getUpdatedAt()),
                () -> assertEquals(
                        existingEntity.getCreatedAt(),
                        existingEntity.getUpdatedAt()
                ),
                () -> assertFalse(
                        existingEntity.getCreatedAt().isBefore(beforeSave)
                ),
                () -> assertFalse(
                        existingEntity.getCreatedAt().isAfter(afterSave)
                ),
                () -> assertEquals(USER_ID, result.id().value())
        );

        verify(authUserRepository).save(existingEntity);
        verifyNoInteractions(roleRepository);
    }

    @Test
    @DisplayName("Deve rejeitar e-mail nulo sem consultar o repositório")
    void loadByEmail_emailNulo_deveLancarNullPointerException() {
        // Arrange
        AuthUserJpaAdapter adapter = createAdapter();

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.loadByEmail(null)
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "email must not be null",
                        exception.getMessage()
                ),
                () -> verifyNoInteractions(authUserRepository)
        );
    }

    @Test
    @DisplayName("Deve retornar vazio quando não existir usuário com o e-mail informado")
    void loadByEmail_usuarioInexistente_deveRetornarOptionalVazio() {
        // Arrange
        EmailAddress email = new EmailAddress(EMAIL);

        when(authUserRepository.findByEmailIgnoreCase(email.value()))
                .thenReturn(Optional.empty());

        AuthUserJpaAdapter adapter = createAdapter();

        // Act
        Optional<AuthUser> result = adapter.loadByEmail(email);

        // Assert
        assertAll(
                () -> assertTrue(result.isEmpty()),
                () -> verify(authUserRepository)
                        .findByEmailIgnoreCase(email.value()),
                () -> verifyNoInteractions(roleRepository)
        );
    }

    @Test
    @DisplayName("Deve converter e retornar o usuário encontrado pelo e-mail")
    void loadByEmail_usuarioExistente_deveRetornarUsuarioMapeado() {
        // Arrange
        EmailAddress email = new EmailAddress(EMAIL);
        AuthUserEntity entity = createAuthUserEntity(USER_ID);

        when(authUserRepository.findByEmailIgnoreCase(email.value()))
                .thenReturn(Optional.of(entity));

        AuthUserJpaAdapter adapter = createAdapter();

        // Act
        Optional<AuthUser> result = adapter.loadByEmail(email);

        // Assert
        assertAll(
                () -> assertTrue(result.isPresent()),
                () -> assertEquals(
                        USER_ID,
                        result.orElseThrow().id().value()
                ),
                () -> assertEquals(
                        EMAIL,
                        result.orElseThrow().email().value()
                ),
                () -> assertEquals(
                        AuthUserStatus.ACTIVE,
                        result.orElseThrow().status()
                ),
                () -> assertEquals(
                        1,
                        result.orElseThrow().roles().size()
                ),
                () -> assertEquals(
                        1,
                        result.orElseThrow()
                                .directPermissions()
                                .size()
                )
        );

        verify(authUserRepository)
                .findByEmailIgnoreCase(email.value());
    }

    @Test
    @DisplayName("Deve rejeitar identificador nulo sem consultar o repositório")
    void loadById_idNulo_deveLancarNullPointerException() {
        // Arrange
        AuthUserJpaAdapter adapter = createAdapter();

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.loadById(null)
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "id must not be null",
                        exception.getMessage()
                ),
                () -> verifyNoInteractions(authUserRepository)
        );
    }

    @Test
    @DisplayName("Deve retornar vazio quando não existir usuário com o identificador informado")
    void loadById_usuarioInexistente_deveRetornarOptionalVazio() {
        // Arrange
        AuthUserId id = new AuthUserId(USER_ID);

        when(authUserRepository.findById(id.value()))
                .thenReturn(Optional.empty());

        AuthUserJpaAdapter adapter = createAdapter();

        // Act
        Optional<AuthUser> result = adapter.loadById(id);

        // Assert
        assertAll(
                () -> assertTrue(result.isEmpty()),
                () -> verify(authUserRepository).findById(id.value()),
                () -> verifyNoInteractions(roleRepository)
        );
    }

    @Test
    @DisplayName("Deve converter e retornar o usuário encontrado pelo identificador")
    void loadById_usuarioExistente_deveRetornarUsuarioMapeado() {
        // Arrange
        AuthUserId id = new AuthUserId(USER_ID);
        AuthUserEntity entity = createAuthUserEntity(USER_ID);

        when(authUserRepository.findById(id.value()))
                .thenReturn(Optional.of(entity));

        AuthUserJpaAdapter adapter = createAdapter();

        // Act
        Optional<AuthUser> result = adapter.loadById(id);

        // Assert
        AuthUser user = result.orElseThrow();

        assertAll(
                () -> assertEquals(USER_ID, user.id().value()),
                () -> assertEquals(EMAIL, user.email().value()),
                () -> assertEquals(
                        PASSWORD_HASH,
                        user.passwordHash().value()
                ),
                () -> assertEquals(
                        AuthUserStatus.ACTIVE,
                        user.status()
                ),
                () -> assertTrue(user.isEmailVerified()),
                () -> assertEquals("Matheus", user.firstName()),
                () -> assertEquals("Silva", user.lastName()),
                () -> assertEquals("pt-BR", user.locale()),
                () -> assertEquals(CREATED_AT, user.createdAt()),
                () -> assertEquals(UPDATED_AT, user.updatedAt()),
                () -> assertEquals(
                        LAST_LOGIN_AT,
                        user.lastLoginAt()
                ),
                () -> assertEquals(
                        2,
                        user.failedLoginAttempts()
                ),
                () -> assertEquals(1, user.roles().size()),
                () -> assertEquals(
                        1,
                        user.directPermissions().size()
                )
        );

        verify(authUserRepository).findById(id.value());
    }

    private AuthUserJpaAdapter createAdapter() {
        return new AuthUserJpaAdapter(
                authUserRepository,
                roleRepository
        );
    }

    private AuthUser createDomainUser(Set<Role> roles) {
        return new AuthUser(
                new AuthUserId(USER_ID),
                new EmailAddress(EMAIL),
                new PasswordHash(PASSWORD_HASH),
                AuthUserStatus.ACTIVE,
                true,
                "Matheus",
                "Silva",
                "pt-BR",
                roles,
                Set.of(),
                CREATED_AT,
                UPDATED_AT,
                LAST_LOGIN_AT,
                2
        );
    }

    private AuthUserEntity createAuthUserEntity(UUID id) {
        PermissionEntity permission = new PermissionEntity();
        permission.setName("profile.read");
        permission.setDescription("Permite consultar o perfil");
        permission.setDomain("users");

        RoleEntity role = createRoleEntity(
                "ROLE_USER",
                "Usuário"
        );

        AuthUserEntity entity = new AuthUserEntity();
        entity.setId(id);
        entity.setEmail(EMAIL);
        entity.setPasswordHash(PASSWORD_HASH);
        entity.setStatus(AuthUserStatus.ACTIVE);
        entity.setEmailVerified(true);
        entity.setFirstName("Matheus");
        entity.setLastName("Silva");
        entity.setLocale("pt-BR");
        entity.setCreatedAt(CREATED_AT);
        entity.setUpdatedAt(UPDATED_AT);
        entity.setLastLoginAt(LAST_LOGIN_AT);
        entity.setFailedLoginAttempts(2);
        entity.setRoles(new HashSet<>(Set.of(role)));
        entity.setPermissions(new HashSet<>(Set.of(permission)));

        return entity;
    }

    private RoleEntity createRoleEntity(
            String name,
            String description
    ) {
        RoleEntity entity = new RoleEntity();
        entity.setName(name);
        entity.setDescription(description);
        entity.setPermissions(new HashSet<>());
        return entity;
    }

    private AuthUserEntity captureSavedEntity() {
        ArgumentCaptor<AuthUserEntity> captor =
                ArgumentCaptor.forClass(AuthUserEntity.class);
        verify(authUserRepository).save(captor.capture());
        return captor.getValue();
    }
}
