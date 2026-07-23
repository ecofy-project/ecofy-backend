package br.com.ecofy.ms_users.adapters.in.web.correlation;

import org.slf4j.MDC;

import java.util.UUID;
import java.util.regex.Pattern;

// Centraliza a criação, validação e recuperação do identificador de correlação.
public final class CorrelationId {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";
    public static final String REQUEST_ATTRIBUTE =
            "ecofy.users.correlationId";
    public static final int MAX_LENGTH = 128;

    private static final Pattern SAFE =
            Pattern.compile("^[A-Za-z0-9._-]+$");

    private CorrelationId() {
    }

    public static boolean isValid(String candidate) {
        if (candidate == null) {
            return false;
        }

        String trimmed = candidate.trim();

        if (trimmed.isEmpty() || trimmed.length() > MAX_LENGTH) {
            return false;
        }

        return SAFE.matcher(trimmed).matches();
    }

    public static String generate() {
        return UUID.randomUUID().toString();
    }

    // Resolve um identificador seguro com fallback para um novo valor.
    public static String sanitizeOrGenerate(String received) {
        return isValid(received) ? received.trim() : generate();
    }

    // Recupera o identificador de correlação disponível no contexto atual.
    public static String current() {
        return MDC.get(MDC_KEY);
    }
}
