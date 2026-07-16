package br.com.ecofy.auth.core.application.service;

import br.com.ecofy.auth.core.application.exception.AuthErrorCode;
import br.com.ecofy.auth.core.application.exception.AuthException;
import br.com.ecofy.auth.core.domain.AuthUser;
import br.com.ecofy.auth.core.domain.Role;
import br.com.ecofy.auth.core.domain.event.UserRegisteredEvent;
import br.com.ecofy.auth.core.domain.valueobject.EmailAddress;
import br.com.ecofy.auth.core.domain.valueobject.PasswordHash;
import br.com.ecofy.auth.core.port.in.RegisterUserUseCase;
import br.com.ecofy.auth.core.port.out.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterUserServiceTest {

    @Mock
    private SaveAuthUserPort saveAuthUserPort;

    @Mock
    private LoadAuthUserByEmailPort loadAuthUserByEmailPort;

    @Mock
    private PasswordHashingPort passwordHashingPort;

    @Mock
    private SendVerificationEmailPort sendVerificationEmailPort;

    @Mock
    private VerificationTokenStorePort verificationTokenStorePort;

    @Mock
    private PublishAuthEventPort publishAuthEventPort;

    @Mock
    private SyncUserToUsersMsPort syncUserToUsersMsPort;

    private RegisterUserService service() {
        return new RegisterUserService(
                saveAuthUserPort,
                loadAuthUserByEmailPort,
                passwordHashingPort,
                sendVerificationEmailPort,
                verificationTokenStorePort,
                publishAuthEventPort,
                syncUserToUsersMsPort
        );
    }

    @Test
    void constructor_shouldRejectNullPorts() {
        assertEquals(
                "saveAuthUserPort must not be null",
                assertThrows(NullPointerException.class, () -> new RegisterUserService(
                        null,
                        loadAuthUserByEmailPort,
                        passwordHashingPort,
                        sendVerificationEmailPort,
                        verificationTokenStorePort,
                        publishAuthEventPort,
                        syncUserToUsersMsPort
                )).getMessage()
        );

        assertEquals(
                "loadAuthUserByEmailPort must not be null",
                assertThrows(NullPointerException.class, () -> new RegisterUserService(
                        saveAuthUserPort,
                        null,
                        passwordHashingPort,
                        sendVerificationEmailPort,
                        verificationTokenStorePort,
                        publishAuthEventPort,
                        syncUserToUsersMsPort
                )).getMessage()
        );

        assertEquals(
                "passwordHashingPort must not be null",
                assertThrows(NullPointerException.class, () -> new RegisterUserService(
                        saveAuthUserPort,
                        loadAuthUserByEmailPort,
                        null,
                        sendVerificationEmailPort,
                        verificationTokenStorePort,
                        publishAuthEventPort,
                        syncUserToUsersMsPort
                )).getMessage()
        );

        assertEquals(
                "sendVerificationEmailPort must not be null",
                assertThrows(NullPointerException.class, () -> new RegisterUserService(
                        saveAuthUserPort,
                        loadAuthUserByEmailPort,
                        passwordHashingPort,
                        null,
                        verificationTokenStorePort,
                        publishAuthEventPort,
                        syncUserToUsersMsPort
                )).getMessage()
        );

        assertEquals(
                "verificationTokenStorePort must not be null",
                assertThrows(NullPointerException.class, () -> new RegisterUserService(
                        saveAuthUserPort,
                        loadAuthUserByEmailPort,
                        passwordHashingPort,
                        sendVerificationEmailPort,
                        null,
                        publishAuthEventPort,
                        syncUserToUsersMsPort
                )).getMessage()
        );

        assertEquals(
                "publishAuthEventPort must not be null",
                assertThrows(NullPointerException.class, () -> new RegisterUserService(
                        saveAuthUserPort,
                        loadAuthUserByEmailPort,
                        passwordHashingPort,
                        sendVerificationEmailPort,
                        verificationTokenStorePort,
                        null,
                        syncUserToUsersMsPort
                )).getMessage()
        );

        assertEquals(
                "syncUserToUsersMsPort must not be null",
                assertThrows(NullPointerException.class, () -> new RegisterUserService(
                        saveAuthUserPort,
                        loadAuthUserByEmailPort,
                        passwordHashingPort,
                        sendVerificationEmailPort,
                        verificationTokenStorePort,
                        publishAuthEventPort,
                        null
                )).getMessage()
        );
    }

    @Test
    void register_shouldRejectNullCommand() {
        RegisterUserService s = service();
        assertEquals("command must not be null", assertThrows(NullPointerException.class, () -> s.register(null)).getMessage());
        verifyNoInteractions(saveAuthUserPort, loadAuthUserByEmailPort, passwordHashingPort, sendVerificationEmailPort, verificationTokenStorePort, publishAuthEventPort, syncUserToUsersMsPort);
    }

    @Test
    void register_shouldThrowEmailAlreadyRegistered_whenExistingUserFound() {
        RegisterUserService s = service();

        RegisterUserUseCase.RegisterUserCommand cmd = mock(RegisterUserUseCase.RegisterUserCommand.class);
        when(cmd.email()).thenReturn("u@ecofy.com");
        when(cmd.locale()).thenReturn(null);
        when(cmd.roles()).thenReturn(null);
        when(cmd.firstName()).thenReturn("U");
        when(cmd.lastName()).thenReturn("E");

        AuthUser existing = mock(AuthUser.class, Answers.RETURNS_DEEP_STUBS);
        when(existing.id().value()).thenReturn(UUID.fromString("11111111-2222-3333-4444-555555555555"));

        when(loadAuthUserByEmailPort.loadByEmail(any(EmailAddress.class))).thenReturn(java.util.Optional.of(existing));

        AuthException ex = assertThrows(AuthException.class, () -> s.register(cmd));
        assertEquals(AuthErrorCode.EMAIL_ALREADY_REGISTERED, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Email already registered: u@ecofy.com"));

        verify(loadAuthUserByEmailPort).loadByEmail(any(EmailAddress.class));
        verifyNoInteractions(passwordHashingPort, saveAuthUserPort, syncUserToUsersMsPort, verificationTokenStorePort, sendVerificationEmailPort, publishAuthEventPort);
    }

    @Test
    void register_shouldAutoConfirm_whenEnabled_useDefaultLocaleAndDefaultRole_andNotSendVerification() {
        RegisterUserService s = service();

        RegisterUserUseCase.RegisterUserCommand cmd = mock(RegisterUserUseCase.RegisterUserCommand.class);
        when(cmd.email()).thenReturn("u@ecofy.com");
        when(cmd.locale()).thenReturn(null);
        when(cmd.roles()).thenReturn(null);
        when(cmd.firstName()).thenReturn("User");
        when(cmd.lastName()).thenReturn("One");
        when(cmd.rawPassword()).thenReturn("pass");
        when(cmd.autoConfirmEmail()).thenReturn(true);

        when(loadAuthUserByEmailPort.loadByEmail(any(EmailAddress.class))).thenReturn(java.util.Optional.empty());

        PasswordHash ph = mock(PasswordHash.class);
        when(passwordHashingPort.hash("pass")).thenReturn(ph);

        AuthUser newUser = mock(AuthUser.class);
        AuthUser persisted = mock(AuthUser.class, Answers.RETURNS_DEEP_STUBS);
        UUID id = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        when(persisted.id().value()).thenReturn(id);

        try (MockedStatic<AuthUser> mocked = mockStatic(AuthUser.class)) {
            mocked.when(() -> AuthUser.newPendingUser(
                            any(EmailAddress.class),
                            same(ph),
                            eq("User"),
                            eq("One"),
                            eq("pt-BR"),
                            anySet()
                    ))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        Set<Role> roles = (Set<Role>) inv.getArgument(5);
                        assertEquals(1, roles.size());
                        assertTrue(roles.stream().anyMatch(r -> "ROLE_USER".equals(r.name())));
                        return newUser;
                    });

            when(saveAuthUserPort.save(newUser)).thenReturn(persisted);

            AuthUser result = s.register(cmd);

            assertSame(persisted, result);
        }

        verify(newUser).confirmEmail();
        verify(saveAuthUserPort).save(newUser);
        verify(syncUserToUsersMsPort).upsertUser(persisted);
        verify(publishAuthEventPort).publish(any(UserRegisteredEvent.class));
        verifyNoInteractions(verificationTokenStorePort, sendVerificationEmailPort);
    }

    @Test
    void register_shouldSwallowSyncFailure_andSendVerification_whenAutoConfirmFalse_andFilterRolesAndUseProvidedLocale() {
        RegisterUserService s = service();

        RegisterUserUseCase.RegisterUserCommand cmd = mock(RegisterUserUseCase.RegisterUserCommand.class);
        when(cmd.email()).thenReturn("u@ecofy.com");
        when(cmd.locale()).thenReturn("en-US");
        when(cmd.roles()).thenReturn(java.util.Arrays.asList(" ", null, "AUTH_ADMIN", " AUTH_USER "));
        when(cmd.firstName()).thenReturn("User");
        when(cmd.lastName()).thenReturn("Two");
        when(cmd.rawPassword()).thenReturn("pass");
        when(cmd.autoConfirmEmail()).thenReturn(false);

        when(loadAuthUserByEmailPort.loadByEmail(any(EmailAddress.class))).thenReturn(java.util.Optional.empty());

        PasswordHash ph = mock(PasswordHash.class);
        when(passwordHashingPort.hash("pass")).thenReturn(ph);

        AuthUser newUser = mock(AuthUser.class);
        AuthUser persisted = mock(AuthUser.class, Answers.RETURNS_DEEP_STUBS);
        UUID id = UUID.fromString("22222222-3333-4444-5555-666666666666");
        when(persisted.id().value()).thenReturn(id);
        when(persisted.email().value()).thenReturn("u@ecofy.com");

        try (MockedStatic<AuthUser> mocked = mockStatic(AuthUser.class)) {
            mocked.when(() -> AuthUser.newPendingUser(
                            any(EmailAddress.class),
                            same(ph),
                            eq("User"),
                            eq("Two"),
                            eq("en-US"),
                            anySet()
                    ))
                    .thenAnswer(inv -> {
                        @SuppressWarnings("unchecked")
                        Set<Role> roles = (Set<Role>) inv.getArgument(5);
                        assertEquals(2, roles.size());
                        assertTrue(roles.stream().anyMatch(r -> "AUTH_ADMIN".equals(r.name())));
                        assertTrue(roles.stream().anyMatch(r -> "AUTH_USER".equals(r.name())));
                        return newUser;
                    });

            when(saveAuthUserPort.save(newUser)).thenReturn(persisted);

            doThrow(new RuntimeException("ms-users down")).when(syncUserToUsersMsPort).upsertUser(persisted);

            s.register(cmd);
        }

        verify(saveAuthUserPort).save(newUser);
        verify(syncUserToUsersMsPort).upsertUser(persisted);

        verify(verificationTokenStorePort).store(eq(persisted), anyString());
        verify(sendVerificationEmailPort).send(eq(persisted), anyString());

        verify(publishAuthEventPort).publish(any(UserRegisteredEvent.class));
    }

    @Test
    void maskToken_shouldCoverAllBranches_inOneTest() throws Exception {
        RegisterUserService s = service();

        Method m = RegisterUserService.class.getDeclaredMethod("maskToken", String.class);
        m.setAccessible(true);

        assertEquals("***", (String) m.invoke(s, (String) null));
        assertEquals("***", (String) m.invoke(s, "   "));
        assertEquals("***", (String) m.invoke(s, "1234567890"));
        assertEquals("1234567890...", (String) m.invoke(s, "12345678901"));
    }
}