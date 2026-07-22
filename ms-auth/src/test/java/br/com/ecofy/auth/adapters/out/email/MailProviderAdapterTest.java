package br.com.ecofy.auth.adapters.out.email;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import br.com.ecofy.auth.config.MailConfig;
import br.com.ecofy.auth.core.domain.AuthUser;
import br.com.ecofy.auth.core.domain.Role;
import br.com.ecofy.auth.core.domain.enums.AuthUserStatus;
import br.com.ecofy.auth.core.domain.valueobject.AuthUserId;
import br.com.ecofy.auth.core.domain.valueobject.EmailAddress;
import br.com.ecofy.auth.core.domain.valueobject.PasswordHash;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários do adaptador de envio de e-mails")
class MailProviderAdapterTest {

    private static final UUID USER_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private JavaMailSender mailSender;

    private MailConfig.EcofyMailProperties properties;
    private MailProviderAdapter adapter;
    private AuthUser user;

    @BeforeEach
    void setUp() {
        properties = new MailConfig.EcofyMailProperties();
        properties.setFrom("no-reply@ecofy.com");
        properties.setFrontendBaseUrl("https://app.ecofy.com");

        adapter = new MailProviderAdapter(mailSender, properties);
        user = authUser();
    }

    @Test
    @DisplayName("Deve rejeitar a criação quando o remetente de e-mail for nulo")
    void constructor_mailSenderNulo_deveLancarNullPointerException() {
        // Arrange
        JavaMailSender nullMailSender = null;

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new MailProviderAdapter(
                        nullMailSender,
                        properties
                )
        );

        // Assert
        assertEquals(
                "mailSender must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve rejeitar a criação quando as propriedades forem nulas")
    void constructor_propriedadesNulas_deveLancarNullPointerException() {
        // Arrange
        MailConfig.EcofyMailProperties nullProperties = null;

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new MailProviderAdapter(
                        mailSender,
                        nullProperties
                )
        );

        // Assert
        assertEquals(
                "props must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve enviar a mensagem de verificação com os valores configurados")
    void send_dadosValidos_deveEnviarMensagemDeVerificacao() {
        // Arrange
        String token = "verification-token-123";
        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);

        // Act
        adapter.send(user, token);

