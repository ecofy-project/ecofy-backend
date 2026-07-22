package br.com.ecofy.auth.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários da validação de chaves JWT em produção")
class JwtProdKeyGuardTest {

    private static final String ACTIVE_KID = "prod-signing-key";

    @Mock
    private KeysProperties keysProperties;

    @Test
    @DisplayName("Deve rejeitar propriedades de chaves nulas durante a validação")
    void verifyProductionKeysAreConfigured_propriedadesNulas_deveLancarNullPointerException() {
        // Arrange
        JwtProdKeyGuard guard = new JwtProdKeyGuard(null);

        // Act
        // Assert
        assertThrows(
                NullPointerException.class,
                guard::verifyProductionKeysAreConfigured
        );
    }

    @Test
    @DisplayName("Deve rejeitar geração de chave em memória no ambiente de produção")
    void verifyProductionKeysAreConfigured_geracaoEmMemoriaPermitida_deveLancarIllegalStateException() {
        // Arrange
        when(keysProperties.isAllowGeneratedKey())
                .thenReturn(true);

        JwtProdKeyGuard guard = createGuard();

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                guard::verifyProductionKeysAreConfigured
        );

        // Assert
        assertEquals(
                "Profile 'prod' não permite geração de chave em memória. "
                        + "Defina ecofy.auth.keys.allow-generated-key=false "
                        + "e forneça a chave por secret externo.",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar identificador nulo para a chave ativa")
    void verifyProductionKeysAreConfigured_activeKidNulo_deveLancarIllegalStateException() {
        // Arrange
        when(keysProperties.isAllowGeneratedKey())
                .thenReturn(false);
        when(keysProperties.getActiveKid()).thenReturn(null);

        JwtProdKeyGuard guard = createGuard();

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                guard::verifyProductionKeysAreConfigured
        );

        // Assert
        assertEquals(
                "Profile 'prod' requer ecofy.auth.keys.active-kid "
                        + "(o kid vai no header do JWT e no JWKS).",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar identificador vazio para a chave ativa")
    void verifyProductionKeysAreConfigured_activeKidVazio_deveLancarIllegalStateException() {
        // Arrange
        when(keysProperties.isAllowGeneratedKey())
                .thenReturn(false);
        when(keysProperties.getActiveKid()).thenReturn("");

        JwtProdKeyGuard guard = createGuard();

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                guard::verifyProductionKeysAreConfigured
        );

        // Assert
        assertEquals(
                "Profile 'prod' requer ecofy.auth.keys.active-kid "
                        + "(o kid vai no header do JWT e no JWKS).",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar identificador contendo apenas espaços para a chave ativa")
    void verifyProductionKeysAreConfigured_activeKidEmBranco_deveLancarIllegalStateException() {
        // Arrange
        when(keysProperties.isAllowGeneratedKey())
                .thenReturn(false);
        when(keysProperties.getActiveKid()).thenReturn("   ");

        JwtProdKeyGuard guard = createGuard();

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                guard::verifyProductionKeysAreConfigured
        );

        // Assert
        assertEquals(
                "Profile 'prod' requer ecofy.auth.keys.active-kid "
                        + "(o kid vai no header do JWT e no JWKS).",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar ausência das duas fontes de chave privada")
    void verifyProductionKeysAreConfigured_fontesDeChaveNulas_deveLancarIllegalStateException() {
        // Arrange
        configureSafeBaseProperties();

        when(keysProperties.getActivePrivateKey())
                .thenReturn(null);
        when(keysProperties.getActivePrivateKeyLocation())
                .thenReturn(null);

        JwtProdKeyGuard guard = createGuard();

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                guard::verifyProductionKeysAreConfigured
        );

        // Assert
        assertEquals(
                "Profile 'prod' requer a chave de assinatura por fonte externa "
                        + "(ecofy.auth.keys.active-private-key "
                        + "ou active-private-key-location).",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar fontes de chave privada vazias")
    void verifyProductionKeysAreConfigured_fontesDeChaveVazias_deveLancarIllegalStateException() {
        // Arrange
        configureSafeBaseProperties();

        when(keysProperties.getActivePrivateKey())
                .thenReturn("");
        when(keysProperties.getActivePrivateKeyLocation())
                .thenReturn("");

        JwtProdKeyGuard guard = createGuard();

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                guard::verifyProductionKeysAreConfigured
        );

        // Assert
        assertEquals(
                "Profile 'prod' requer a chave de assinatura por fonte externa "
                        + "(ecofy.auth.keys.active-private-key "
                        + "ou active-private-key-location).",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar fontes de chave privada contendo apenas espaços")
    void verifyProductionKeysAreConfigured_fontesDeChaveEmBranco_deveLancarIllegalStateException() {
        // Arrange
        configureSafeBaseProperties();

        when(keysProperties.getActivePrivateKey())
                .thenReturn("   ");
        when(keysProperties.getActivePrivateKeyLocation())
                .thenReturn("\t");

        JwtProdKeyGuard guard = createGuard();

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                guard::verifyProductionKeysAreConfigured
        );

        // Assert
        assertEquals(
                "Profile 'prod' requer a chave de assinatura por fonte externa "
                        + "(ecofy.auth.keys.active-private-key "
                        + "ou active-private-key-location).",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve aceitar chave privada fornecida diretamente sem localização externa")
    void verifyProductionKeysAreConfigured_chaveInlineValida_deveConcluirSemExcecao() {
        // Arrange
        configureSafeBaseProperties();

        when(keysProperties.getActivePrivateKey())
                .thenReturn("-----BEGIN PRIVATE KEY-----");
        when(keysProperties.getActivePrivateKeyLocation())
                .thenReturn(null);

        JwtProdKeyGuard guard = createGuard();

        // Act
        // Assert
        assertDoesNotThrow(
                guard::verifyProductionKeysAreConfigured
        );
    }

    @Test
    @DisplayName("Deve aceitar chave privada localizada em arquivo externo")
    void verifyProductionKeysAreConfigured_localizacaoFileValida_deveConcluirSemExcecao() {
        // Arrange
        configureSafeBaseProperties();

        when(keysProperties.getActivePrivateKey())
                .thenReturn(null);
        when(keysProperties.getActivePrivateKeyLocation())
                .thenReturn("file:/run/secrets/private-key.pem");

        JwtProdKeyGuard guard = createGuard();

        // Act
        // Assert
        assertDoesNotThrow(
                guard::verifyProductionKeysAreConfigured
        );
    }

    @Test
    @DisplayName("Deve aceitar localização externa com espaços e letras maiúsculas")
    void verifyProductionKeysAreConfigured_localizacaoFileNormalizada_deveConcluirSemExcecao() {
        // Arrange
        configureSafeBaseProperties();

        when(keysProperties.getActivePrivateKey())
                .thenReturn(null);
        when(keysProperties.getActivePrivateKeyLocation())
                .thenReturn("  FILE:/run/secrets/private-key.pem  ");

        JwtProdKeyGuard guard = createGuard();

        // Act
        // Assert
        assertDoesNotThrow(
                guard::verifyProductionKeysAreConfigured
        );
    }

    @Test
    @DisplayName("Deve rejeitar chave privada localizada diretamente no classpath")
    void verifyProductionKeysAreConfigured_localizacaoClasspath_deveLancarIllegalStateException() {
        // Arrange
        configureSafeBaseProperties();

        when(keysProperties.getActivePrivateKey())
                .thenReturn(null);
        when(keysProperties.getActivePrivateKeyLocation())
                .thenReturn("classpath:keys/private-key.pem");

        JwtProdKeyGuard guard = createGuard();

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                guard::verifyProductionKeysAreConfigured
        );

        // Assert
        assertEquals(
                "Profile 'prod' não pode usar chave privada empacotada no classpath. "
                        + "Use file:/ (secret montado) ou injete o PEM "
                        + "por variável de ambiente.",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar localização no classpath ignorando espaços e diferenças de caixa")
    void verifyProductionKeysAreConfigured_localizacaoClasspathNormalizada_deveLancarIllegalStateException() {
        // Arrange
        configureSafeBaseProperties();

        when(keysProperties.getActivePrivateKey())
                .thenReturn(null);
        when(keysProperties.getActivePrivateKeyLocation())
                .thenReturn("  ClAsSpAtH:keys/private-key.pem  ");

        JwtProdKeyGuard guard = createGuard();

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                guard::verifyProductionKeysAreConfigured
        );

        // Assert
        assertEquals(
                "Profile 'prod' não pode usar chave privada empacotada no classpath. "
                        + "Use file:/ (secret montado) ou injete o PEM "
                        + "por variável de ambiente.",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar localização no classpath mesmo quando houver chave inline")
    void verifyProductionKeysAreConfigured_chaveInlineEClasspath_deveLancarIllegalStateException() {
        // Arrange
        configureSafeBaseProperties();

        when(keysProperties.getActivePrivateKey())
                .thenReturn("-----BEGIN PRIVATE KEY-----");
        when(keysProperties.getActivePrivateKeyLocation())
                .thenReturn("classpath:keys/private-key.pem");

        JwtProdKeyGuard guard = createGuard();

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                guard::verifyProductionKeysAreConfigured
        );

        // Assert
        assertEquals(
                "Profile 'prod' não pode usar chave privada empacotada no classpath. "
                        + "Use file:/ (secret montado) ou injete o PEM "
                        + "por variável de ambiente.",
                exception.getMessage()
        );
    }

    private JwtProdKeyGuard createGuard() {
        return new JwtProdKeyGuard(keysProperties);
    }

    private void configureSafeBaseProperties() {
        when(keysProperties.isAllowGeneratedKey())
                .thenReturn(false);
        when(keysProperties.getActiveKid())
                .thenReturn(ACTIVE_KID);
    }
}
