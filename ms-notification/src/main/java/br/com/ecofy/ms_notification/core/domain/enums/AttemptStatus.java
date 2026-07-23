package br.com.ecofy.ms_notification.core.domain.enums;

// Define os estados possíveis de uma tentativa de entrega, alinhados à taxonomia de falhas de provider.
public enum AttemptStatus {

    SUCCESS,
    FAILED,
    RETRYING,
    RETRY_SCHEDULED,
    INVALID_DESTINATION,
    PROVIDER_BLOCKED,
    DISCARDED
}
