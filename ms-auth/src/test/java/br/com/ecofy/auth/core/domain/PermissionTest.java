package br.com.ecofy.auth.core.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Testes unitários do value object Permission")
class PermissionTest {

    @Test
    @DisplayName("Deve criar a permissão normalizando o nome e o domínio e preservando a descrição")
    void constructor_valoresValidos_deveCriarPermissaoNormalizada() {
        // Arrange
        String name = "  transactions:read  ";
        String description = "Permite consultar transações";
        String domain = "  BILLING  ";

        // Act
        Permission permission = new Permission(name, description, domain);

        // Assert
        assertAll(
                () -> assertEquals("transactions:read", permission.name()),
                () -> assertEquals(description, permission.description()),
                () -> assertEquals("billing", permission.domain())
        );
    }

    @Test
    @DisplayName("Deve permitir a criação da permissão com descrição nula")
    void constructor_descricaoNula_deveCriarPermissao() {
        // Act
        Permission permission = new Permission("transactions:read", null, "billing");

        // Assert
        assertNull(permission.description());
    }

    @Test
    @DisplayName("Deve utilizar o domínio global quando o domínio informado for nulo")
    void constructor_dominioNulo_deveUtilizarDominioGlobal() {
        // Act
        Permission permission = new Permission("transactions:read", null, null);

        // Assert
        assertEquals("*", permission.domain());
    }

    @Test
    @DisplayName("Deve utilizar o domínio global quando o domínio informado estiver vazio")
    void constructor_dominioVazio_deveUtilizarDominioGlobal() {
        // Act
        Permission permission = new Permission("transactions:read", null, "");

        // Assert
        assertEquals("*", permission.domain());
    }

    @Test
    @DisplayName("Deve utilizar o domínio global quando o domínio contiver apenas espaços")
    void constructor_dominioComApenasEspacos_deveUtilizarDominioGlobal() {
        // Act
        Permission permission = new Permission("transactions:read", null, "   ");

        // Assert
        assertEquals("*", permission.domain());
    }

    @Test
    @DisplayName("Deve lançar NullPointerException quando o nome da permissão for nulo")
    void constructor_nomeNulo_deveLancarNullPointerException() {
        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new Permission(null, "Descrição", "auth")
        );

        // Assert
        assertEquals("name must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando o nome da permissão estiver vazio")
    void constructor_nomeVazio_deveLancarIllegalArgumentException() {
        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Permission("", "Descrição", "auth")
        );

