package br.com.ecofy.ms_notification.core.domain;

import br.com.ecofy.ms_notification.core.domain.enums.DomainEventType;
import br.com.ecofy.ms_notification.core.domain.enums.NotificationChannel;
import br.com.ecofy.ms_notification.core.domain.enums.TemplateEngine;
import br.com.ecofy.ms_notification.core.domain.valueobject.TemplateId;
import br.com.ecofy.ms_notification.core.domain.valueobject.UserId;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

// Define o conteúdo e as regras de um template de notificação.
@Getter
@Builder(toBuilder = true)
public class NotificationTemplate {

    private final TemplateId id;
    private final UserId ownerUserId;
    private final DomainEventType eventType;
    private final NotificationChannel channel;
    private final TemplateEngine engine;

    private final String subjectTemplate;
    private final String bodyTemplate;

    private final boolean active;
    private final Instant createdAt;
    private final Instant updatedAt;

    // Valida os campos obrigatórios conforme o canal configurado.
    public NotificationTemplate validate() {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(channel, "channel must not be null");
        Objects.requireNonNull(engine, "engine must not be null");
        Objects.requireNonNull(bodyTemplate, "bodyTemplate must not be null");

        if (bodyTemplate.isBlank()) {
            throw new IllegalArgumentException(
                    "bodyTemplate must not be blank"
            );
        }

        if (channel == NotificationChannel.EMAIL
                && (subjectTemplate == null || subjectTemplate.isBlank())) {
            throw new IllegalArgumentException(
                    "subjectTemplate must not be blank for EMAIL"
            );
        }

        return this;
    }

    // Renderiza o assunto quando definido pelo template.
    public String renderSubject(Map<String, Object> vars) {
        if (subjectTemplate == null) {
            return null;
        }
        return SimpleTemplateEngine.render(subjectTemplate, vars);
    }

    // Renderiza o corpo com as variáveis fornecidas.
    public String renderBody(Map<String, Object> vars) {
        return SimpleTemplateEngine.render(bodyTemplate, vars);
    }
}
