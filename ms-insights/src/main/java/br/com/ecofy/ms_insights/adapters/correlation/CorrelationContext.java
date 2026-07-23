package br.com.ecofy.ms_insights.adapters.correlation;

import org.slf4j.MDC;

import java.util.UUID;
import java.util.regex.Pattern;

// Centraliza a correlação e a causalidade dos fluxos HTTP, Kafka e Outbox.
public final class CorrelationContext {

    public static final String HEADER = "X-Correlation-Id";
    public static final String KAFKA_HEADER = HEADER;
    public static final String KAFKA_EVENT_ID_HEADER = "eventId";

    public static final String MDC_CORRELATION_KEY = "correlationId";
    public static final String MDC_CAUSATION_KEY = "causationId";

    public static final int MAX_LENGTH = 128;

    private static final Pattern SAFE = Pattern.compile("^[A-Za-z0-9._-]+$");

    private CorrelationContext() {
    }

    public static boolean isValid(String candidate) {
        if (candidate == null) {
            return false;
        }
        String trimmed = candidate.trim();
        return !trimmed.isEmpty() && trimmed.length() <= MAX_LENGTH && SAFE.matcher(trimmed).matches();
    }

    public static String generate() {
        return UUID.randomUUID().toString();
    }

    public static String sanitizeOrGenerate(String received) {
        return isValid(received) ? received.trim() : generate();
    }

    public static String currentCorrelationId() {
        return MDC.get(MDC_CORRELATION_KEY);
    }

    public static String currentCorrelationIdOrGenerate() {
        String current = currentCorrelationId();
        return current != null ? current : generate();
    }

    public static UUID currentCausationId() {
        String raw = MDC.get(MDC_CAUSATION_KEY);
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // Propaga os identificadores para o contexto de logging.
    public static void put(String correlationId, UUID causationId) {
        if (correlationId != null) {
            MDC.put(MDC_CORRELATION_KEY, correlationId);
        }
        if (causationId != null) {
            MDC.put(MDC_CAUSATION_KEY, causationId.toString());
        }
    }

    public static void clear() {
        MDC.remove(MDC_CORRELATION_KEY);
        MDC.remove(MDC_CAUSATION_KEY);
    }
}
