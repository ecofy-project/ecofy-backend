package br.com.ecofy.ms_budgeting.adapters.in.web.filter;

import br.com.ecofy.ms_budgeting.adapters.correlation.CorrelationContext;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
// Resolve e propaga o identificador de correlação das requisições HTTP.
public class CorrelationIdFilter extends OncePerRequestFilter {

    private final MeterRegistry meterRegistry;

    public CorrelationIdFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    // Propaga um identificador de correlação válido durante o processamento da requisição.
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String received = request.getHeader(CorrelationContext.HEADER);
        String correlationId;

        if (received == null) {
            meterRegistry.counter("ecofy.budgeting.correlation.missing").increment();
            correlationId = CorrelationContext.generate();
        } else if (CorrelationContext.isValid(received)) {
            correlationId = received.trim();
        } else {
            meterRegistry.counter("ecofy.budgeting.correlation.invalid").increment();
            correlationId = CorrelationContext.generate();
        }

        response.setHeader(CorrelationContext.HEADER, correlationId);
        CorrelationContext.put(correlationId, null);

        try {
            filterChain.doFilter(request, response);
        } finally {
            CorrelationContext.clear();
        }
    }
}
