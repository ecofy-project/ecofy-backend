package br.com.ecofy.ms_notification.adapters.out.persistence.mapper;

import br.com.ecofy.ms_notification.adapters.out.persistence.document.NotificationTemplateDocument;
import br.com.ecofy.ms_notification.core.domain.NotificationTemplate;
import br.com.ecofy.ms_notification.core.domain.valueobject.TemplateId;
import br.com.ecofy.ms_notification.core.domain.valueobject.UserId;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Component
public class TemplateMapper {

    public NotificationTemplateDocument toDoc(NotificationTemplate t) {
        if (t == null) throw new IllegalArgumentException("template must not be null");

        UUID id = requireId(t);

        return NotificationTemplateDocument.builder()
                .id(id)
                .ownerUserId(toOwnerUserIdValue(t.getOwnerUserId()))
                .eventType(t.getEventType())
                .channel(t.getChannel())
                .engine(t.getEngine())
                .subjectTemplate(blankToNull(t.getSubjectTemplate()))
                .bodyTemplate(blankToNull(t.getBodyTemplate()))
                .active(t.isActive())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    public NotificationTemplate toDomain(NotificationTemplateDocument d) {
        if (d == null) throw new IllegalArgumentException("document must not be null");

        UUID id = Objects.requireNonNull(d.getId(), "document.id must not be null");

        return NotificationTemplate.builder()
                .id(new TemplateId(id))
                .ownerUserId(toOwnerUserId(d.getOwnerUserId()))
                .eventType(d.getEventType())
                .channel(d.getChannel())
                .engine(d.getEngine())
                .subjectTemplate(blankToNull(d.getSubjectTemplate()))
                .bodyTemplate(blankToNull(d.getBodyTemplate()))
                .active(d.isActive())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    private static UUID requireId(NotificationTemplate t) {
        if (t.getId() == null) throw new IllegalArgumentException("template.id must not be null");
        UUID id = t.getId().value();
        if (id == null) throw new IllegalArgumentException("template.id.value must not be null");
        return id;
    }

    private static UUID toOwnerUserIdValue(UserId owner) {
        return owner == null ? null : owner.value();
    }

    private static UserId toOwnerUserId(UUID ownerUserId) {
        return ownerUserId == null ? null : new UserId(ownerUserId);
    }

    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }
}
