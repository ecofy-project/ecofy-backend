package br.com.ecofy.auth.core.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Testes unitários do domínio Role")
class RoleTest {

    @Test
    @DisplayName("Deve criar o papel normalizando o nome e preservando seus atributos")
    void constructor_valoresValidos_deveCriarRoleNormalizado() {
        // Arrange
        Permission readPermission = createPermission("transactions:read");
        Permission writePermission = createPermission("transactions:write");
        Set<Permission> permissions = Set.of(readPermission, writePermission);

        // Act
        Role role = new Role(
                "  ROLE_ADMIN  ",
                "Administrador do sistema",
                permissions
        );

        // Assert
        assertAll(
                () -> assertEquals("ROLE_ADMIN", role.name()),
                () -> assertEquals("Administrador do sistema", role.description()),
                () -> assertEquals(permissions, role.permissions()),
                () -> assertEquals(2, role.permissions().size())
        );
    }

    @Test
    @DisplayName("Deve permitir a criação do papel com descrição nula")
    void constructor_descricaoNula_deveCriarRole() {
        // Act
        Role role = new Role("ROLE_USER", null, Set.of());

        // Assert
        assertNull(role.description());
    }

    @Test
    @DisplayName("Deve criar o papel sem permissões quando o conjunto informado for nulo")
    void constructor_permissoesNulas_deveCriarRoleComConjuntoVazio() {
        // Act
        Role role = new Role("ROLE_USER", "Usuário", null);

        // Assert
        assertAll(
                () -> assertTrue(role.permissions().isEmpty()),
                () -> assertEquals(0, role.permissions().size())
        );
    }

    @Test
    @DisplayName("Deve realizar cópia defensiva do conjunto de permissões informado")
    void constructor_conjuntoDePermissoesMutavel_deveRealizarCopiaDefensiva() {
        // Arrange
        Permission permission = createPermission("transactions:read");
        Set<Permission> originalPermissions = new HashSet<>();
        originalPermissions.add(permission);

        Role role = new Role(
                "ROLE_USER",
                "Usuário",
                originalPermissions
        );

        // Act
        originalPermissions.clear();

        // Assert
        assertAll(
                () -> assertTrue(role.permissions().contains(permission)),
                () -> assertEquals(1, role.permissions().size()),
                () -> assertTrue(originalPermissions.isEmpty())
        );
    }

    @Test
    @DisplayName("Deve retornar um conjunto de permissões que não permite alterações externas")
    void permissions_tentativaDeAlteracao_deveLancarUnsupportedOperationException() {
        // Arrange
        Role role = new Role(
                "ROLE_USER",
                "Usuário",
                Set.of(createPermission("transactions:read"))
        );
        Permission newPermission = createPermission("transactions:write");

        // Act
        Set<Permission> returnedPermissions = role.permissions();

        // Assert
        assertThrows(
                UnsupportedOperationException.class,
                () -> returnedPermissions.add(newPermission)
        );
    }

    @Test
    @DisplayName("Deve lançar NullPointerException quando o nome do papel for nulo")
    void constructor_nomeNulo_deveLancarNullPointerException() {
        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new Role(null, "Usuário", Set.of())
        );

        // Assert
        assertEquals("name must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando o nome do papel estiver vazio")
    void constructor_nomeVazio_deveLancarIllegalArgumentException() {
        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Role("", "Usuário", Set.of())
        );

        // Assert
        assertEquals("name must not be blank", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando o nome do papel contiver apenas espaços")
    void constructor_nomeComApenasEspacos_deveLancarIllegalArgumentException() {
        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Role("   ", "Usuário", Set.of())
        );

        // Assert
        assertEquals("name must not be blank", exception.getMessage());
    }

