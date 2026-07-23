package br.com.ecofy.ms_ingestion.adapters.in.web.correlation;

import org.slf4j.MDC;

import java.util.UUID;
import java.util.regex.Pattern;

// Centraliza a validação, a geração e a recuperação do identificador de correlação.
public final class CorrelationId {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";
    public static final String REQUEST_ATTRIBUTE = "ecofy.ingestion.correlationId";
    public static final String KAFKA_HEADER = HEADER;
    public static final int MAX_LENGTH = 128;

    private static final Pattern SAFE = Pattern.compile("^[A-Za-z0-9._-]+$");

    private CorrelationId() {
    }

    // Valida o formato e o tamanho do identificador recebido.
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

    // Resolve um identificador válido ou gera um novo.
    public static String sanitizeOrGenerate(String received) {
        return isValid(received) ? received.trim() : generate();
    }

    // Recupera o identificador associado ao contexto atual.
    public static String current() {
        return MDC.get(MDC_KEY);
    }

    // Recupera o identificador atual ou gera um novo quando ausente.
    public static String currentOrGenerate() {
        String current = current();
        return current != null ? current : generate();
    }
}
