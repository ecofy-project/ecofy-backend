package br.com.ecofy.auth.adapters.in.web.ratelimit;

import br.com.ecofy.auth.adapters.in.web.correlation.CorrelationId;
import br.com.ecofy.auth.adapters.in.web.dto.response.ApiErrorResponse;
import br.com.ecofy.auth.config.RateLimitProperties;
import br.com.ecofy.auth.core.application.exception.AuthErrorCode;
import br.com.ecofy.auth.core.domain.ratelimit.RateLimitDecision;
import br.com.ecofy.auth.core.domain.ratelimit.RateLimitPolicy;
import br.com.ecofy.auth.core.port.out.RateLimiterPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// Aplica limites de requisição configurados para endpoints sensíveis.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    // Relaciona as rotas protegidas às respectivas operações de rate limiting.
    private static final Map<String, String> POST_OPERATIONS = Map.ofEntries(
            Map.entry("/api/v1/auth/token", "login"),
            Map.entry("/api/v1/auth/refresh", "refresh"),
            Map.entry("/api/v1/auth/validate", "validate"),
            Map.entry("/api/v1/auth/register", "register"),
            Map.entry("/api/v1/auth/password/reset-request", "password-reset"),
            Map.entry("/api/v1/auth/password/reset-confirm", "password-reset"),
            Map.entry("/api/auth/token", "login"),
            Map.entry("/api/auth/refresh", "refresh"),
            Map.entry("/api/auth/validate", "validate"),
            Map.entry("/api/register", "register"),
            Map.entry("/api/password/reset-request", "password-reset"),
            Map.entry("/api/password/reset-confirm", "password-reset")
    );

    private final RateLimiterPort rateLimiter;
    private final RateLimitProperties properties;
    private final ClientIpResolver clientIpResolver;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public RateLimitFilter(
            RateLimiterPort rateLimiter,
            RateLimitProperties properties,
            ClientIpResolver clientIpResolver,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry
    ) {
        this.rateLimiter = rateLimiter;
        this.properties = properties;
        this.clientIpResolver = clientIpResolver;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    // Interrompe requisições que excedem a política associada à operação.
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        Optional<RateLimitPolicy> policy = resolvePolicy(request);

        if (policy.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitPolicy p = policy.get();
        String ip = clientIpResolver.resolve(request);
        String key = p.name() + ":ip:" + ip;

        RateLimitDecision decision = rateLimiter.tryConsume(key, p);

        if (decision.allowed()) {
            filterChain.doFilter(request, response);
            return;
        }

        meterRegistry.counter(
                "ecofy.auth.rate_limit",
                "operation",
                p.name(),
                "outcome",
                "blocked"
        ).increment();

        log.warn(
                "[RateLimitFilter] -> Limite excedido operation={} path={}",
                p.name(),
                request.getRequestURI()
        );

        writeTooManyRequests(request, response, decision.retryAfter());
    }

    // Resolve a política configurada para a rota e o método recebidos.
    private Optional<RateLimitPolicy> resolvePolicy(HttpServletRequest request) {
        if (!properties.isEnabled()
                || !"POST".equalsIgnoreCase(request.getMethod())) {
            return Optional.empty();
        }

        String operation = POST_OPERATIONS.get(pathWithinApplication(request));

        if (operation == null) {
            return Optional.empty();
        }

        return properties.policyFor(operation);
    }

    // Remove o contexto da aplicação para identificar a rota interna.
    private String pathWithinApplication(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();

        if (context != null
                && !context.isEmpty()
                && uri.startsWith(context)) {
            return uri.substring(context.length());
        }

        return uri;
    }

    // Escreve a resposta padronizada para requisições bloqueadas.
    private void writeTooManyRequests(
            HttpServletRequest request,
            HttpServletResponse response,
            Duration retryAfter
    ) throws IOException {

        long retryAfterSeconds = Math.max(
                1,
                (long) Math.ceil(retryAfter.toMillis() / 1000d)
        );

        response.setStatus(
                AuthErrorCode.RATE_LIMIT_EXCEEDED.getHttpStatus().value()
        );
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(
                HttpHeaders.RETRY_AFTER,
                Long.toString(retryAfterSeconds)
        );

        ApiErrorResponse body = ApiErrorResponse.of(
                AuthErrorCode.RATE_LIMIT_EXCEEDED.getHttpStatus().value(),
                AuthErrorCode.RATE_LIMIT_EXCEEDED.getCode(),
                "Muitas tentativas foram realizadas. Tente novamente mais tarde.",
                request.getRequestURI(),
                CorrelationId.current()
        );

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
