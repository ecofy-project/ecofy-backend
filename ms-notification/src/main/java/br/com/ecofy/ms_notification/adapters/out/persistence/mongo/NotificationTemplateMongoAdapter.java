package br.com.ecofy.ms_notification.adapters.out.persistence.mongo;

import br.com.ecofy.ms_notification.adapters.out.persistence.mapper.TemplateMapper;
import br.com.ecofy.ms_notification.adapters.out.persistence.repository.NotificationTemplateMongoRepository;
import br.com.ecofy.ms_notification.core.domain.NotificationTemplate;
import br.com.ecofy.ms_notification.core.domain.enums.DomainEventType;
import br.com.ecofy.ms_notification.core.domain.enums.NotificationChannel;
import br.com.ecofy.ms_notification.core.domain.valueobject.TemplateId;
import br.com.ecofy.ms_notification.core.domain.valueobject.UserId;
import br.com.ecofy.ms_notification.core.port.out.LoadNotificationTemplatePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class NotificationTemplateMongoAdapter implements LoadNotificationTemplatePort {

    private final NotificationTemplateMongoRepository repo;
    private final TemplateMapper mapper;

    public NotificationTemplateMongoAdapter(NotificationTemplateMongoRepository repo, TemplateMapper mapper) {
        this.repo = Objects.requireNonNull(repo, "repo must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    // Persiste um NotificationTemplate no Mongo (doc -> repo.save -> domain) com logs de sucesso/falha e rethrow em erro.
    public NotificationTemplate save(NotificationTemplate template) {
        if (template == null) throw new IllegalArgumentException("template must not be null");

        UUID templateId = template.getId() != null ? template.getId().value() : null;
        UUID ownerUserId = template.getOwnerUserId() != null ? template.getOwnerUserId().value() : null;

        try {
            var saved = repo.save(mapper.toDoc(template));
            var domain = mapper.toDomain(saved);

            log.debug(
                    "[NotificationTemplateMongoAdapter] - [save] -> saved templateId={} ownerUserId={} eventType={} channel={} active={}",
                    domain.getId() != null ? domain.getId().value() : templateId,
                    domain.getOwnerUserId() != null ? domain.getOwnerUserId().value() : ownerUserId,
                    domain.getEventType(),
                    domain.getChannel(),
                    domain.isActive()
            );

            return domain;
        } catch (Exception ex) {
            log.error(
                    "[NotificationTemplateMongoAdapter] - [save] -> failed to save templateId={} ownerUserId={}",
                    templateId,
                    ownerUserId,
                    ex
            );
            throw ex;
        }
    }

    // Carrega um template por TemplateId, retornando Optional e registrando em log se foi encontrado ou não (com tratamento de erro).
    @Override
    public Optional<NotificationTemplate> loadById(TemplateId id) {
        Objects.requireNonNull(id, "id must not be null");
        UUID uuid = Objects.requireNonNull(id.value(), "id.value must not be null");

        try {
            var opt = repo.findById(uuid).map(mapper::toDomain);

            log.debug(
                    "[NotificationTemplateMongoAdapter] - [loadById] -> loaded templateId={} found={}",
                    uuid,
                    opt.isPresent()
            );

            return opt;
        } catch (Exception ex) {
            log.error(
                    "[NotificationTemplateMongoAdapter] - [loadById] -> failed to load templateId={}",
                    uuid,
                    ex
            );
            throw ex;
        }
    }

    // Resolve o template ativo por (userId opcional, eventType, channel), priorizando template do usuário e fazendo fallback para template global (ownerUserId null).
    @Override
    public Optional<NotificationTemplate> loadActiveTemplate(
            UserId userIdOrNull,
            DomainEventType eventType,
            NotificationChannel channel
    ) {
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(channel, "channel must not be null");

        UUID ownerUserId = userIdOrNull != null ? userIdOrNull.value() : null;

        try {
            // 1) tenta template específico do usuário
            if (ownerUserId != null) {
                var userTemplate = repo.findFirstByOwnerUserIdAndEventTypeAndChannelAndActiveTrue(
                        ownerUserId, eventType, channel
                );

                if (userTemplate.isPresent()) {
                    log.debug(
                            "[NotificationTemplateMongoAdapter] - [loadActiveTemplate] -> found user template ownerUserId={} eventType={} channel={}",
                            ownerUserId,
                            eventType,
                            channel
                    );
                    return userTemplate.map(mapper::toDomain);
                }
            }

            // 2) fallback: template global (ownerUserId null)
            var globalTemplate = repo.findFirstByOwnerUserIdIsNullAndEventTypeAndChannelAndActiveTrue(eventType, channel);

            log.debug(
                    "[NotificationTemplateMongoAdapter] - [loadActiveTemplate] -> userTemplateFound={} globalTemplateFound={} ownerUserId={} eventType={} channel={}",
                    false,
                    globalTemplate.isPresent(),
                    ownerUserId,
                    eventType,
                    channel
            );

            return globalTemplate.map(mapper::toDomain);
        } catch (Exception ex) {
            log.error(
                    "[NotificationTemplateMongoAdapter] - [loadActiveTemplate] -> failed to load active template ownerUserId={} eventType={} channel={}",
                    ownerUserId,
                    eventType,
                    channel,
                    ex
            );
            throw ex;
        }
    }

}
