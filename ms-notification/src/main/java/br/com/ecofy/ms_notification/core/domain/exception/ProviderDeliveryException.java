package br.com.ecofy.ms_notification.core.domain.exception;

import br.com.ecofy.ms_notification.core.domain.enums.DeliveryErrorCategory;

import java.time.Duration;

public class ProviderDeliveryException extends RuntimeException {

    private final DeliveryErrorCategory category;
    private final Integer providerStatusCode;
    private final Duration retryAfter;
    private final String errorCode;

    public ProviderDeliveryException(String message, DeliveryErrorCategory category, String errorCode,
                                     Integer providerStatusCode, Duration retryAfter, Throwable cause) {
        super(message, cause);
        this.category = category == null ? DeliveryErrorCategory.TEMPORARY_FAILURE : category;
        this.errorCode = errorCode;
        this.providerStatusCode = providerStatusCode;
        this.retryAfter = retryAfter;
    }

    public ProviderDeliveryException(String message, DeliveryErrorCategory category, String errorCode) {
        this(message, category, errorCode, null, null, null);
    }

    public DeliveryErrorCategory getCategory() {
        return category;
    }

    public Integer getProviderStatusCode() {
        return providerStatusCode;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public boolean isRetryable() {
        return category.retryable();
    }
}