    @Test
    @DisplayName("Deve retornar verdadeiro quando o papel possuir exatamente a permissão informada")
    void hasExactPermission_permissaoExistente_deveRetornarVerdadeiro() {
        // Arrange
        Role role = new Role(
                "ROLE_USER",
                "Usuário",
                Set.of(createPermission("transactions:read"))
        );

        // Act
        boolean result = role.hasExactPermission("transactions:read");

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Deve retornar falso quando o papel não possuir exatamente a permissão informada")
    void hasExactPermission_permissaoInexistente_deveRetornarFalso() {
        // Arrange
        Role role = new Role(
                "ROLE_USER",
                "Usuário",
                Set.of(createPermission("transactions:read"))
        );

        // Act
        boolean result = role.hasExactPermission("transactions:write");

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Deve comparar o nome da permissão exatamente sem normalizar o valor consultado")
    void hasExactPermission_nomeComEspacos_deveRetornarFalso() {
        // Arrange
        Role role = new Role(
                "ROLE_USER",
                "Usuário",
                Set.of(createPermission("transactions:read"))
        );

        // Act
        boolean result = role.hasExactPermission(" transactions:read ");

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Deve retornar falso ao consultar permissão exata em um papel sem permissões")
    void hasExactPermission_roleSemPermissoes_deveRetornarFalso() {
        // Arrange
        Role role = new Role("ROLE_USER", "Usuário", Set.of());

        // Act
        boolean result = role.hasExactPermission("transactions:read");

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Deve lançar NullPointerException quando o nome da permissão exata for nulo")
    void hasExactPermission_nomeNulo_deveLancarNullPointerException() {
        // Arrange
        Role role = new Role("ROLE_USER", "Usuário", Set.of());

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> role.hasExactPermission(null)
        );

        // Assert
        assertEquals("permissionName must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("Deve retornar verdadeiro quando o papel conceder diretamente a permissão solicitada")
    void hasPermission_permissaoDireta_deveRetornarVerdadeiro() {
        // Arrange
        Role role = new Role(
                "ROLE_USER",
                "Usuário",
                Set.of(createPermission("transactions:read"))
        );

        // Act
        boolean result = role.hasPermission("transactions:read");

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Deve retornar verdadeiro quando o wildcard por prefixo conceder a permissão solicitada")
    void hasPermission_wildcardPorPrefixo_deveRetornarVerdadeiro() {
        // Arrange
        Permission wildcardPermission = new Permission(
                "transactions:*",
                "Gerencia transações",
                "billing"
        );
        Role role = new Role(
                "ROLE_ADMIN",
                "Administrador",
                Set.of(wildcardPermission)
        );

        // Act
        boolean result = role.hasPermission("transactions:write");

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Deve retornar verdadeiro quando o wildcard global conceder a permissão solicitada")
    void hasPermission_wildcardGlobal_deveRetornarVerdadeiro() {
        // Arrange
        Permission wildcardPermission = new Permission(
                "*",
                "Acesso irrestrito",
                "*"
        );
        Role role = new Role(
                "ROLE_ADMIN",
                "Administrador",
                Set.of(wildcardPermission)
        );

        // Act
        boolean result = role.hasPermission("users:delete");

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Deve retornar falso quando nenhuma permissão conceder a permissão solicitada")
    void hasPermission_permissaoNaoConcedida_deveRetornarFalso() {
        // Arrange
        Role role = new Role(
                "ROLE_USER",
                "Usuário",
                Set.of(createPermission("transactions:read"))
        );

        // Act
        boolean result = role.hasPermission("transactions:write");

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Deve lançar NullPointerException quando o nome da permissão solicitada for nulo")
    void hasPermission_nomeNulo_deveLancarNullPointerException() {
        // Arrange
        Role role = new Role("ROLE_USER", "Usuário", Set.of());

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> role.hasPermission(null)
        );

        // Assert
        assertEquals("permissionName must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando o nome da permissão solicitada estiver vazio")
    void hasPermission_nomeVazio_deveLancarIllegalArgumentException() {
        // Arrange
        Role role = new Role("ROLE_USER", "Usuário", Set.of());

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> role.hasPermission("")
        );

        // Assert
        assertEquals("name must not be blank", exception.getMessage());
    }

    @Test
    @DisplayName("Deve retornar verdadeiro quando alguma permissão do papel implicar a permissão informada")
    void implies_permissaoImplicada_deveRetornarVerdadeiro() {
        // Arrange
        Role role = new Role(
                "ROLE_ADMIN",
                "Administrador",
                Set.of(new Permission("transactions:*", null, "billing"))
        );
        Permission requestedPermission = new Permission(
                "transactions:read",
                null,
                "billing"
        );

        // Act
        boolean result = role.implies(requestedPermission);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Deve retornar falso quando nenhuma permissão do papel implicar a permissão informada")
    void implies_permissaoNaoImplicada_deveRetornarFalso() {
        // Arrange
        Role role = new Role(
                "ROLE_USER",
                "Usuário",
                Set.of(createPermission("transactions:read"))
        );
        Permission requestedPermission = createPermission("users:read");

        // Act
        boolean result = role.implies(requestedPermission);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Deve retornar falso ao verificar implicação em um papel sem permissões")
    void implies_roleSemPermissoes_deveRetornarFalso() {
        // Arrange
        Role role = new Role("ROLE_USER", "Usuário", Set.of());
        Permission requestedPermission = createPermission("transactions:read");

        // Act
        boolean result = role.implies(requestedPermission);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Deve lançar NullPointerException quando a permissão a verificar for nula")
    void implies_permissaoNula_deveLancarNullPointerException() {
        // Arrange
        Role role = new Role("ROLE_USER", "Usuário", Set.of());

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> role.implies(null)
        );

        // Assert
        assertEquals("permission must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("Deve retornar novo papel com a permissão adicionada sem alterar o papel original")
    void withPermission_novaPermissao_deveRetornarNovoRoleComPermissao() {
        // Arrange
        Permission readPermission = createPermission("transactions:read");
        Permission writePermission = createPermission("transactions:write");
        Role originalRole = new Role(
                "ROLE_USER",
                "Usuário",
                Set.of(readPermission)
        );

        // Act
        Role updatedRole = originalRole.withPermission(writePermission);

        // Assert
        assertAll(
                () -> assertNotSame(originalRole, updatedRole),
                () -> assertEquals(originalRole.name(), updatedRole.name()),
                () -> assertEquals(originalRole.description(), updatedRole.description()),
                () -> assertEquals(1, originalRole.permissions().size()),
                () -> assertTrue(originalRole.permissions().contains(readPermission)),
                () -> assertFalse(originalRole.permissions().contains(writePermission)),
                () -> assertEquals(2, updatedRole.permissions().size()),
                () -> assertTrue(updatedRole.permissions().contains(readPermission)),
                () -> assertTrue(updatedRole.permissions().contains(writePermission))
        );
    }

    @Test
    @DisplayName("Deve manter uma única ocorrência quando a permissão adicionada já existir")
    void withPermission_permissaoJaExistente_deveManterConjuntoSemDuplicidade() {
        // Arrange
        Permission permission = createPermission("transactions:read");
        Role originalRole = new Role(
                "ROLE_USER",
                "Usuário",
                Set.of(permission)
        );

        // Act
        Role updatedRole = originalRole.withPermission(
                new Permission("transactions:read", "Outra descrição", "auth")
        );

        // Assert
        assertAll(
                () -> assertNotSame(originalRole, updatedRole),
                () -> assertEquals(1, originalRole.permissions().size()),
                () -> assertEquals(1, updatedRole.permissions().size())
        );
    }

    @Test
    @DisplayName("Deve lançar NullPointerException ao tentar adicionar uma permissão nula")
    void withPermission_permissaoNula_deveLancarNullPointerException() {
        // Arrange
        Role role = new Role("ROLE_USER", "Usuário", Set.of());

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> role.withPermission(null)
        );

        // Assert
        assertEquals("permission must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("Deve retornar novo papel sem a permissão removida e preservar o papel original")
    void withoutPermission_permissaoExistente_deveRetornarNovoRoleSemPermissao() {
        // Arrange
        Permission readPermission = createPermission("transactions:read");
        Permission writePermission = createPermission("transactions:write");
        Role originalRole = new Role(
                "ROLE_USER",
                "Usuário",
                Set.of(readPermission, writePermission)
        );

        // Act
        Role updatedRole = originalRole.withoutPermission(readPermission);

        // Assert
        assertAll(
                () -> assertNotSame(originalRole, updatedRole),
                () -> assertEquals(originalRole.name(), updatedRole.name()),
                () -> assertEquals(originalRole.description(), updatedRole.description()),
                () -> assertEquals(2, originalRole.permissions().size()),
                () -> assertTrue(originalRole.permissions().contains(readPermission)),
                () -> assertTrue(originalRole.permissions().contains(writePermission)),
                () -> assertEquals(1, updatedRole.permissions().size()),
                () -> assertFalse(updatedRole.permissions().contains(readPermission)),
                () -> assertTrue(updatedRole.permissions().contains(writePermission))
        );
    }

    @Test
    @DisplayName("Deve retornar novo papel inalterado quando a permissão a remover não existir")
    void withoutPermission_permissaoInexistente_deveRetornarNovoRoleComMesmoConjunto() {
        // Arrange
        Permission existingPermission = createPermission("transactions:read");
        Role originalRole = new Role(
                "ROLE_USER",
                "Usuário",
                Set.of(existingPermission)
        );

        // Act
        Role updatedRole = originalRole.withoutPermission(
                createPermission("transactions:write")
        );

        // Assert
        assertAll(
                () -> assertNotSame(originalRole, updatedRole),
                () -> assertEquals(originalRole.permissions(), updatedRole.permissions()),
                () -> assertEquals(1, updatedRole.permissions().size())
        );
    }

    @Test
    @DisplayName("Deve lançar NullPointerException ao tentar remover uma permissão nula")
    void withoutPermission_permissaoNula_deveLancarNullPointerException() {
        // Arrange
        Role role = new Role("ROLE_USER", "Usuário", Set.of());

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> role.withoutPermission(null)
        );

        // Assert
        assertEquals("permission must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("Deve considerar a mesma instância de papel igual a si própria")
    void equals_mesmaInstancia_deveRetornarVerdadeiro() {
        // Arrange
        Role role = new Role("ROLE_USER", "Usuário", Set.of());

        // Act
        boolean result = role.equals(role);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Deve considerar iguais papéis com o mesmo nome independentemente dos demais atributos")
    void equals_mesmoNomeComOutrosAtributosDiferentes_deveRetornarVerdadeiro() {
        // Arrange
        Role firstRole = new Role(
                "ROLE_USER",
                "Primeira descrição",
                Set.of(createPermission("transactions:read"))
        );
        Role secondRole = new Role(
                "ROLE_USER",
                "Segunda descrição",
                Set.of(createPermission("transactions:write"))
        );

        // Act
        boolean result = firstRole.equals(secondRole);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Deve considerar diferentes papéis com nomes distintos")
    void equals_nomesDiferentes_deveRetornarFalso() {
        // Arrange
        Role userRole = new Role("ROLE_USER", "Usuário", Set.of());
        Role adminRole = new Role("ROLE_ADMIN", "Administrador", Set.of());

        // Act
        boolean result = userRole.equals(adminRole);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Deve considerar o papel diferente de nulo e de objetos de outro tipo")
    void equals_objetosIncompativeis_deveRetornarFalso() {
        // Arrange
        Role role = new Role("ROLE_USER", "Usuário", Set.of());

        // Act
        boolean equalToNull = role.equals(null);
        boolean equalToString = role.equals("ROLE_USER");

        // Assert
        assertAll(
                () -> assertFalse(equalToNull),
                () -> assertFalse(equalToString)
        );
    }

    @Test
    @DisplayName("Deve gerar o mesmo hash para papéis com o mesmo nome")
    void hashCode_mesmoNome_deveRetornarMesmoHash() {
        // Arrange
        Role firstRole = new Role(
                "ROLE_USER",
                "Primeira descrição",
                Set.of(createPermission("transactions:read"))
        );
        Role secondRole = new Role(
                "ROLE_USER",
                "Segunda descrição",
                Set.of(createPermission("transactions:write"))
        );

        // Act
        int firstHash = firstRole.hashCode();
        int secondHash = secondRole.hashCode();

        // Assert
        assertEquals(firstHash, secondHash);
    }

    @Test
    @DisplayName("Deve retornar representação textual contendo o nome e a quantidade de permissões")
    void toString_roleValido_deveRetornarRepresentacaoEsperada() {
        // Arrange
        Role role = new Role(
                "ROLE_ADMIN",
                "Descrição confidencial",
                Set.of(
                        createPermission("transactions:read"),
                        createPermission("transactions:write")
                )
        );

        // Act
        String representation = role.toString();

        // Assert
        assertAll(
                () -> assertEquals(
                        "Role{name='ROLE_ADMIN', permissionsCount=2}",
                        representation
                ),
                () -> assertFalse(representation.contains("Descrição confidencial")),
                () -> assertFalse(representation.contains("transactions:read")),
                () -> assertFalse(representation.contains("transactions:write"))
        );
    }

    private Permission createPermission(String name) {
        return new Permission(name, null, "*");
    }
}
