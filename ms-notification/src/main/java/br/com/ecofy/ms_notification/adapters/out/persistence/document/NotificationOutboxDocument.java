package br.com.ecofy.ms_notification.adapters.out.persistence.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notification_outbox")
public class NotificationOutboxDocument {

    @Id
    private UUID id;

    @Indexed(unique = true)
    private UUID aggregateId;

    private String eventType;
    private int eventVersion;
    private String topic;
    private String partitionKey;

    private String payload;

    private String correlationId;
    private UUID causationId;

    private String status;
    private int attempts;
    private Instant nextAttemptAt;

    private Instant occurredAt;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant publishedAt;
    private String lastErrorCode;
}
