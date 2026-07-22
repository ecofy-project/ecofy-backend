package br.com.ecofy.auth.adapters.out.security;

import br.com.ecofy.auth.core.domain.valueobject.PasswordHash;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários do adaptador de hash de senhas com BCrypt")
class BCryptPasswordHashingAdapterTest {

    private static final String RAW_PASSWORD = "Senha@Segura123";
    private static final String ENCODED_PASSWORD =
            "$2a$10$abcdefghijklmnopqrstuvwxyz012345678901234567890";

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Deve rejeitar PasswordEncoder nulo ao construir o adaptador")
    void constructor_passwordEncoderNulo_deveLancarNullPointerException() {
        // Arrange
        PasswordEncoder nullPasswordEncoder = null;

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new BCryptPasswordHashingAdapter(
                        nullPasswordEncoder
                )
        );

        // Assert
        assertEquals(
                "passwordEncoder must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar senha nula sem acionar o encoder")
    void hash_senhaNula_deveLancarNullPointerException() {
        // Arrange
        BCryptPasswordHashingAdapter adapter = createAdapter();

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.hash(null)
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "rawPassword must not be null",
                        exception.getMessage()
                ),
                () -> verifyNoInteractions(passwordEncoder)
        );
    }

    @Test
    @DisplayName("Deve rejeitar senha vazia sem acionar o encoder")
    void hash_senhaVazia_deveLancarIllegalArgumentException() {
        // Arrange
        BCryptPasswordHashingAdapter adapter = createAdapter();

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adapter.hash("")
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "rawPassword must not be blank",
                        exception.getMessage()
                ),
                () -> verifyNoInteractions(passwordEncoder)
        );
    }

    @Test
    @DisplayName("Deve rejeitar senha contendo apenas espaços sem acionar o encoder")
    void hash_senhaEmBranco_deveLancarIllegalArgumentException() {
        // Arrange
        BCryptPasswordHashingAdapter adapter = createAdapter();

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adapter.hash("   ")
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "rawPassword must not be blank",
                        exception.getMessage()
                ),
                () -> verifyNoInteractions(passwordEncoder)
        );
    }

    @Test
    @DisplayName("Deve codificar a senha válida e retornar o hash gerado")
    void hash_senhaValida_deveRetornarPasswordHash() {
        // Arrange
        when(passwordEncoder.encode(RAW_PASSWORD))
                .thenReturn(ENCODED_PASSWORD);

        BCryptPasswordHashingAdapter adapter = createAdapter();

        // Act
        PasswordHash result = adapter.hash(RAW_PASSWORD);

        // Assert
        assertAll(
                () -> assertEquals(
                        ENCODED_PASSWORD,
                        result.value()
                ),
                () -> verify(passwordEncoder)
                        .encode(RAW_PASSWORD)
        );
    }

    @Test
    @DisplayName("Deve propagar a exceção quando o encoder falhar ao gerar o hash")
    void hash_encoderLancaExcecao_devePropagarExcecao() {
        // Arrange
        RuntimeException encoderException =
                new RuntimeException("Falha ao gerar hash");

        when(passwordEncoder.encode(RAW_PASSWORD))
                .thenThrow(encoderException);

        BCryptPasswordHashingAdapter adapter = createAdapter();

        // Act
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> adapter.hash(RAW_PASSWORD)
        );

        // Assert
        assertSame(encoderException, exception);

        verify(passwordEncoder).encode(RAW_PASSWORD);
    }

    @Test
    @DisplayName("Deve rejeitar senha nula ao verificar correspondência")
    void matches_senhaNula_deveLancarNullPointerException() {
        // Arrange
        PasswordHash hash = new PasswordHash(ENCODED_PASSWORD);
        BCryptPasswordHashingAdapter adapter = createAdapter();

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.matches(null, hash)
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "rawPassword must not be null",
                        exception.getMessage()
                ),
                () -> verifyNoInteractions(passwordEncoder)
        );
    }

    @Test
    @DisplayName("Deve rejeitar hash nulo ao verificar correspondência")
    void matches_hashNulo_deveLancarNullPointerException() {
        // Arrange
        BCryptPasswordHashingAdapter adapter = createAdapter();

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.matches(RAW_PASSWORD, null)
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "hash must not be null",
                        exception.getMessage()
                ),
                () -> verifyNoInteractions(passwordEncoder)
        );
    }

    @Test
    @DisplayName("Deve retornar verdadeiro quando a senha corresponder ao hash")
    void matches_senhaCorrespondente_deveRetornarTrue() {
        // Arrange
        PasswordHash hash = new PasswordHash(ENCODED_PASSWORD);

        when(
                passwordEncoder.matches(
                        RAW_PASSWORD,
                        ENCODED_PASSWORD
                )
        ).thenReturn(true);

        BCryptPasswordHashingAdapter adapter = createAdapter();

        // Act
        boolean result = adapter.matches(RAW_PASSWORD, hash);

        // Assert
        assertTrue(result);

        verify(passwordEncoder).matches(
                RAW_PASSWORD,
                ENCODED_PASSWORD
        );
    }

    @Test
    @DisplayName("Deve retornar falso quando a senha não corresponder ao hash")
    void matches_senhaNaoCorrespondente_deveRetornarFalse() {
        // Arrange
        PasswordHash hash = new PasswordHash(ENCODED_PASSWORD);

        when(
                passwordEncoder.matches(
                        RAW_PASSWORD,
                        ENCODED_PASSWORD
                )
        ).thenReturn(false);

        BCryptPasswordHashingAdapter adapter = createAdapter();

        // Act
        boolean result = adapter.matches(RAW_PASSWORD, hash);

        // Assert
        assertFalse(result);

        verify(passwordEncoder).matches(
                RAW_PASSWORD,
                ENCODED_PASSWORD
        );
    }

    @Test
    @DisplayName("Deve encaminhar senha vazia ao encoder durante a verificação")
    void matches_senhaVazia_deveDelegarVerificacaoAoEncoder() {
        // Arrange
        String emptyPassword = "";
        PasswordHash hash = new PasswordHash(ENCODED_PASSWORD);

        when(
                passwordEncoder.matches(
                        emptyPassword,
                        ENCODED_PASSWORD
                )
        ).thenReturn(false);

        BCryptPasswordHashingAdapter adapter = createAdapter();

        // Act
        boolean result = adapter.matches(emptyPassword, hash);

        // Assert
        assertFalse(result);

        verify(passwordEncoder).matches(
                emptyPassword,
                ENCODED_PASSWORD
        );
    }

    @Test
    @DisplayName("Deve encaminhar senha em branco ao encoder durante a verificação")
    void matches_senhaEmBranco_deveDelegarVerificacaoAoEncoder() {
        // Arrange
        String blankPassword = "   ";
        PasswordHash hash = new PasswordHash(ENCODED_PASSWORD);

        when(
                passwordEncoder.matches(
                        blankPassword,
                        ENCODED_PASSWORD
                )
        ).thenReturn(false);

        BCryptPasswordHashingAdapter adapter = createAdapter();

        // Act
        boolean result = adapter.matches(blankPassword, hash);

        // Assert
        assertFalse(result);

        verify(passwordEncoder).matches(
                blankPassword,
                ENCODED_PASSWORD
        );
    }

    @Test
    @DisplayName("Deve propagar a exceção quando o encoder falhar durante a verificação")
    void matches_encoderLancaExcecao_devePropagarExcecao() {
        // Arrange
        PasswordHash hash = new PasswordHash(ENCODED_PASSWORD);
        RuntimeException encoderException =
                new RuntimeException("Falha ao verificar senha");

        when(
                passwordEncoder.matches(
                        RAW_PASSWORD,
                        ENCODED_PASSWORD
                )
        ).thenThrow(encoderException);

        BCryptPasswordHashingAdapter adapter = createAdapter();

        // Act
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> adapter.matches(RAW_PASSWORD, hash)
        );

        // Assert
        assertSame(encoderException, exception);

        verify(passwordEncoder).matches(
                RAW_PASSWORD,
                ENCODED_PASSWORD
        );
        verify(passwordEncoder, never()).encode(RAW_PASSWORD);
    }

    private BCryptPasswordHashingAdapter createAdapter() {
        return new BCryptPasswordHashingAdapter(passwordEncoder);
    }
}
