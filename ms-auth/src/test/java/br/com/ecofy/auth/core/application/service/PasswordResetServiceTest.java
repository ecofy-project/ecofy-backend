package br.com.ecofy.auth.core.application.service;

import br.com.ecofy.auth.core.application.exception.AuthErrorCode;
import br.com.ecofy.auth.core.application.exception.AuthException;
import br.com.ecofy.auth.core.domain.AuthUser;
import br.com.ecofy.auth.core.domain.event.PasswordResetRequestedEvent;
import br.com.ecofy.auth.core.domain.valueobject.AuthUserId;
import br.com.ecofy.auth.core.domain.valueobject.EmailAddress;
import br.com.ecofy.auth.core.domain.valueobject.PasswordHash;
import br.com.ecofy.auth.core.port.in.RequestPasswordResetUseCase;
import br.com.ecofy.auth.core.port.in.ResetPasswordUseCase;
import br.com.ecofy.auth.core.port.out.LoadAuthUserByEmailPort;
import br.com.ecofy.auth.core.port.out.PasswordHashingPort;
import br.com.ecofy.auth.core.port.out.PasswordResetTokenStorePort;
import br.com.ecofy.auth.core.port.out.PublishAuthEventPort;
import br.com.ecofy.auth.core.port.out.SaveAuthUserPort;
import br.com.ecofy.auth.core.port.out.SendResetPasswordEmailPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private LoadAuthUserByEmailPort loadAuthUserByEmailPort;

    @Mock
    private PasswordResetTokenStorePort passwordResetTokenStorePort;

    @Mock
    private SendResetPasswordEmailPort sendResetPasswordEmailPort;

    @Mock
    private SaveAuthUserPort saveAuthUserPort;

    @Mock
    private PasswordHashingPort passwordHashingPort;

    @Mock
    private PublishAuthEventPort publishAuthEventPort;

    private PasswordResetService service() {
        return new PasswordResetService(
                loadAuthUserByEmailPort,
                passwordResetTokenStorePort,
                sendResetPasswordEmailPort,
                saveAuthUserPort,
                passwordHashingPort,
                publishAuthEventPort
        );
    }

    @Test
    void constructor_shouldRejectNullPorts() {
        assertEquals(
                "loadAuthUserByEmailPort must not be null",
                assertThrows(NullPointerException.class, () -> new PasswordResetService(
                        null,
                        passwordResetTokenStorePort,
                        sendResetPasswordEmailPort,
                        saveAuthUserPort,
                        passwordHashingPort,
                        publishAuthEventPort
                )).getMessage()
        );

        assertEquals(
                "passwordResetTokenStorePort must not be null",
                assertThrows(NullPointerException.class, () -> new PasswordResetService(
                        loadAuthUserByEmailPort,
                        null,
                        sendResetPasswordEmailPort,
                        saveAuthUserPort,
                        passwordHashingPort,
                        publishAuthEventPort
                )).getMessage()
        );

        assertEquals(
                "sendResetPasswordEmailPort must not be null",
                assertThrows(NullPointerException.class, () -> new PasswordResetService(
                        loadAuthUserByEmailPort,
                        passwordResetTokenStorePort,
                        null,
                        saveAuthUserPort,
                        passwordHashingPort,
                        publishAuthEventPort
                )).getMessage()
        );

        assertEquals(
                "saveAuthUserPort must not be null",
                assertThrows(NullPointerException.class, () -> new PasswordResetService(
                        loadAuthUserByEmailPort,
                        passwordResetTokenStorePort,
                        sendResetPasswordEmailPort,
                        null,
                        passwordHashingPort,
                        publishAuthEventPort
                )).getMessage()
        );

        assertEquals(
                "passwordHashingPort must not be null",
                assertThrows(NullPointerException.class, () -> new PasswordResetService(
                        loadAuthUserByEmailPort,
                        passwordResetTokenStorePort,
                        sendResetPasswordEmailPort,
                        saveAuthUserPort,
                        null,
                        publishAuthEventPort
                )).getMessage()
        );

        assertEquals(
                "publishAuthEventPort must not be null",
                assertThrows(NullPointerException.class, () -> new PasswordResetService(
                        loadAuthUserByEmailPort,
                        passwordResetTokenStorePort,
                        sendResetPasswordEmailPort,
                        saveAuthUserPort,
                        passwordHashingPort,
                        null
                )).getMessage()
        );
    }

    @Test
    void requestReset_shouldRejectNullCommand() {
        PasswordResetService s = service();
        assertEquals("command must not be null", assertThrows(NullPointerException.class, () -> s.requestReset(null)).getMessage());
        verifyNoInteractions(loadAuthUserByEmailPort, passwordResetTokenStorePort, sendResetPasswordEmailPort, saveAuthUserPort, passwordHashingPort, publishAuthEventPort);
    }

    @Test
    void requestReset_shouldBeNoOp_whenUserMissing_toPreventEnumeration() {
        PasswordResetService s = service();

        RequestPasswordResetUseCase.RequestPasswordResetCommand cmd = mock(RequestPasswordResetUseCase.RequestPasswordResetCommand.class);
        when(cmd.email()).thenReturn("missing@ecofy.com");

        when(loadAuthUserByEmailPort.loadByEmail(any(EmailAddress.class))).thenReturn(Optional.empty());

        // Anti-enumeração: e-mail inexistente NÃO gera exceção nem revela ausência do usuário.
        assertDoesNotThrow(() -> s.requestReset(cmd));

        verify(loadAuthUserByEmailPort).loadByEmail(any(EmailAddress.class));
        verifyNoMoreInteractions(loadAuthUserByEmailPort);
        // Nenhum token é gerado/armazenado, nenhum e-mail enviado, nenhum evento publicado.
        verifyNoInteractions(passwordResetTokenStorePort, sendResetPasswordEmailPort, saveAuthUserPort, passwordHashingPort, publishAuthEventPort);
    }

    @Test
    void requestReset_shouldStoreSendAndPublish_whenUserExists() {
        PasswordResetService s = service();

        RequestPasswordResetUseCase.RequestPasswordResetCommand cmd = mock(RequestPasswordResetUseCase.RequestPasswordResetCommand.class);
        when(cmd.email()).thenReturn("u@ecofy.com");

        UUID uid = UUID.fromString("11111111-2222-3333-4444-555555555555");
        AuthUserId idVo = mock(AuthUserId.class);
        when(idVo.value()).thenReturn(uid);

        EmailAddress emailVo = mock(EmailAddress.class);
        when(emailVo.value()).thenReturn("u@ecofy.com");

        AuthUser user = mock(AuthUser.class, Answers.RETURNS_DEEP_STUBS);
        when(user.id()).thenReturn(idVo);
        when(user.email()).thenReturn(emailVo);

        when(loadAuthUserByEmailPort.loadByEmail(any(EmailAddress.class))).thenReturn(Optional.of(user));

        s.requestReset(cmd);

        ArgumentCaptor<String> tokenCaptor1 = ArgumentCaptor.forClass(String.class);
        verify(passwordResetTokenStorePort).store(same(user), tokenCaptor1.capture());
        String resetToken = tokenCaptor1.getValue();
        assertNotNull(resetToken);
        assertFalse(resetToken.isBlank());

        ArgumentCaptor<String> tokenCaptor2 = ArgumentCaptor.forClass(String.class);
        verify(sendResetPasswordEmailPort).sendReset(same(user), tokenCaptor2.capture());
        assertEquals(resetToken, tokenCaptor2.getValue());

        ArgumentCaptor<PasswordResetRequestedEvent> ev = ArgumentCaptor.forClass(PasswordResetRequestedEvent.class);
        verify(publishAuthEventPort).publish(ev.capture());
        assertSame(user, ev.getValue().user());
        assertEquals(resetToken, ev.getValue().resetToken());
    }

    @Test
    void resetPassword_shouldRejectNullCommand() {
        PasswordResetService s = service();
        assertEquals("command must not be null", assertThrows(NullPointerException.class, () -> s.resetPassword(null)).getMessage());
        verifyNoInteractions(loadAuthUserByEmailPort, passwordResetTokenStorePort, sendResetPasswordEmailPort, saveAuthUserPort, passwordHashingPort, publishAuthEventPort);
    }

    @Test
    void resetPassword_shouldThrowInvalidToken_whenConsumeReturnsEmpty() {
        PasswordResetService s = service();

        ResetPasswordUseCase.ResetPasswordCommand cmd = mock(ResetPasswordUseCase.ResetPasswordCommand.class);
        when(cmd.resetToken()).thenReturn("bad-token");

        when(passwordResetTokenStorePort.consume("bad-token")).thenReturn(Optional.empty());

        AuthException ex = assertThrows(AuthException.class, () -> s.resetPassword(cmd));
        assertEquals(AuthErrorCode.PASSWORD_RESET_TOKEN_INVALID, ex.getErrorCode());
        assertEquals("Invalid or expired reset token", ex.getMessage());

        verify(passwordResetTokenStorePort).consume("bad-token");
        verifyNoMoreInteractions(passwordResetTokenStorePort);
        verifyNoInteractions(saveAuthUserPort, passwordHashingPort);
    }

    @Test
    void resetPassword_shouldHashChangeAndSave_whenTokenValid() {
        PasswordResetService s = service();

        ResetPasswordUseCase.ResetPasswordCommand cmd = mock(ResetPasswordUseCase.ResetPasswordCommand.class);
        when(cmd.resetToken()).thenReturn("good-token");
        when(cmd.newPassword()).thenReturn("new-pass");

        UUID uid = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        AuthUserId idVo = mock(AuthUserId.class);
        when(idVo.value()).thenReturn(uid);

        AuthUser user = mock(AuthUser.class, Answers.RETURNS_DEEP_STUBS);
        when(user.id()).thenReturn(idVo);

        when(passwordResetTokenStorePort.consume("good-token")).thenReturn(Optional.of(user));

        PasswordHash newHash = mock(PasswordHash.class);
        when(passwordHashingPort.hash("new-pass")).thenReturn(newHash);

        when(saveAuthUserPort.save(user)).thenReturn(user);

        s.resetPassword(cmd);

        verify(passwordResetTokenStorePort).consume("good-token");
        verify(passwordHashingPort).hash("new-pass");
        verify(user).changePassword(newHash);
        verify(saveAuthUserPort).save(user);
    }

    @Test
    void maskToken_shouldCoverAllBranches_inOneTest() throws Exception {
        PasswordResetService s = service();

        Method m = PasswordResetService.class.getDeclaredMethod("maskToken", String.class);
        m.setAccessible(true);

        assertEquals("***", (String) m.invoke(s, (String) null));
        assertEquals("***", (String) m.invoke(s, "   "));
        assertEquals("***", (String) m.invoke(s, "1234567890"));
        assertEquals("1234567890...", (String) m.invoke(s, "12345678901"));
    }
}