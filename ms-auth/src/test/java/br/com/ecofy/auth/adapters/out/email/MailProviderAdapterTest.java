package br.com.ecofy.auth.adapters.out.email;

import br.com.ecofy.auth.config.MailConfig;
import br.com.ecofy.auth.core.domain.AuthUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailProviderAdapterTest {

    @Mock private JavaMailSender mailSender;
    @Mock private MailConfig.EcofyMailProperties props;

    @Captor private ArgumentCaptor<SimpleMailMessage> messageCaptor;

    @BeforeEach
    void resetMocks() {
        clearInvocations(mailSender, props);
    }

    // constructor coverage

    @Test
    @DisplayName("constructor: mailSender null -> NPE com mensagem")
    void constructor_mailSenderNull_throwsNpe() {
        assertThatThrownBy(() -> new MailProviderAdapter(null, props))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("mailSender must not be null");
    }

    @Test
    @DisplayName("constructor: props null -> NPE com mensagem")
    void constructor_propsNull_throwsNpe() {
        assertThatThrownBy(() -> new MailProviderAdapter(mailSender, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("props must not be null");
    }

    // validation coverage

    @Test
    @DisplayName("send: user null -> NPE")
    void send_userNull_throwsNpe() {
        MailProviderAdapter a = adapterNoStub();

        assertThatThrownBy(() -> a.send(null, "token"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("user must not be null");

        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("send: token null -> NPE")
    void send_tokenNull_throwsNpe() {
        // IMPORTANTE: não stubbar props nem AuthUser aqui (Mockito strict).
        MailProviderAdapter a = adapterNoStub();
        AuthUser u = mock(AuthUser.class); // sem stubs; não serão usados antes do NPE

        assertThatThrownBy(() -> a.send(u, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("verificationToken must not be null");

        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("sendReset: user null -> NPE")
    void sendReset_userNull_throwsNpe() {
        MailProviderAdapter a = adapterNoStub();

        assertThatThrownBy(() -> a.sendReset(null, "token"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("user must not be null");

        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("sendReset: token null -> NPE")
    void sendReset_tokenNull_throwsNpe() {
        MailProviderAdapter a = adapterNoStub();
        AuthUser u = mock(AuthUser.class); // sem stubs; não serão usados antes do NPE

        assertThatThrownBy(() -> a.sendReset(u, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("resetToken must not be null");

        verifyNoInteractions(mailSender);
    }

    // success paths + branch coverage (fallbacks)

    @Test
    @DisplayName("send: sucesso com from custom e baseUrl custom (subject/to/from/text/link)")
    void send_success_customFrom_customBaseUrl() {
        MailProviderAdapter a = adapter("no-reply@ecofy.com", "https://ecofy.app");
        AuthUser u = user("3f5d1c0a-1b2c-4d5e-9f00-111122223333", "user@ecofy.com", "Carlos");

        a.send(u, "vt-123");

        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage msg = messageCaptor.getValue();

        assertThat(msg.getFrom()).isEqualTo("no-reply@ecofy.com");
        assertThat(msg.getTo()).containsExactly("user@ecofy.com");
        assertThat(msg.getSubject()).isEqualTo("Confirme seu e-mail – EcoFy");

        String text = msg.getText();
        assertThat(text).contains("Olá Carlos");
        assertThat(text).contains("https://ecofy.app/auth/confirm-email?token=vt-123");
        assertThat(text).contains("Se você não criou esta conta, ignore esta mensagem.");
    }

    @Test
    @DisplayName("sendReset: sucesso com fallbacks (from default e baseUrl default)")
    void sendReset_success_fallbackFrom_fallbackBaseUrl() {
        MailProviderAdapter a = adapter(null, null);
        AuthUser u = user("8d8c7b6a-5e4d-3c2b-1a00-abcdefabcdef", "reset@ecofy.com", "Marina");

        a.sendReset(u, "rt-456");

        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage msg = messageCaptor.getValue();

        assertThat(msg.getFrom()).isEqualTo("no-reply@ecofy.local");
        assertThat(msg.getTo()).containsExactly("reset@ecofy.com");
        assertThat(msg.getSubject()).isEqualTo("Redefinição de senha – EcoFy");

        String text = msg.getText();
        assertThat(text).contains("Olá Marina");
        assertThat(text).contains("http://localhost:3000/auth/reset-password?token=rt-456");
        assertThat(text).contains("Se você não solicitou esta redefinição, apenas ignore este e-mail.");
    }

    @Test
    @DisplayName("send: token com caracteres especiais deve ser incluído na query string (sem encoding)")
    void send_tokenEncoding_isApplied() {
        MailProviderAdapter a = adapter(null, "http://localhost:3000");
        AuthUser u = user("11111111-2222-3333-4444-555555555555", "enc@ecofy.com", "João");

        String token = "a b&c";
        a.send(u, token);

        verify(mailSender, times(1)).send(messageCaptor.capture());
        String text = messageCaptor.getValue().getText();

        assertThat(text).contains("http://localhost:3000/auth/confirm-email?token=");
        assertThat(text).contains("token=" + token);

        // opcional: garante que NÃO houve encoding (documenta o comportamento atual)
        assertThat(text).doesNotContain("token=a%20b%26c");
    }


    // failure paths (catch MailException branch)

    @Test
    @DisplayName("send: MailException -> IllegalStateException com mensagem e causa")
    void send_mailException_rethrowsIllegalState() {
        MailProviderAdapter a = adapter("no-reply@ecofy.com", "https://ecofy.app");
        AuthUser u = user("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", "fail@ecofy.com", "Carlos");

        doThrow(new MailSendException("smtp down"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> a.send(u, "vt-err"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Falha ao enviar e-mail de verificação")
                .hasCauseInstanceOf(MailSendException.class);

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("sendReset: MailException -> IllegalStateException com mensagem e causa")
    void sendReset_mailException_rethrowsIllegalState() {
        MailProviderAdapter a = adapter("no-reply@ecofy.com", "https://ecofy.app");
        AuthUser u = user("99999999-8888-7777-6666-555555555555", "failreset@ecofy.com", "Marina");

        doThrow(new MailSendException("smtp down"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> a.sendReset(u, "rt-err"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Falha ao enviar e-mail de reset de senha")
                .hasCauseInstanceOf(MailSendException.class);

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    // helpers (apenas métodos)

    private MailProviderAdapter adapterNoStub() {
        return new MailProviderAdapter(mailSender, mock(MailConfig.EcofyMailProperties.class));
    }

    private MailProviderAdapter adapter(String from, String frontendBaseUrl) {
        when(props.getFrom()).thenReturn(from);
        when(props.getFrontendBaseUrl()).thenReturn(frontendBaseUrl);
        return new MailProviderAdapter(mailSender, props);
    }

    private AuthUser user(String uuid, String email, String fullName) {
        AuthUser u = mock(AuthUser.class, RETURNS_DEEP_STUBS);
        when(u.id().value()).thenReturn(UUID.fromString(uuid));
        when(u.email().value()).thenReturn(email);
        when(u.fullName()).thenReturn(fullName);
        return u;
    }

}
