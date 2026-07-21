package br.com.ecofy.ms_users.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// Autentica as rotas internas pelo token de serviço, mantendo-as protegidas mesmo com permit-all ligado.
@Component
@Slf4j
public class InternalTokenAuthenticationFilter extends OncePerRequestFilter {

    public static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";
    public static final String INTERNAL_ROLE = "ROLE_INTERNAL";
    private static final String INTERNAL_PATH_PREFIX = "/internal";

    private final boolean internalEnabled;
    private final String configuredToken;

    public InternalTokenAuthenticationFilter(UsersProperties usersProperties) {
        UsersProperties.Internal internal = usersProperties != null ? usersProperties.internal() : null;
        this.internalEnabled = internal != null && internal.enabled();
        this.configuredToken = internal != null ? internal.token() : null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path == null || !path.startsWith(INTERNAL_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String provided = request.getHeader(INTERNAL_TOKEN_HEADER);

        if (!internalEnabled || configuredToken == null || configuredToken.isBlank()) {
            // Endpoint interno desabilitado ou mal configurado -> nega por segurança.
            log.warn("[InternalTokenAuthenticationFilter] -> internal endpoint disabled/misconfigured; rejecting request");
            unauthorized(response, request, "Internal endpoint is not available");
            return;
        }

        if (provided == null || provided.isBlank() || !constantTimeEquals(provided.trim(), configuredToken.trim())) {
            log.warn(
                    "[InternalTokenAuthenticationFilter] -> invalid/missing {} for internal request path={}",
                    INTERNAL_TOKEN_HEADER, request.getServletPath()
            );
            unauthorized(response, request, "Invalid or missing internal token");
            return;
        }

        var authentication = new UsernamePasswordAuthenticationToken(
                "internal-service",
                null,
                List.of(new SimpleGrantedAuthority(INTERNAL_ROLE))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    // Escreve resposta 401 padronizada (mesmo shape do ApiErrorResponse: code/message/timestamp/path).
    private static void unauthorized(HttpServletResponse response, HttpServletRequest request, String message)
            throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        String body = """
                {"code":"UNAUTHORIZED_INTERNAL","message":"%s","timestamp":"%s","path":"%s"}"""
                .formatted(
                        escape(message),
                        java.time.Instant.now(),
                        escape(request.getRequestURI())
                );
        response.getWriter().write(body);
    }

    private static String escape(String v) {
        return v == null ? "" : v.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // Comparação de tokens em tempo (aprox.) constante para reduzir vazamento por timing.
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] ab = a.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] bb = b.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return java.security.MessageDigest.isEqual(ab, bb);
    }
}
