package br.com.ecofy.ms_notification.core.domain.enums;

// Classifica falhas de entrega de provider e decide quais tentativas podem receber retry.
public enum DeliveryErrorCategory {

    TEMPORARY_FAILURE(true),

    PERMANENT_FAILURE(false),

    INVALID_DESTINATION(false),

    PROVIDER_BLOCKED(false),

    RATE_LIMITED(true);

    private final boolean retryable;

    DeliveryErrorCategory(boolean retryable) {
        this.retryable = retryable;
    }

    public boolean retryable() {
        return retryable;
    }
}
