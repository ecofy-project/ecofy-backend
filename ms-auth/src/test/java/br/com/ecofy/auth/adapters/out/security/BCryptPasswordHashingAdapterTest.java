package br.com.ecofy.auth.adapters.out.security;

import br.com.ecofy.auth.core.domain.valueobject.PasswordHash;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BCryptPasswordHashingAdapterTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void constructor_shouldRejectNullPasswordEncoder() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> new BCryptPasswordHashingAdapter(null));
        assertEquals("passwordEncoder must not be null", ex.getMessage());
    }

    @Test
    void hash_shouldRejectNullRawPassword() {
        BCryptPasswordHashingAdapter adapter = new BCryptPasswordHashingAdapter(passwordEncoder);

        NullPointerException ex = assertThrows(NullPointerException.class, () -> adapter.hash(null));
        assertEquals("rawPassword must not be null", ex.getMessage());

        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void hash_shouldRejectBlankRawPassword() {
        BCryptPasswordHashingAdapter adapter = new BCryptPasswordHashingAdapter(passwordEncoder);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> adapter.hash("   "));
        assertEquals("rawPassword must not be blank", ex.getMessage());

        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void hash_shouldEncodeAndWrapInPasswordHash() {
        BCryptPasswordHashingAdapter adapter = new BCryptPasswordHashingAdapter(passwordEncoder);

        when(passwordEncoder.encode("pass")).thenReturn("ENC");

        PasswordHash result = adapter.hash("pass");

        assertNotNull(result);
        assertEquals("ENC", result.value());

        verify(passwordEncoder).encode("pass");
        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    void matches_shouldRejectNullRawPassword() {
        BCryptPasswordHashingAdapter adapter = new BCryptPasswordHashingAdapter(passwordEncoder);

        NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> adapter.matches(null, new PasswordHash("h"))
        );
        assertEquals("rawPassword must not be null", ex.getMessage());

        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void matches_shouldRejectNullHash() {
        BCryptPasswordHashingAdapter adapter = new BCryptPasswordHashingAdapter(passwordEncoder);

        NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> adapter.matches("pass", null)
        );
        assertEquals("hash must not be null", ex.getMessage());

        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void matches_shouldDelegateToPasswordEncoder_andReturnTrue() {
        BCryptPasswordHashingAdapter adapter = new BCryptPasswordHashingAdapter(passwordEncoder);

        PasswordHash hash = new PasswordHash("ENC");
        when(passwordEncoder.matches("pass", "ENC")).thenReturn(true);

        boolean result = adapter.matches("pass", hash);

        assertTrue(result);

        verify(passwordEncoder).matches("pass", "ENC");
        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    void matches_shouldDelegateToPasswordEncoder_andReturnFalse() {
        BCryptPasswordHashingAdapter adapter = new BCryptPasswordHashingAdapter(passwordEncoder);

        PasswordHash hash = new PasswordHash("ENC");
        when(passwordEncoder.matches("pass", "ENC")).thenReturn(false);

        boolean result = adapter.matches("pass", hash);

        assertFalse(result);

        verify(passwordEncoder).matches("pass", "ENC");
        verifyNoMoreInteractions(passwordEncoder);
    }
}