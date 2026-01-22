package br.com.ecofy.ms_notification.adapters.out.persistence.document;

import br.com.ecofy.ms_notification.core.domain.enums.DomainEventType;
import br.com.ecofy.ms_notification.core.domain.enums.NotificationChannel;
import br.com.ecofy.ms_notification.core.domain.enums.NotificationStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notifications")
public class NotificationDocument {

    @Id
    private UUID id;

    private UUID userId;
    private DomainEventType eventType;
    private NotificationChannel channel;

    private String destination;
    private String subject;
    private String body;

    private NotificationStatus status;
    private int attemptCount;

    private String idempotencyKey;

    private Map<String, Object> payload;

    private Instant createdAt;
    private Instant updatedAt;

}