        // Assert
        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertEquals("no-reply@ecofy.com", message.getFrom());
        assertArrayEquals(
                new String[]{"user@ecofy.com"},
                message.getTo()
        );
        assertEquals(
                "Confirme seu e-mail – EcoFy",
                message.getSubject()
        );
        assertNotNull(message.getText());
        assertTrue(message.getText().contains("Matheus Lemes"));
        assertTrue(
                message.getText().contains(
                        "https://app.ecofy.com/auth/confirm-email"
                                + "?token=verification-token-123"
                )
        );
    }

    @Test
    @DisplayName("Deve usar os valores padrão ao enviar a verificação sem configurações opcionais")
    void send_configuracoesOpcionaisNulas_deveUsarValoresPadrao() {
        // Arrange
        properties.setFrom(null);
        properties.setFrontendBaseUrl(null);

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);

        // Act
        adapter.send(user, "verification-token-123");

        // Assert
        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertEquals(
                "no-reply@ecofy.local",
                message.getFrom()
        );
        assertNotNull(message.getText());
        assertTrue(
                message.getText().contains(
                        "http://localhost:3000/auth/confirm-email"
                                + "?token=verification-token-123"
                )
        );
    }

    @Test
    @DisplayName("Deve rejeitar o envio de verificação quando o usuário for nulo")
    void send_usuarioNulo_deveLancarNullPointerException() {
        // Arrange
        AuthUser nullUser = null;

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.send(
                        nullUser,
                        "verification-token-123"
                )
        );

        // Assert
        assertEquals("user must not be null", exception.getMessage());
        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("Deve rejeitar o envio de verificação quando o token for nulo")
    void send_tokenNulo_deveLancarNullPointerException() {
        // Arrange
        String nullToken = null;

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.send(user, nullToken)
        );

        // Assert
        assertEquals(
                "verificationToken must not be null",
                exception.getMessage()
        );
        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("Deve encapsular a falha ocorrida durante o envio da verificação")
    void send_falhaNoEnvio_deveLancarIllegalStateException() {
        // Arrange
        MailSendException mailException =
                new MailSendException("Falha no provedor");

        doThrow(mailException)
                .when(mailSender)
                .send(any(SimpleMailMessage.class));

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> adapter.send(
                        user,
                        "verification-token-123"
                )
        );

        // Assert
        assertEquals(
                "Failed to send email of type: verificação",
                exception.getMessage()
        );
        assertSame(mailException, exception.getCause());
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Deve enviar a mensagem de redefinição com os valores configurados")
    void sendReset_dadosValidos_deveEnviarMensagemDeRedefinicao() {
        // Arrange
        String token = "reset-token-123";
        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);

        // Act
        adapter.sendReset(user, token);

        // Assert
        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertEquals("no-reply@ecofy.com", message.getFrom());
        assertArrayEquals(
                new String[]{"user@ecofy.com"},
                message.getTo()
        );
        assertEquals(
                "Redefinição de senha – EcoFy",
                message.getSubject()
        );
        assertNotNull(message.getText());
        assertTrue(message.getText().contains("Matheus Lemes"));
        assertTrue(
                message.getText().contains(
                        "https://app.ecofy.com/auth/reset-password"
                                + "?token=reset-token-123"
                )
        );
    }

    @Test
    @DisplayName("Deve usar os valores padrão ao enviar a redefinição sem configurações opcionais")
    void sendReset_configuracoesOpcionaisNulas_deveUsarValoresPadrao() {
        // Arrange
        properties.setFrom(null);
        properties.setFrontendBaseUrl(null);

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);

        // Act
        adapter.sendReset(user, "reset-token-123");

        // Assert
        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertEquals(
                "no-reply@ecofy.local",
                message.getFrom()
        );
        assertNotNull(message.getText());
        assertTrue(
                message.getText().contains(
                        "http://localhost:3000/auth/reset-password"
                                + "?token=reset-token-123"
                )
        );
    }

    @Test
    @DisplayName("Deve rejeitar o envio de redefinição quando o usuário for nulo")
    void sendReset_usuarioNulo_deveLancarNullPointerException() {
        // Arrange
        AuthUser nullUser = null;

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.sendReset(
                        nullUser,
                        "reset-token-123"
                )
        );

        // Assert
        assertEquals("user must not be null", exception.getMessage());
        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("Deve rejeitar o envio de redefinição quando o token for nulo")
    void sendReset_tokenNulo_deveLancarNullPointerException() {
        // Arrange
        String nullToken = null;

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.sendReset(user, nullToken)
        );

        // Assert
        assertEquals(
                "resetToken must not be null",
                exception.getMessage()
        );
        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("Deve encapsular a falha ocorrida durante o envio da redefinição")
    void sendReset_falhaNoEnvio_deveLancarIllegalStateException() {
        // Arrange
        MailSendException mailException =
                new MailSendException("Falha no provedor");

        doThrow(mailException)
                .when(mailSender)
                .send(any(SimpleMailMessage.class));

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> adapter.sendReset(
                        user,
                        "reset-token-123"
                )
        );

        // Assert
        assertEquals(
                "Failed to send email of type: reset de senha",
                exception.getMessage()
        );
        assertSame(mailException, exception.getCause());
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    private AuthUser authUser() {
        return new AuthUser(
                new AuthUserId(USER_ID),
                new EmailAddress("user@ecofy.com"),
                new PasswordHash("hashed-password"),
                AuthUserStatus.ACTIVE,
                true,
                "Matheus",
                "Lemes",
                "pt-BR",
                Set.of(
                        new Role(
                                "ROLE_USER",
                                "Usuário",
                                Set.of()
                        )
                ),
                Set.of(),
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-02T10:00:00Z"),
                null,
                0
        );
    }
}
