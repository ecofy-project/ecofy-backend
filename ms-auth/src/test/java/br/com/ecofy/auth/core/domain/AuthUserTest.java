package br.com.ecofy.auth.core.domain;

import br.com.ecofy.auth.core.domain.enums.AuthUserStatus;
import br.com.ecofy.auth.core.domain.valueobject.AuthUserId;
import br.com.ecofy.auth.core.domain.valueobject.EmailAddress;
import br.com.ecofy.auth.core.domain.valueobject.PasswordHash;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Testes unitários do usuário de autenticação")
class AuthUserTest {

    private static final Instant CREATED_AT =
            Instant.parse("2026-07-22T10:00:00Z");

    private static final Instant UPDATED_AT =
            Instant.parse("2026-07-22T11:00:00Z");

    private static final Instant LAST_LOGIN_AT =
            Instant.parse("2026-07-22T11:30:00Z");

    @Test
    @DisplayName("Deve reconstruir o usuário preservando todos os dados válidos")
    void constructor_dadosValidos_deveReconstruirUsuario() {
        // Arrange
        AuthUserId id = createId();
        EmailAddress email = createEmail();
        PasswordHash passwordHash = createPasswordHash();
        Role role = mock(Role.class);
        Permission permission = mock(Permission.class);
        Set<Role> roles = new HashSet<>(Set.of(role));
        Set<Permission> permissions = new HashSet<>(Set.of(permission));

        // Act
        AuthUser user = new AuthUser(
                id,
                email,
                passwordHash,
                AuthUserStatus.ACTIVE,
                true,
                " Matheus ",
                " Silva ",
                "en-US",
                roles,
                permissions,
                CREATED_AT,
                UPDATED_AT,
                LAST_LOGIN_AT,
                2
        );

        // Assert
        assertAll(
                () -> assertSame(id, user.id()),
                () -> assertSame(email, user.email()),
                () -> assertSame(
                        passwordHash,
                        user.passwordHash()
                ),
                () -> assertEquals(
                        AuthUserStatus.ACTIVE,
                        user.status()
                ),
                () -> assertTrue(user.isEmailVerified()),
                () -> assertEquals(
                        " Matheus ",
                        user.firstName()
                ),
                () -> assertEquals(
                        " Silva ",
                        user.lastName()
                ),
                () -> assertEquals("en-US", user.locale()),
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
                () -> assertEquals(Set.of(role), user.roles()),
                () -> assertEquals(
                        Set.of(permission),
                        user.directPermissions()
                )
        );
    }

    @Test
    @DisplayName("Deve aplicar valores padrão quando locale e coleções forem nulos")
    void constructor_localeEColecoesNulos_deveAplicarValoresPadrao() {
        // Arrange e Act
        AuthUser user = new AuthUser(
                createId(),
                createEmail(),
                createPasswordHash(),
                AuthUserStatus.PENDING_EMAIL_CONFIRMATION,
                false,
                null,
                null,
                null,
                null,
                null,
                CREATED_AT,
                UPDATED_AT,
                null,
                -1
        );

        // Assert
        assertAll(
                () -> assertEquals("pt-BR", user.locale()),
                () -> assertTrue(user.roles().isEmpty()),
                () -> assertTrue(
                        user.directPermissions().isEmpty()
                ),
                () -> assertEquals(
                        0,
                        user.failedLoginAttempts()
                ),
                () -> assertFalse(user.isEmailVerified()),
                () -> assertEquals(null, user.lastLoginAt())
        );
    }

    @Test
    @DisplayName("Deve aplicar o locale padrão quando o valor contiver somente espaços")
    void constructor_localeComEspacos_deveAplicarLocalePadrao() {
        // Arrange e Act
        AuthUser user = createUser(
                AuthUserStatus.ACTIVE,
                "   ",
                0
        );

        // Assert
        assertEquals("pt-BR", user.locale());
    }

