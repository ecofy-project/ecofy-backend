package br.com.ecofy.gateway.api_gateway.correlation;

import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.regex.Pattern;

// Valida e gera correlation IDs seguros para headers e logs.
@Component
public class CorrelationIdValidator {

    private static final Pattern SAFE = Pattern.compile("^[A-Za-z0-9._-]+$");

    private final CorrelationProperties properties;

    public CorrelationIdValidator(CorrelationProperties properties) {
        this.properties = properties;
    }

    // Valida o formato e o tamanho do correlation ID recebido.
    public boolean isValid(String candidate) {
        if (candidate == null) {
            return false;
        }

        String trimmed = candidate.trim();

        if (trimmed.isEmpty() || trimmed.length() > properties.getMaxLength()) {
            return false;
        }

        return SAFE.matcher(trimmed).matches();
    }

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
