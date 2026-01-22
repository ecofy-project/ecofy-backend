package br.com.ecofy.ms_notification.adapters.out.persistence.document;

import br.com.ecofy.ms_notification.core.domain.enums.DomainEventType;
import br.com.ecofy.ms_notification.core.domain.enums.NotificationChannel;
import br.com.ecofy.ms_notification.core.domain.enums.TemplateEngine;
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
@Document(collection = "notification_templates")
public class NotificationTemplateDocument {

    @Id
    private UUID id;

    private UUID ownerUserId; // null = global
    private DomainEventType eventType;
    private NotificationChannel channel;
    private TemplateEngine engine;

    private String subjectTemplate;
    private String bodyTemplate;

    private boolean active;

    private Instant createdAt;
    private Instant updatedAt;

}