        // Assert
        assertEquals("name must not be blank", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando o nome contiver apenas espaços")
    void constructor_nomeComApenasEspacos_deveLancarIllegalArgumentException() {
        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Permission("   ", "Descrição", "auth")
        );

        // Assert
        assertEquals("name must not be blank", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar NullPointerException ao verificar implicação com permissão nula")
    void implies_permissaoNula_deveLancarNullPointerException() {
        // Arrange
        Permission permission = new Permission("transactions:read", null, "billing");

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> permission.implies((Permission) null)
        );

        // Assert
        assertEquals("other must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("Deve conceder qualquer permissão quando possuir wildcard global")
    void implies_wildcardGlobal_deveRetornarVerdadeiro() {
        // Arrange
        Permission wildcard = new Permission("*", null, "auth");
        Permission other = new Permission("transactions:write", null, "billing");

        // Act
        boolean result = wildcard.implies(other);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Deve negar implicação quando os domínios específicos forem diferentes")
    void implies_dominiosEspecificosDiferentes_deveRetornarFalso() {
        // Arrange
        Permission permission = new Permission("transactions:read", null, "auth");
        Permission other = new Permission("transactions:read", null, "billing");

        // Act
        boolean result = permission.implies(other);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Deve implicar permissão com o mesmo nome e o mesmo domínio")
    void implies_mesmoNomeEMesmoDominio_deveRetornarVerdadeiro() {
        // Arrange
        Permission permission = new Permission("transactions:read", null, "billing");
        Permission other = new Permission(
                "transactions:read",
                "Descrição diferente",
                "BILLING"
        );

        // Act
        boolean result = permission.implies(other);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Deve implicar permissão com o mesmo nome quando o domínio atual for global")
    void implies_dominioAtualGlobal_deveRetornarVerdadeiro() {
        // Arrange
        Permission permission = new Permission("transactions:read", null, "*");
        Permission other = new Permission("transactions:read", null, "billing");

        // Act
        boolean result = permission.implies(other);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Deve implicar permissão com o mesmo nome quando o outro domínio for global")
    void implies_outroDominioGlobal_deveRetornarVerdadeiro() {
        // Arrange
        Permission permission = new Permission("transactions:read", null, "billing");
        Permission other = new Permission("transactions:read", null, "*");

        // Act
        boolean result = permission.implies(other);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Deve implicar permissão cujo nome corresponda ao prefixo do wildcard")
    void implies_wildcardPorPrefixoCorrespondente_deveRetornarVerdadeiro() {
        // Arrange
        Permission permission = new Permission("transactions:*", null, "billing");
        Permission other = new Permission("transactions:write", null, "billing");

        // Act
        boolean result = permission.implies(other);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Deve negar implicação quando o nome não corresponder ao prefixo do wildcard")
    void implies_wildcardPorPrefixoNaoCorrespondente_deveRetornarFalso() {
        // Arrange
        Permission permission = new Permission("transactions:*", null, "billing");
        Permission other = new Permission("budgets:read", null, "billing");

        // Act
        boolean result = permission.implies(other);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Deve negar wildcard por prefixo quando os domínios específicos forem diferentes")
    void implies_wildcardPorPrefixoComDominioDiferente_deveRetornarFalso() {
        // Arrange
        Permission permission = new Permission("transactions:*", null, "auth");
        Permission other = new Permission("transactions:read", null, "billing");

        // Act
        boolean result = permission.implies(other);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Deve negar implicação entre nomes distintos sem wildcard")
    void implies_nomesDiferentesSemWildcard_deveRetornarFalso() {
        // Arrange
        Permission permission = new Permission("transactions:read", null, "billing");
        Permission other = new Permission("transactions:write", null, "billing");

        // Act
        boolean result = permission.implies(other);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Deve verificar implicação a partir de um nome bruto normalizado")
    void implies_nomeBrutoCorrespondente_deveRetornarVerdadeiro() {
        // Arrange
        Permission permission = new Permission("transactions:*", null, "billing");

        // Act
        boolean result = permission.implies("  transactions:read  ");

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Deve lançar NullPointerException quando o nome bruto informado for nulo")
    void implies_nomeBrutoNulo_deveLancarNullPointerException() {
        // Arrange
        Permission permission = new Permission("transactions:*", null, "billing");

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> permission.implies((String) null)
        );

        // Assert
        assertEquals("name must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("Deve identificar uma permissão com wildcard global")
    void isWildcard_nomeGlobal_deveRetornarVerdadeiro() {
        // Arrange
        Permission permission = new Permission("*", null, "auth");

        // Act
        boolean result = permission.isWildcard();

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Deve rejeitar como wildcard global uma permissão específica")
    void isWildcard_nomeEspecifico_deveRetornarFalso() {
        // Arrange
        Permission permission = new Permission("transactions:read", null, "auth");

        // Act
        boolean result = permission.isWildcard();

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Deve identificar um wildcard válido por prefixo")
    void isDomainWildcardName_wildcardPorPrefixoValido_deveRetornarVerdadeiro() {
        // Arrange
        Permission permission = new Permission("transactions:*", null, "billing");

        // Act
        boolean result = permission.isDomainWildcardName();

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Deve rejeitar como wildcard por prefixo um nome sem o sufixo esperado")
    void isDomainWildcardName_nomeSemSufixoWildcard_deveRetornarFalso() {
        // Arrange
        Permission permission = new Permission("transactions:read", null, "billing");

        // Act
        boolean result = permission.isDomainWildcardName();

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Deve rejeitar como wildcard por prefixo o nome mínimo sem prefixo")
    void isDomainWildcardName_nomeSemPrefixo_deveRetornarFalso() {
        // Arrange
        Permission permission = new Permission(":*", null, "billing");

        // Act
        boolean result = permission.isDomainWildcardName();

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Deve considerar a mesma instância igual a si própria")
    void equals_mesmaInstancia_deveRetornarVerdadeiro() {
        // Arrange
        Permission permission = new Permission("transactions:read", null, "billing");

        // Act
        boolean result = permission.equals(permission);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Deve considerar iguais permissões com o mesmo nome")
    void equals_mesmoNomeComOutrosAtributosDiferentes_deveRetornarVerdadeiro() {
        // Arrange
        Permission first = new Permission(
                "transactions:read",
                "Primeira descrição",
                "billing"
        );
        Permission second = new Permission(
                "transactions:read",
                "Segunda descrição",
                "auth"
        );

        // Act
        boolean result = first.equals(second);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Deve considerar diferentes permissões com nomes distintos")
    void equals_nomesDiferentes_deveRetornarFalso() {
        // Arrange
        Permission first = new Permission("transactions:read", null, "billing");
        Permission second = new Permission("transactions:write", null, "billing");

        // Act
        boolean result = first.equals(second);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Deve considerar a permissão diferente de nulo e de outro tipo")
    void equals_objetosIncompativeis_deveRetornarFalso() {
        // Arrange
        Permission permission = new Permission("transactions:read", null, "billing");

        // Act
        boolean equalToNull = permission.equals(null);
        boolean equalToString = permission.equals("transactions:read");

        // Assert
        assertAll(
                () -> assertFalse(equalToNull),
                () -> assertFalse(equalToString)
        );
    }

    @Test
    @DisplayName("Deve gerar o mesmo hash para permissões com o mesmo nome")
    void hashCode_mesmoNome_deveRetornarMesmoHash() {
        // Arrange
        Permission first = new Permission("transactions:read", null, "billing");
        Permission second = new Permission(
                "transactions:read",
                "Outra descrição",
                "auth"
        );

        // Act
        int firstHash = first.hashCode();
        int secondHash = second.hashCode();

        // Assert
        assertEquals(firstHash, secondHash);
    }

    @Test
    @DisplayName("Deve gerar hashes diferentes para permissões com nomes distintos")
    void hashCode_nomesDiferentes_deveRetornarHashesDiferentes() {
        // Arrange
        Permission first = new Permission("transactions:read", null, "billing");
        Permission second = new Permission("transactions:write", null, "billing");

        // Act
        int firstHash = first.hashCode();
        int secondHash = second.hashCode();

        // Assert
        assertNotEquals(firstHash, secondHash);
    }

    @Test
    @DisplayName("Deve retornar representação textual contendo apenas o nome e o domínio")
    void toString_permissaoValida_deveRetornarRepresentacaoEsperada() {
        // Arrange
        Permission permission = new Permission(
                "transactions:read",
                "Descrição confidencial",
                "BILLING"
        );

        // Act
        String representation = permission.toString();

        // Assert
        assertAll(
                () -> assertEquals(
                        "Permission{name='transactions:read', domain='billing'}",
                        representation
                ),
                () -> assertFalse(representation.contains("Descrição confidencial"))
        );
    }
}
