package br.com.ecofy.ms_notification.adapters.out.persistence.document;

import br.com.ecofy.ms_notification.core.domain.enums.AttemptStatus;
import br.com.ecofy.ms_notification.core.domain.enums.NotificationChannel;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "delivery_attempts")
public class DeliveryAttemptDocument {

    @Id
    private UUID id;

    private UUID notificationId;
    private NotificationChannel channel;

    private int attemptNumber;
    private AttemptStatus status;

    private String provider;
    private String providerMessageId;

    private String errorCode;
    private String errorMessage;

    // Etapa 8 (§11): auditoria enriquecida.
    private String errorCategory;
    private Integer providerStatusCode;
    private Instant nextRetryAt;
    private String correlationId;

    private Instant createdAt;

}
