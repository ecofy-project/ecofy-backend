package br.com.ecofy.auth.core.application.service;

import br.com.ecofy.auth.core.application.exception.AuthErrorCode;
import br.com.ecofy.auth.core.application.exception.AuthException;
import br.com.ecofy.auth.core.domain.AuthUser;
import br.com.ecofy.auth.core.domain.enums.AuthUserStatus;
import br.com.ecofy.auth.core.domain.valueobject.AuthUserId;
import br.com.ecofy.auth.core.domain.valueobject.EmailAddress;
import br.com.ecofy.auth.core.port.out.CurrentUserProviderPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrentUserProfileServiceTest {

    @Mock
    private CurrentUserProviderPort currentUserProviderPort;

    @Test
    void constructor_shouldRejectNullPort() {
        NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> new CurrentUserProfileService(null)
        );
        assertEquals("currentUserProviderPort must not be null", ex.getMessage());
    }

    @Test
    void getCurrentUser_shouldReturnUser_whenProviderSucceeds() {
        CurrentUserProfileService service = new CurrentUserProfileService(currentUserProviderPort);

        UUID uid = UUID.fromString("11111111-2222-3333-4444-555555555555");

        AuthUser user = mock(AuthUser.class, Answers.RETURNS_DEEP_STUBS);
        when(user.id().value()).thenReturn(uid);

        EmailAddress email = mock(EmailAddress.class);
        when(email.value()).thenReturn("u@ecofy.com");
        when(user.email()).thenReturn(email);

        when(user.status()).thenReturn(AuthUserStatus.ACTIVE);

        when(currentUserProviderPort.getCurrentUserOrThrow()).thenReturn(user);

        AuthUser result = service.getCurrentUser();

        assertSame(user, result);

        verify(currentUserProviderPort).getCurrentUserOrThrow();
        verifyNoMoreInteractions(currentUserProviderPort);
    }

    @Test
    void getCurrentUser_shouldWrapRuntimeExceptionIntoAuthException() {
        CurrentUserProfileService service = new CurrentUserProfileService(currentUserProviderPort);

        RuntimeException cause = new RuntimeException("boom");
        when(currentUserProviderPort.getCurrentUserOrThrow()).thenThrow(cause);

        AuthException ex = assertThrows(AuthException.class, service::getCurrentUser);

        assertEquals(AuthErrorCode.CURRENT_USER_NOT_AUTHENTICATED, ex.getErrorCode());
        assertEquals("No authenticated user found", ex.getMessage());
        assertSame(cause, ex.getCause());

        verify(currentUserProviderPort).getCurrentUserOrThrow();
        verifyNoMoreInteractions(currentUserProviderPort);
    }
}