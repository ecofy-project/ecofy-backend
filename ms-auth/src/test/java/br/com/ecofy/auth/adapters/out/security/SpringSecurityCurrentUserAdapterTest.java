package br.com.ecofy.auth.adapters.out.security;

import br.com.ecofy.auth.adapters.out.persistence.AuthUserJpaAdapter;
import br.com.ecofy.auth.core.domain.AuthUser;
import br.com.ecofy.auth.core.domain.valueobject.AuthUserId;
import br.com.ecofy.auth.core.domain.valueobject.EmailAddress;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpringSecurityCurrentUserAdapterTest {

    @Mock
    private AuthUserJpaAdapter authUserJpaAdapter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void constructor_shouldRejectNullAuthUserJpaAdapter() {
        NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> new SpringSecurityCurrentUserAdapter(null)
        );
        assertEquals("authUserJpaAdapter must not be null", ex.getMessage());
    }

    @Test
    void getCurrentUserOrThrow_shouldThrow_whenAuthenticationIsNull() {
        SpringSecurityCurrentUserAdapter adapter = new SpringSecurityCurrentUserAdapter(authUserJpaAdapter);

        SecurityContextHolder.clearContext();

        IllegalStateException ex = assertThrows(IllegalStateException.class, adapter::getCurrentUserOrThrow);
        assertEquals("Usuário não autenticado", ex.getMessage());

        verifyNoInteractions(authUserJpaAdapter);
    }

    @Test
    void getCurrentUserOrThrow_shouldThrow_whenPrincipalIsNotJwt() {
        SpringSecurityCurrentUserAdapter adapter = new SpringSecurityCurrentUserAdapter(authUserJpaAdapter);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("not-a-jwt");
        SecurityContextHolder.getContext().setAuthentication(auth);

        IllegalStateException ex = assertThrows(IllegalStateException.class, adapter::getCurrentUserOrThrow);
        assertEquals("JWT inválido ou ausente", ex.getMessage());

        verifyNoInteractions(authUserJpaAdapter);
    }

    @Test
    void getCurrentUserOrThrow_shouldThrow_whenJwtSubjectIsNotUuid() {
        SpringSecurityCurrentUserAdapter adapter = new SpringSecurityCurrentUserAdapter(authUserJpaAdapter);

        Jwt jwt = Jwt.withTokenValue("t")
                .headers(h -> h.putAll(Map.of("alg", "none")))
                .claims(c -> c.put("sub", "not-uuid"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(jwt);
        SecurityContextHolder.getContext().setAuthentication(auth);

        IllegalStateException ex = assertThrows(IllegalStateException.class, adapter::getCurrentUserOrThrow);
        assertEquals("Identificador de usuário inválido", ex.getMessage());

        verifyNoInteractions(authUserJpaAdapter);
    }

    @Test
    void getCurrentUserOrThrow_shouldThrow_whenUserNotFound() {
        SpringSecurityCurrentUserAdapter adapter = new SpringSecurityCurrentUserAdapter(authUserJpaAdapter);

        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Jwt jwt = jwtWithSub(userId.toString(), "jti-1");

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(jwt);
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(authUserJpaAdapter.loadById(any(AuthUserId.class))).thenReturn(Optional.empty());

        IllegalStateException ex = assertThrows(IllegalStateException.class, adapter::getCurrentUserOrThrow);
        assertEquals("Usuário autenticado não encontrado", ex.getMessage());

        ArgumentCaptor<AuthUserId> captor = ArgumentCaptor.forClass(AuthUserId.class);
        verify(authUserJpaAdapter).loadById(captor.capture());
        assertEquals(userId, captor.getValue().value());
        verifyNoMoreInteractions(authUserJpaAdapter);
    }

    @Test
    void getCurrentUserOrThrow_shouldReturnUser_whenJwtIsValidAndUserExists() {
        SpringSecurityCurrentUserAdapter adapter = new SpringSecurityCurrentUserAdapter(authUserJpaAdapter);

        UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Jwt jwt = jwtWithSub(userId.toString(), "jti-2");

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(jwt);
        SecurityContextHolder.getContext().setAuthentication(auth);

        AuthUser user = mock(AuthUser.class);

        AuthUserId idVo = mock(AuthUserId.class);
        when(idVo.value()).thenReturn(userId);
        when(user.id()).thenReturn(idVo);

        EmailAddress email = mock(EmailAddress.class);
        when(email.value()).thenReturn("u@ecofy.com");
        when(user.email()).thenReturn(email);

        when(authUserJpaAdapter.loadById(any(AuthUserId.class))).thenReturn(Optional.of(user));

        AuthUser result = adapter.getCurrentUserOrThrow();

        assertSame(user, result);

        ArgumentCaptor<AuthUserId> captor = ArgumentCaptor.forClass(AuthUserId.class);
        verify(authUserJpaAdapter).loadById(captor.capture());
        assertEquals(userId, captor.getValue().value());
        verifyNoMoreInteractions(authUserJpaAdapter);
    }

    private static Jwt jwtWithSub(String sub, String jti) {
        return Jwt.withTokenValue("t")
                .headers(h -> h.putAll(Map.of("alg", "none")))
                .claims(c -> {
                    c.put("sub", sub);
                    c.put("jti", jti);
                })
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
    }
}