    @Test
    @DisplayName("Deve copiar as coleções recebidas e proteger o estado interno")
    void constructor_colecoesMutaveis_deveRealizarCopiasDefensivas() {
        // Arrange
        Role initialRole = mock(Role.class);
        Role laterRole = mock(Role.class);
        Permission initialPermission = mock(Permission.class);
        Permission laterPermission = mock(Permission.class);

        Set<Role> roles = new HashSet<>(Set.of(initialRole));
        Set<Permission> permissions =
                new HashSet<>(Set.of(initialPermission));

        AuthUser user = new AuthUser(
                createId(),
                createEmail(),
                createPasswordHash(),
                AuthUserStatus.ACTIVE,
                true,
                "Matheus",
                "Silva",
                "pt-BR",
                roles,
                permissions,
                CREATED_AT,
                UPDATED_AT,
                null,
                0
        );

        // Act
        roles.add(laterRole);
        permissions.add(laterPermission);

        // Assert
        assertAll(
                () -> assertEquals(
                        Set.of(initialRole),
                        user.roles()
                ),
                () -> assertEquals(
                        Set.of(initialPermission),
                        user.directPermissions()
                ),
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> user.roles().add(laterRole)
                ),
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> user.directPermissions()
                                .add(laterPermission)
                )
        );
    }

    @Test
    @DisplayName("Deve rejeitar o identificador nulo")
    void constructor_idNulo_deveLancarNullPointerException() {
        // Arrange, Act e Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new AuthUser(
                        null,
                        createEmail(),
                        createPasswordHash(),
                        AuthUserStatus.ACTIVE,
                        true,
                        "Matheus",
                        "Silva",
                        "pt-BR",
                        Set.of(),
                        Set.of(),
                        CREATED_AT,
                        UPDATED_AT,
                        null,
                        0
                )
        );

        assertEquals(
                "id must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar o endereço de e-mail nulo")
    void constructor_emailNulo_deveLancarNullPointerException() {
        // Arrange, Act e Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new AuthUser(
                        createId(),
                        null,
                        createPasswordHash(),
                        AuthUserStatus.ACTIVE,
                        true,
                        "Matheus",
                        "Silva",
                        "pt-BR",
                        Set.of(),
                        Set.of(),
                        CREATED_AT,
                        UPDATED_AT,
                        null,
                        0
                )
        );

        assertEquals(
                "email must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar o hash de senha nulo")
    void constructor_passwordHashNulo_deveLancarNullPointerException() {
        // Arrange, Act e Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new AuthUser(
                        createId(),
                        createEmail(),
                        null,
                        AuthUserStatus.ACTIVE,
                        true,
                        "Matheus",
                        "Silva",
                        "pt-BR",
                        Set.of(),
                        Set.of(),
                        CREATED_AT,
                        UPDATED_AT,
                        null,
                        0
                )
        );

        assertEquals(
                "passwordHash must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar o status nulo")
    void constructor_statusNulo_deveLancarNullPointerException() {
        // Arrange, Act e Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new AuthUser(
                        createId(),
                        createEmail(),
                        createPasswordHash(),
                        null,
                        true,
                        "Matheus",
                        "Silva",
                        "pt-BR",
                        Set.of(),
                        Set.of(),
                        CREATED_AT,
                        UPDATED_AT,
                        null,
                        0
                )
        );

        assertEquals(
                "status must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar a data de criação nula")
    void constructor_createdAtNulo_deveLancarNullPointerException() {
        // Arrange, Act e Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new AuthUser(
                        createId(),
                        createEmail(),
                        createPasswordHash(),
                        AuthUserStatus.ACTIVE,
                        true,
                        "Matheus",
                        "Silva",
                        "pt-BR",
                        Set.of(),
                        Set.of(),
                        null,
                        UPDATED_AT,
                        null,
                        0
                )
        );

        assertEquals(
                "createdAt must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar a data de atualização nula")
    void constructor_updatedAtNulo_deveLancarNullPointerException() {
        // Arrange, Act e Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new AuthUser(
                        createId(),
                        createEmail(),
                        createPasswordHash(),
                        AuthUserStatus.ACTIVE,
                        true,
                        "Matheus",
                        "Silva",
                        "pt-BR",
                        Set.of(),
                        Set.of(),
                        CREATED_AT,
                        null,
                        null,
                        0
                )
        );

        assertEquals(
                "updatedAt must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve criar um novo usuário pendente com os dados iniciais esperados")
    void newPendingUser_dadosValidos_deveCriarUsuarioPendente() {
        // Arrange
        EmailAddress email = createEmail();
        PasswordHash passwordHash = createPasswordHash();
        Role role = mock(Role.class);
        Instant beforeCreation = Instant.now();

        // Act
        AuthUser user = AuthUser.newPendingUser(
                email,
                passwordHash,
                "Matheus",
                "Silva",
                "en-US",
                Set.of(role)
        );

        Instant afterCreation = Instant.now();

        // Assert
        assertAll(
                () -> assertNotNull(user.id()),
                () -> assertNotNull(user.id().value()),
                () -> assertSame(email, user.email()),
                () -> assertSame(
                        passwordHash,
                        user.passwordHash()
                ),
                () -> assertEquals(
                        AuthUserStatus.PENDING_EMAIL_CONFIRMATION,
                        user.status()
                ),
                () -> assertFalse(user.isEmailVerified()),
                () -> assertEquals(
                        "Matheus",
                        user.firstName()
                ),
                () -> assertEquals(
                        "Silva",
                        user.lastName()
                ),
                () -> assertEquals("en-US", user.locale()),
                () -> assertEquals(Set.of(role), user.roles()),
                () -> assertTrue(
                        user.directPermissions().isEmpty()
                ),
                () -> assertEquals(
                        user.createdAt(),
                        user.updatedAt()
                ),
                () -> assertFalse(
                        user.createdAt().isBefore(beforeCreation)
                ),
                () -> assertFalse(
                        user.createdAt().isAfter(afterCreation)
                ),
                () -> assertEquals(null, user.lastLoginAt()),
                () -> assertEquals(
                        0,
                        user.failedLoginAttempts()
                )
        );
    }

    @Test
    @DisplayName("Deve aplicar valores padrão ao criar usuário sem locale e sem roles")
    void newPendingUser_localeERolesNulos_deveAplicarValoresPadrao() {
        // Arrange e Act
        AuthUser user = AuthUser.newPendingUser(
                createEmail(),
                createPasswordHash(),
                null,
                null,
                null,
                null
        );

        // Assert
        assertAll(
                () -> assertEquals("pt-BR", user.locale()),
                () -> assertTrue(user.roles().isEmpty()),
                () -> assertTrue(
                        user.directPermissions().isEmpty()
                )
        );
    }

    @Test
    @DisplayName("Deve aplicar o locale padrão ao criar usuário com locale em branco")
    void newPendingUser_localeEmBranco_deveAplicarLocalePadrao() {
        // Arrange e Act
        AuthUser user = AuthUser.newPendingUser(
                createEmail(),
                createPasswordHash(),
                "Matheus",
                "Silva",
                "   ",
                Set.of()
        );

        // Assert
        assertEquals("pt-BR", user.locale());
    }

    @Test
    @DisplayName("Deve montar o nome completo removendo espaços externos")
    void fullName_nomesPreenchidos_deveRetornarNomeCompletoNormalizado() {
        // Arrange
        AuthUser user = createUser(
                AuthUserStatus.ACTIVE,
                "pt-BR",
                0,
                "  Matheus  ",
                "  Silva  "
        );

        // Act
        String result = user.fullName();

        // Assert
        assertEquals("Matheus Silva", result);
    }

    @Test
    @DisplayName("Deve retornar apenas o primeiro nome quando o sobrenome for nulo")
    void fullName_sobrenomeNulo_deveRetornarPrimeiroNome() {
        // Arrange
        AuthUser user = createUser(
                AuthUserStatus.ACTIVE,
                "pt-BR",
                0,
                "Matheus",
                null
        );

        // Act
        String result = user.fullName();

        // Assert
        assertEquals("Matheus", result);
    }

    @Test
    @DisplayName("Deve retornar apenas o sobrenome quando o primeiro nome for nulo")
    void fullName_primeiroNomeNulo_deveRetornarSobrenome() {
        // Arrange
        AuthUser user = createUser(
                AuthUserStatus.ACTIVE,
                "pt-BR",
                0,
                null,
                "Silva"
        );

        // Act
        String result = user.fullName();

        // Assert
        assertEquals("Silva", result);
    }

    @Test
    @DisplayName("Deve retornar nome vazio quando os nomes forem nulos")
    void fullName_nomesNulos_deveRetornarStringVazia() {
        // Arrange
        AuthUser user = createUser(
                AuthUserStatus.ACTIVE,
                "pt-BR",
                0,
                null,
                null
        );

        // Act
        String result = user.fullName();

        // Assert
        assertEquals("", result);
    }

    @Test
    @DisplayName("Deve confirmar o e-mail e ativar o usuário pendente")
    void confirmEmail_usuarioPendente_deveConfirmarEAtivarUsuario() {
        // Arrange
        AuthUser user = createUser(
                AuthUserStatus.PENDING_EMAIL_CONFIRMATION,
                "pt-BR",
                0
        );

        // Act
        user.confirmEmail();

        // Assert
        assertAll(
                () -> assertTrue(user.isEmailVerified()),
                () -> assertEquals(
                        AuthUserStatus.ACTIVE,
                        user.status()
                ),
                () -> assertNotEquals(
                        UPDATED_AT,
                        user.updatedAt()
                )
        );
    }

    @Test
    @DisplayName("Deve confirmar o e-mail preservando o status de um usuário ativo")
    void confirmEmail_usuarioAtivo_devePreservarStatus() {
        // Arrange
        AuthUser user = createUser(
                AuthUserStatus.ACTIVE,
                "pt-BR",
                0
        );

        // Act
        user.confirmEmail();

        // Assert
        assertAll(
                () -> assertTrue(user.isEmailVerified()),
                () -> assertEquals(
                        AuthUserStatus.ACTIVE,
                        user.status()
                ),
                () -> assertNotEquals(
                        UPDATED_AT,
                        user.updatedAt()
                )
        );
    }

    @Test
    @DisplayName("Deve rejeitar a confirmação de e-mail de usuário bloqueado")
    void confirmEmail_usuarioBloqueado_deveLancarIllegalStateException() {
        // Arrange
        AuthUser user = createUser(
                AuthUserStatus.BLOCKED,
                "pt-BR",
                0
        );

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                user::confirmEmail
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "User is not eligible to confirm email",
                        exception.getMessage()
                ),
                () -> assertFalse(user.isEmailVerified()),
                () -> assertEquals(
                        AuthUserStatus.BLOCKED,
                        user.status()
                ),
                () -> assertEquals(
                        UPDATED_AT,
                        user.updatedAt()
                )
        );
    }

    @Test
    @DisplayName("Deve rejeitar a confirmação de e-mail de usuário deletado")
    void confirmEmail_usuarioDeletado_deveLancarIllegalStateException() {
        // Arrange
        AuthUser user = createUser(
                AuthUserStatus.DELETED,
                "pt-BR",
                0
        );

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                user::confirmEmail
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "User is not eligible to confirm email",
                        exception.getMessage()
                ),
                () -> assertFalse(user.isEmailVerified()),
                () -> assertEquals(
                        AuthUserStatus.DELETED,
                        user.status()
                ),
                () -> assertEquals(
                        UPDATED_AT,
                        user.updatedAt()
                )
        );
    }

    @Test
    @DisplayName("Deve trocar a senha, zerar as tentativas falhas e atualizar o usuário")
    void changePassword_novoHashValido_deveAtualizarSenhaEZerarTentativas() {
        // Arrange
        AuthUser user = createUser(
                AuthUserStatus.ACTIVE,
                "pt-BR",
                3
        );
        PasswordHash newPasswordHash =
                new PasswordHash("$2a$12$novoHashSeguro");

        // Act
        user.changePassword(newPasswordHash);

        // Assert
        assertAll(
                () -> assertSame(
                        newPasswordHash,
                        user.passwordHash()
                ),
                () -> assertEquals(
                        0,
                        user.failedLoginAttempts()
                ),
                () -> assertNotEquals(
                        UPDATED_AT,
                        user.updatedAt()
                )
        );
    }

    @Test
    @DisplayName("Deve rejeitar a troca para um hash de senha nulo")
    void changePassword_novoHashNulo_deveLancarNullPointerException() {
        // Arrange
        AuthUser user = createUser(
                AuthUserStatus.ACTIVE,
                "pt-BR",
                3
        );
        PasswordHash originalPasswordHash =
                user.passwordHash();

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> user.changePassword(null)
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "newPasswordHash must not be null",
                        exception.getMessage()
                ),
                () -> assertSame(
                        originalPasswordHash,
                        user.passwordHash()
                ),
                () -> assertEquals(
                        3,
                        user.failedLoginAttempts()
                ),
                () -> assertEquals(
                        UPDATED_AT,
                        user.updatedAt()
                )
        );
    }

    @Test
    @DisplayName("Deve registrar login bem-sucedido, zerar tentativas e atualizar as datas")
    void registerSuccessfulLogin_usuarioComFalhas_deveRegistrarSucesso() {
        // Arrange
        AuthUser user = createUser(
                AuthUserStatus.ACTIVE,
                "pt-BR",
                3
        );
        Instant beforeLogin = Instant.now();

        // Act
        user.registerSuccessfulLogin();

        Instant afterLogin = Instant.now();

        // Assert
        assertAll(
                () -> assertEquals(
                        0,
                        user.failedLoginAttempts()
                ),
                () -> assertNotNull(user.lastLoginAt()),
                () -> assertFalse(
                        user.lastLoginAt().isBefore(beforeLogin)
                ),
                () -> assertFalse(
                        user.lastLoginAt().isAfter(afterLogin)
                ),
                () -> assertFalse(
                        user.updatedAt().isBefore(
                                user.lastLoginAt()
                        )
                ),
                () -> assertNotEquals(
                        UPDATED_AT,
                        user.updatedAt()
                )
        );
    }

    @Test
    @DisplayName("Deve incrementar as tentativas sem bloquear antes do limite")
    void registerFailedLogin_tentativasAbaixoDoLimite_deveManterStatus() {
        // Arrange
        AuthUser user = createUser(
                AuthUserStatus.ACTIVE,
                "pt-BR",
                0
        );

        // Act
        user.registerFailedLogin(3);

        // Assert
        assertAll(
                () -> assertEquals(
                        1,
                        user.failedLoginAttempts()
                ),
                () -> assertEquals(
                        AuthUserStatus.ACTIVE,
                        user.status()
                ),
                () -> assertNotEquals(
                        UPDATED_AT,
                        user.updatedAt()
                )
        );
    }

    @Test
    @DisplayName("Deve bloquear o usuário quando atingir o limite de tentativas")
    void registerFailedLogin_tentativaAtingeLimite_deveBloquearUsuario() {
        // Arrange
        AuthUser user = createUser(
                AuthUserStatus.ACTIVE,
                "pt-BR",
                2
        );

        // Act
        user.registerFailedLogin(3);

        // Assert
        assertAll(
                () -> assertEquals(
                        3,
                        user.failedLoginAttempts()
                ),
                () -> assertEquals(
                        AuthUserStatus.LOCKED,
                        user.status()
                ),
                () -> assertNotEquals(
                        UPDATED_AT,
                        user.updatedAt()
                )
        );
    }

    @Test
    @DisplayName("Deve rejeitar um limite de tentativas igual a zero")
    void registerFailedLogin_limiteZero_deveLancarIllegalArgumentException() {
        // Arrange
        AuthUser user = createUser(
                AuthUserStatus.ACTIVE,
                "pt-BR",
                0
        );

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> user.registerFailedLogin(0)
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "maxAttemptsBeforeLock must be greater than zero",
                        exception.getMessage()
                ),
                () -> assertEquals(
                        0,
                        user.failedLoginAttempts()
                ),
                () -> assertEquals(
                        AuthUserStatus.ACTIVE,
                        user.status()
                ),
                () -> assertEquals(
                        UPDATED_AT,
                        user.updatedAt()
                )
        );
    }

    @Test
    @DisplayName("Deve rejeitar um limite de tentativas negativo")
    void registerFailedLogin_limiteNegativo_deveLancarIllegalArgumentException() {
        // Arrange
        AuthUser user = createUser(
                AuthUserStatus.ACTIVE,
                "pt-BR",
                0
        );

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> user.registerFailedLogin(-1)
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "maxAttemptsBeforeLock must be greater than zero",
                        exception.getMessage()
                ),
                () -> assertEquals(
                        0,
                        user.failedLoginAttempts()
                )
        );
    }

    @Test
    @DisplayName("Deve reconhecer uma permissão concedida por role")
    void hasPermission_roleConcedePermissao_deveRetornarTrue() {
        // Arrange
        Role role = mock(Role.class);
        Permission directPermission = mock(Permission.class);
        when(role.implies(any(Permission.class)))
                .thenReturn(true);

        AuthUser user = createUser(
                Set.of(role),
                Set.of(directPermission)
        );

        // Act
        boolean result = user.hasPermission(
                "transactions:read"
        );

        // Assert
        assertTrue(result);
        verify(role).implies(any(Permission.class));
        verify(
                directPermission,
                never()
        ).implies(any(Permission.class));
    }

    @Test
    @DisplayName("Deve reconhecer uma permissão concedida diretamente ao usuário")
    void hasPermission_permissaoDiretaConcedeAcesso_deveRetornarTrue() {
        // Arrange
        Role role = mock(Role.class);
        Permission directPermission = mock(Permission.class);

        when(role.implies(any(Permission.class)))
                .thenReturn(false);
        when(directPermission.implies(any(Permission.class)))
                .thenReturn(true);

        AuthUser user = createUser(
                Set.of(role),
                Set.of(directPermission)
        );

        // Act
        boolean result = user.hasPermission(
                "transactions:read"
        );

        // Assert
        assertTrue(result);
        verify(role).implies(any(Permission.class));
        verify(directPermission)
                .implies(any(Permission.class));
    }

    @Test
    @DisplayName("Deve negar uma permissão não concedida por roles nem diretamente")
    void hasPermission_permissaoNaoConcedida_deveRetornarFalse() {
        // Arrange
        Role role = mock(Role.class);
        Permission directPermission = mock(Permission.class);

        when(role.implies(any(Permission.class)))
                .thenReturn(false);
        when(directPermission.implies(any(Permission.class)))
                .thenReturn(false);

        AuthUser user = createUser(
                Set.of(role),
                Set.of(directPermission)
        );

        // Act
        boolean result = user.hasPermission(
                "transactions:delete"
        );

        // Assert
        assertFalse(result);
        verify(role).implies(any(Permission.class));
        verify(directPermission)
                .implies(any(Permission.class));
    }

    @Test
    @DisplayName("Deve negar uma permissão quando o usuário não possuir autorizações")
    void hasPermission_usuarioSemAutorizacoes_deveRetornarFalse() {
        // Arrange
        AuthUser user = createUser(Set.of(), Set.of());

        // Act
        boolean result = user.hasPermission(
                "transactions:read"
        );

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Deve adicionar uma role e atualizar o usuário")
    void addRole_roleValida_deveAdicionarRole() {
        // Arrange
        AuthUser user = createUser(Set.of(), Set.of());
        Role role = mock(Role.class);

        // Act
        user.addRole(role);

        // Assert
        assertAll(
                () -> assertEquals(
                        Set.of(role),
                        user.roles()
                ),
                () -> assertNotEquals(
                        UPDATED_AT,
                        user.updatedAt()
                )
        );
    }

    @Test
    @DisplayName("Deve manter uma única ocorrência ao adicionar uma role já existente")
    void addRole_roleDuplicada_deveManterConjuntoSemDuplicidade() {
        // Arrange
        Role role = mock(Role.class);
        AuthUser user = createUser(
                Set.of(role),
                Set.of()
        );

        // Act
        user.addRole(role);

        // Assert
        assertAll(
                () -> assertEquals(1, user.roles().size()),
                () -> assertTrue(user.roles().contains(role)),
                () -> assertNotEquals(
                        UPDATED_AT,
                        user.updatedAt()
                )
        );
    }

    @Test
    @DisplayName("Deve rejeitar a adição de uma role nula")
    void addRole_roleNula_deveLancarNullPointerException() {
        // Arrange
        AuthUser user = createUser(Set.of(), Set.of());

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> user.addRole(null)
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "role must not be null",
                        exception.getMessage()
                ),
                () -> assertTrue(user.roles().isEmpty()),
                () -> assertEquals(
                        UPDATED_AT,
                        user.updatedAt()
                )
        );
    }

    @Test
    @DisplayName("Deve adicionar uma permissão direta e atualizar o usuário")
    void addDirectPermission_permissaoValida_deveAdicionarPermissao() {
        // Arrange
        AuthUser user = createUser(Set.of(), Set.of());
        Permission permission = mock(Permission.class);

        // Act
        user.addDirectPermission(permission);

        // Assert
        assertAll(
                () -> assertEquals(
                        Set.of(permission),
                        user.directPermissions()
                ),
                () -> assertNotEquals(
                        UPDATED_AT,
                        user.updatedAt()
                )
        );
    }

    @Test
    @DisplayName("Deve manter uma única ocorrência ao adicionar permissão já existente")
    void addDirectPermission_permissaoDuplicada_deveManterConjuntoSemDuplicidade() {
        // Arrange
        Permission permission = mock(Permission.class);
        AuthUser user = createUser(
                Set.of(),
                Set.of(permission)
        );

        // Act
        user.addDirectPermission(permission);

        // Assert
        assertAll(
                () -> assertEquals(
                        1,
                        user.directPermissions().size()
                ),
                () -> assertTrue(
                        user.directPermissions()
                                .contains(permission)
                ),
                () -> assertNotEquals(
                        UPDATED_AT,
                        user.updatedAt()
                )
        );
    }

    @Test
    @DisplayName("Deve rejeitar a adição de uma permissão direta nula")
    void addDirectPermission_permissaoNula_deveLancarNullPointerException() {
        // Arrange
        AuthUser user = createUser(Set.of(), Set.of());

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> user.addDirectPermission(null)
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "permission must not be null",
                        exception.getMessage()
                ),
                () -> assertTrue(
                        user.directPermissions().isEmpty()
                ),
                () -> assertEquals(
                        UPDATED_AT,
                        user.updatedAt()
                )
        );
    }

    @Test
    @DisplayName("Deve marcar o usuário como bloqueado e atualizar o estado")
    void block_usuarioAtivo_deveAlterarStatusParaBloqueado() {
        // Arrange
        AuthUser user = createUser(
                AuthUserStatus.ACTIVE,
                "pt-BR",
                0
        );

        // Act
        user.block();

        // Assert
        assertAll(
                () -> assertEquals(
                        AuthUserStatus.BLOCKED,
                        user.status()
                ),
                () -> assertNotEquals(
                        UPDATED_AT,
                        user.updatedAt()
                )
        );
    }

    @Test
    @DisplayName("Deve marcar o usuário como deletado e atualizar o estado")
    void delete_usuarioAtivo_deveAlterarStatusParaDeletado() {
        // Arrange
        AuthUser user = createUser(
                AuthUserStatus.ACTIVE,
                "pt-BR",
                0
        );

        // Act
        user.delete();

        // Assert
        assertAll(
                () -> assertEquals(
                        AuthUserStatus.DELETED,
                        user.status()
                ),
                () -> assertNotEquals(
                        UPDATED_AT,
                        user.updatedAt()
                )
        );
    }

    private AuthUser createUser(
            AuthUserStatus status,
            String locale,
            int failedLoginAttempts
    ) {
        return createUser(
                status,
                locale,
                failedLoginAttempts,
                "Matheus",
                "Silva"
        );
    }

    private AuthUser createUser(
            AuthUserStatus status,
            String locale,
            int failedLoginAttempts,
            String firstName,
            String lastName
    ) {
        return new AuthUser(
                createId(),
                createEmail(),
                createPasswordHash(),
                status,
                false,
                firstName,
                lastName,
                locale,
                Set.of(),
                Set.of(),
                CREATED_AT,
                UPDATED_AT,
                null,
                failedLoginAttempts
        );
    }

    private AuthUser createUser(
            Set<Role> roles,
            Set<Permission> permissions
    ) {
        return new AuthUser(
                createId(),
                createEmail(),
                createPasswordHash(),
                AuthUserStatus.ACTIVE,
                true,
                "Matheus",
                "Silva",
                "pt-BR",
                roles,
                permissions,
                CREATED_AT,
                UPDATED_AT,
                null,
                0
        );
    }

    private AuthUserId createId() {
        return new AuthUserId(
                UUID.fromString(
                        "7f0b5416-5322-4c34-873e-b96aa3f62639"
                )
        );
    }

    private EmailAddress createEmail() {
        return new EmailAddress("matheus@ecofy.com");
    }

    private PasswordHash createPasswordHash() {
        return new PasswordHash("$2a$12$hashSeguro");
    }
}
