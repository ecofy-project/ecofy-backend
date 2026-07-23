package br.com.ecofy.ms_notification.adapters.out.persistence.mapper;

import br.com.ecofy.ms_notification.adapters.out.persistence.document.DeliveryAttemptDocument;
import br.com.ecofy.ms_notification.core.domain.DeliveryAttempt;
import br.com.ecofy.ms_notification.core.domain.valueobject.NotificationId;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Component
public class AttemptMapper {

    // Converte o objeto de domínio DeliveryAttempt para o documento Mongo DeliveryAttemptDocument, normalizando campos opcionais e validando notificationId.
    public DeliveryAttemptDocument toDoc(DeliveryAttempt a) {
        if (a == null) throw new IllegalArgumentException("attempt must not be null");
        UUID notificationId = requireNotificationId(a);

        return DeliveryAttemptDocument.builder()
                .id(a.getId())
                .notificationId(notificationId)
                .channel(a.getChannel())
                .attemptNumber(a.getAttemptNumber())
                .status(a.getStatus())
                .provider(a.getProvider())
                .providerMessageId(blankToNull(a.getProviderMessageId()))
                .errorCode(blankToNull(a.getErrorCode()))
                .errorMessage(blankToNull(a.getErrorMessage()))
                .errorCategory(blankToNull(a.getErrorCategory()))
                .providerStatusCode(a.getProviderStatusCode())
                .nextRetryAt(a.getNextRetryAt())
                .correlationId(blankToNull(a.getCorrelationId()))
                .createdAt(a.getCreatedAt())
                .build();
    }

    // Converte o documento Mongo DeliveryAttemptDocument para o objeto de domínio DeliveryAttempt, recriando NotificationId e normalizando strings em branco.
    public DeliveryAttempt toDomain(DeliveryAttemptDocument d) {
        if (d == null) throw new IllegalArgumentException("document must not be null");
        UUID notificationId = Objects.requireNonNull(d.getNotificationId(), "document.notificationId must not be null");

        return DeliveryAttempt.builder()
                .id(d.getId())
                .notificationId(new NotificationId(notificationId))
                .channel(d.getChannel())
                .attemptNumber(d.getAttemptNumber())
                .status(d.getStatus())
                .provider(d.getProvider())
                .providerMessageId(blankToNull(d.getProviderMessageId()))
                .errorCode(blankToNull(d.getErrorCode()))
                .errorMessage(blankToNull(d.getErrorMessage()))
                .errorCategory(blankToNull(d.getErrorCategory()))
                .providerStatusCode(d.getProviderStatusCode())
                .nextRetryAt(d.getNextRetryAt())
                .correlationId(blankToNull(d.getCorrelationId()))
                .createdAt(d.getCreatedAt())
                .build();
    }

    // Extrai e valida o UUID de notificationId do domínio (NotificationId.value), falhando rápido caso esteja ausente/nulo.
    private static UUID requireNotificationId(DeliveryAttempt a) {
        if (a.getNotificationId() == null) throw new IllegalArgumentException("attempt.notificationId must not be null");
        UUID id = a.getNotificationId().value();
        if (id == null) throw new IllegalArgumentException("attempt.notificationId.value must not be null");
        return id;
    }

    // Normaliza strings removendo espaços e convertendo valores nulos/em branco para null (útil para persistência e índices).
    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

}
