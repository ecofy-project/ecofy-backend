package br.com.ecofy.auth.core.application.service;

import br.com.ecofy.auth.core.application.exception.AuthErrorCode;
import br.com.ecofy.auth.core.application.exception.AuthException;
import br.com.ecofy.auth.core.domain.AuthUser;
import br.com.ecofy.auth.core.domain.event.UserEmailConfirmedEvent;
import br.com.ecofy.auth.core.domain.valueobject.AuthUserId;
import br.com.ecofy.auth.core.port.in.ConfirmEmailUseCase;
import br.com.ecofy.auth.core.port.out.PublishAuthEventPort;
import br.com.ecofy.auth.core.port.out.SaveAuthUserPort;
import br.com.ecofy.auth.core.port.out.VerificationTokenStorePort;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailConfirmationServiceTest {

    @Mock
    private VerificationTokenStorePort verificationTokenStorePort;

    @Mock
    private SaveAuthUserPort saveAuthUserPort;

    @Mock
    private PublishAuthEventPort publishAuthEventPort;

    @Test
    void constructor_shouldRejectNullPorts() {
        assertEquals(
                "verificationTokenStorePort must not be null",
                assertThrows(NullPointerException.class, () -> new EmailConfirmationService(null, saveAuthUserPort, publishAuthEventPort)).getMessage()
        );
        assertEquals(
                "saveAuthUserPort must not be null",
                assertThrows(NullPointerException.class, () -> new EmailConfirmationService(verificationTokenStorePort, null, publishAuthEventPort)).getMessage()
        );
        assertEquals(
                "publishAuthEventPort must not be null",
                assertThrows(NullPointerException.class, () -> new EmailConfirmationService(verificationTokenStorePort, saveAuthUserPort, null)).getMessage()
        );
    }

    @Test
    void confirm_shouldRejectNullCommand() {
        EmailConfirmationService service = new EmailConfirmationService(verificationTokenStorePort, saveAuthUserPort, publishAuthEventPort);

        assertEquals(
                "command must not be null",
                assertThrows(NullPointerException.class, () -> service.confirm(null)).getMessage()
        );

        verifyNoInteractions(verificationTokenStorePort, saveAuthUserPort, publishAuthEventPort);
    }

    @Test
    void confirm_shouldRejectNullVerificationToken() {
        EmailConfirmationService service = new EmailConfirmationService(verificationTokenStorePort, saveAuthUserPort, publishAuthEventPort);

        ConfirmEmailUseCase.ConfirmEmailCommand cmd = mock(ConfirmEmailUseCase.ConfirmEmailCommand.class);
        when(cmd.verificationToken()).thenReturn(null);

        assertEquals(
                "verificationToken must not be null",
                assertThrows(NullPointerException.class, () -> service.confirm(cmd)).getMessage()
        );

        verify(cmd).verificationToken();
        verifyNoInteractions(verificationTokenStorePort, saveAuthUserPort, publishAuthEventPort);
    }

    @Test
    void confirm_shouldThrowAuthException_whenTokenInvalidOrExpired() {
        EmailConfirmationService service = new EmailConfirmationService(verificationTokenStorePort, saveAuthUserPort, publishAuthEventPort);

        ConfirmEmailUseCase.ConfirmEmailCommand cmd = mock(ConfirmEmailUseCase.ConfirmEmailCommand.class);
        when(cmd.verificationToken()).thenReturn("invalid-token");

        when(verificationTokenStorePort.consume("invalid-token")).thenReturn(Optional.empty());

        AuthException ex = assertThrows(AuthException.class, () -> service.confirm(cmd));
        assertEquals(AuthErrorCode.EMAIL_CONFIRMATION_TOKEN_INVALID, ex.getErrorCode());
        assertEquals("Invalid or expired verification token", ex.getMessage());

        verify(verificationTokenStorePort).consume("invalid-token");
        verifyNoInteractions(saveAuthUserPort, publishAuthEventPort);
    }

    @Test
    void confirm_shouldConfirmPersistPublish_andReturnPersistedUser() {
        EmailConfirmationService service = new EmailConfirmationService(verificationTokenStorePort, saveAuthUserPort, publishAuthEventPort);

        ConfirmEmailUseCase.ConfirmEmailCommand cmd = mock(ConfirmEmailUseCase.ConfirmEmailCommand.class);
        when(cmd.verificationToken()).thenReturn("token-12345678901");

        UUID id = UUID.fromString("11111111-2222-3333-4444-555555555555");
        AuthUserId idVo = mock(AuthUserId.class);
        when(idVo.value()).thenReturn(id);

        AuthUser user = mock(AuthUser.class, Answers.RETURNS_DEEP_STUBS);
        when(user.id()).thenReturn(idVo);
        when(user.isEmailVerified()).thenReturn(false);

        when(verificationTokenStorePort.consume("token-12345678901")).thenReturn(Optional.of(user));

        AuthUser persisted = mock(AuthUser.class, Answers.RETURNS_DEEP_STUBS);
        when(persisted.id().value()).thenReturn(id);
        when(persisted.isEmailVerified()).thenReturn(true);

        when(saveAuthUserPort.save(user)).thenReturn(persisted);

        AuthUser result = service.confirm(cmd);

        assertSame(persisted, result);

        verify(user).confirmEmail();
        verify(saveAuthUserPort).save(user);

        ArgumentCaptor<UserEmailConfirmedEvent> ev = ArgumentCaptor.forClass(UserEmailConfirmedEvent.class);
        verify(publishAuthEventPort).publish(ev.capture());
        assertSame(persisted, ev.getValue().user());

        verify(verificationTokenStorePort).consume("token-12345678901");
    }

    @Test
    void maskToken_shouldCoverAllBranches_inOneTest() throws Exception {
        EmailConfirmationService service = new EmailConfirmationService(verificationTokenStorePort, saveAuthUserPort, publishAuthEventPort);

        Method m = EmailConfirmationService.class.getDeclaredMethod("maskToken", String.class);
        m.setAccessible(true);

        assertEquals("***", (String) m.invoke(service, (String) null));
        assertEquals("***", (String) m.invoke(service, "   "));
        assertEquals("***", (String) m.invoke(service, "1234567890"));
        assertEquals("1234567890...", (String) m.invoke(service, "12345678901"));
    }
}