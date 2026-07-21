package br.com.ecofy.auth.adapters.in.web.correlation;

import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;

// Centraliza a validação, a geração e o acesso ao correlation ID.
public final class CorrelationId {

    public static final String HEADER = "X-Correlation-Id";

    public static final String MDC_KEY = "correlationId";

    public static final String REQUEST_ATTRIBUTE = "ecofy.auth.correlationId";

    // Limita o tamanho aceito para evitar headers desproporcionais.
    public static final int MAX_LENGTH = 128;

    // Restringe o correlation ID a caracteres seguros para headers e logs.
    private static final Pattern SAFE = Pattern.compile("^[A-Za-z0-9._-]+$");

    private CorrelationId() {
    }

    // Valida o formato e o tamanho do correlation ID recebido.
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

    // Recupera o correlation ID associado ao contexto atual.
    public static String current() {
        return MDC.get(MDC_KEY);
    }
}
