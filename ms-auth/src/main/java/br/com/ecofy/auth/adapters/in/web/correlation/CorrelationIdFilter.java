package br.com.ecofy.auth.adapters.in.web.correlation;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// Resolve e propaga o correlation ID durante o processamento da requisição.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private final MeterRegistry meterRegistry;

    public CorrelationIdFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    // Configura o correlation ID no contexto e garante sua remoção após a requisição.
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String received = request.getHeader(CorrelationId.HEADER);
        String correlationId;

        if (received == null) {
            meterRegistry.counter("ecofy.auth.correlation.missing").increment();
            correlationId = CorrelationId.generate();
        } else if (CorrelationId.isValid(received)) {
            correlationId = received.trim();
        } else {
            meterRegistry.counter("ecofy.auth.correlation.invalid").increment();
            correlationId = CorrelationId.generate();
        }

        request.setAttribute(CorrelationId.REQUEST_ATTRIBUTE, correlationId);
        response.setHeader(CorrelationId.HEADER, correlationId);
        MDC.put(CorrelationId.MDC_KEY, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CorrelationId.MDC_KEY);
        }
    }
}
