package br.com.ecofy.ms_notification.core.application.service;

import br.com.ecofy.ms_notification.core.application.command.CreateTemplateCommand;
import br.com.ecofy.ms_notification.core.domain.NotificationTemplate;
import br.com.ecofy.ms_notification.core.domain.valueobject.TemplateId;
import br.com.ecofy.ms_notification.core.domain.valueobject.UserId;
import br.com.ecofy.ms_notification.core.port.in.CreateTemplateUseCase;
import br.com.ecofy.ms_notification.core.port.in.GetTemplateUseCase;
import br.com.ecofy.ms_notification.core.port.out.LoadNotificationTemplatePort;
import br.com.ecofy.ms_notification.core.port.out.SaveNotificationTemplatePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Correção Dia 7 (item #6): centraliza a criação/consulta de templates em um caso de uso do core,
 * removendo essa lógica (build + validate + persistência) do TemplateController, que antes acessava
 * o adapter Mongo concreto diretamente.
 */
@Slf4j
@Service
public class TemplateManagementService implements CreateTemplateUseCase, GetTemplateUseCase {

    private final SaveNotificationTemplatePort savePort;
    private final LoadNotificationTemplatePort loadPort;

    public TemplateManagementService(SaveNotificationTemplatePort savePort, LoadNotificationTemplatePort loadPort) {
        this.savePort = Objects.requireNonNull(savePort, "savePort must not be null");
        this.loadPort = Objects.requireNonNull(loadPort, "loadPort must not be null");
    }

    // Constrói, valida e persiste um novo template a partir do comando de aplicação.
    @Override
    public NotificationTemplate create(CreateTemplateCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        var now = Instant.now();

        var template = NotificationTemplate.builder()
                .id(TemplateId.newId())
                .ownerUserId(command.ownerUserId() == null ? null : new UserId(command.ownerUserId()))
                .eventType(command.eventType())
                .channel(command.channel())
                .engine(command.engine())
                .subjectTemplate(command.subjectTemplate())
                .bodyTemplate(command.bodyTemplate())
                .active(command.active())
                .createdAt(now)
                .updatedAt(now)
                .build()
                .validate();

        var saved = savePort.save(template);

        log.info(
                "[TemplateManagementService] - [create] -> created templateId={} ownerUserId={} eventType={} channel={} active={}",
                saved.getId().value(),
                saved.getOwnerUserId() == null ? null : saved.getOwnerUserId().value(),
                saved.getEventType(), saved.getChannel(), saved.isActive()
        );

        return saved;
    }

    // Busca um template por id, delegando ao port de carga.
    @Override
    public Optional<NotificationTemplate> findById(TemplateId id) {
        Objects.requireNonNull(id, "id must not be null");
        return loadPort.loadById(id);
    }
}
