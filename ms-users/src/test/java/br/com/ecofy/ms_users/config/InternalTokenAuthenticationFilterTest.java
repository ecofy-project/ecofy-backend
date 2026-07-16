package br.com.ecofy.ms_users.config;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class InternalTokenAuthenticationFilterTest {

    private static final String TOKEN = "super-secret-internal-token";

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private InternalTokenAuthenticationFilter filter(boolean enabled, String token) {
        UsersProperties props = new UsersProperties(
                null, null, null, new UsersProperties.Internal(enabled, token));
        return new InternalTokenAuthenticationFilter(props);
    }

    private MockHttpServletRequest internalRequest() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setServletPath("/internal/users/auth-1");
        req.setRequestURI("/users/internal/users/auth-1");
        req.setMethod("PUT");
        return req;
    }

    @Test
    void shouldReject401_whenTokenMissing() throws ServletException, IOException {
        var f = filter(true, TOKEN);
        var req = internalRequest();
        var res = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        f.doFilter(req, res, chain);

        assertEquals(401, res.getStatus());
        assertNull(chain.getRequest(), "chain não deve prosseguir sem token válido");
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldReject401_whenTokenInvalid() throws ServletException, IOException {
        var f = filter(true, TOKEN);
        var req = internalRequest();
        req.addHeader(InternalTokenAuthenticationFilter.INTERNAL_TOKEN_HEADER, "wrong-token");
        var res = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        f.doFilter(req, res, chain);

        assertEquals(401, res.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void shouldAuthenticateAndProceed_whenTokenValid() throws ServletException, IOException {
        var f = filter(true, TOKEN);
        var req = internalRequest();
        req.addHeader(InternalTokenAuthenticationFilter.INTERNAL_TOKEN_HEADER, TOKEN);
        var res = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        f.doFilter(req, res, chain);

        assertNotEquals(401, res.getStatus());
        assertNotNull(chain.getRequest(), "chain deve prosseguir com token válido");
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(InternalTokenAuthenticationFilter.INTERNAL_ROLE)));
    }

    @Test
    void shouldReject401_whenInternalDisabled() throws ServletException, IOException {
        var f = filter(false, TOKEN);
        var req = internalRequest();
        req.addHeader(InternalTokenAuthenticationFilter.INTERNAL_TOKEN_HEADER, TOKEN);
        var res = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        f.doFilter(req, res, chain);

        assertEquals(401, res.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void shouldNotFilterNonInternalPaths() throws ServletException, IOException {
        var f = filter(true, TOKEN);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setServletPath("/api/users/v1/preferences/x");
        req.setRequestURI("/users/api/users/v1/preferences/x");
        var res = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        f.doFilter(req, res, chain);

        // Fora de /internal, o filtro não interfere (deixa passar; autorização fica com o Security).
        assertNotEquals(401, res.getStatus());
        assertNotNull(chain.getRequest());
    }
}
