package br.com.ecofy.ms_notification.core.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record TemplateId(UUID value) {

    // Garante que o identificador do template seja válido (não nulo) ao criar o value object.
    public TemplateId {
        Objects.requireNonNull(value, "templateId must not be null");
    }

    // Gera um novo TemplateId com UUID aleatório para uso na criação de novos templates.
    public static TemplateId newId() {
        return new TemplateId(UUID.randomUUID());
    }

}
