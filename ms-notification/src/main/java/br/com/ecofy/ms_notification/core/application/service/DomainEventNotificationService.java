package br.com.ecofy.ms_notification.core.application.service;

import br.com.ecofy.ms_notification.config.NotificationProperties;
import br.com.ecofy.ms_notification.core.application.command.HandleDomainEventCommand;
import br.com.ecofy.ms_notification.core.application.command.SendNotificationCommand;
import br.com.ecofy.ms_notification.core.domain.enums.DomainEventType;
import br.com.ecofy.ms_notification.core.domain.enums.NotificationChannel;
import br.com.ecofy.ms_notification.core.port.in.HandleDomainEventNotificationUseCase;
import br.com.ecofy.ms_notification.core.port.in.SendNotificationUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class DomainEventNotificationService implements HandleDomainEventNotificationUseCase {

    private static final NotificationChannel FALLBACK_CHANNEL = NotificationChannel.EMAIL;

    private final NotificationProperties props;
    private final SendNotificationUseCase sendNotificationUseCase;

    public DomainEventNotificationService(
            NotificationProperties props,
            SendNotificationUseCase sendNotificationUseCase
    ) {
        this.props = Objects.requireNonNull(props, "props must not be null");
        this.sendNotificationUseCase = Objects.requireNonNull(sendNotificationUseCase, "sendNotificationUseCase must not be null");
        Objects.requireNonNull(props.getTemplates(), "props.templates must not be null");
        Objects.requireNonNull(props.getTemplates().getDefaultChannels(), "props.templates.defaultChannels must not be null");
    }

    @Override
    public void handle(HandleDomainEventCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        UUID userId = Objects.requireNonNull(command.userId(), "command.userId must not be null");
        DomainEventType eventType = Objects.requireNonNull(command.eventType(), "command.eventType must not be null");

        Map<String, Object> payload = safePayload(command.payload());
        NotificationChannel channel = resolveDefaultChannel(eventType);

        String idem = blankToNull(command.idempotencyKey());

        log.debug(
                "[DomainEventNotificationService] - [handle] -> handling domain event userId={} eventType={} channel={} hasPayload={} hasIdempotencyKey={}",
                userId,
                eventType,
                channel,
                !payload.isEmpty(),
                idem != null
        );

        var sendCmd = new SendNotificationCommand(
                userId,
                eventType,
                channel,
                null,
                payload,
                idem
        );

        sendNotificationUseCase.send(sendCmd);
    }

    private NotificationChannel resolveDefaultChannel(DomainEventType type) {
        String raw = props.getTemplates().getDefaultChannels().get(type.name());

        if (raw == null || raw.isBlank()) {
            log.debug(
                    "[DomainEventNotificationService] - [resolveDefaultChannel] -> no default channel configured for eventType={}, using fallback={}",
                    type,
                    FALLBACK_CHANNEL
            );
            return FALLBACK_CHANNEL;
        }

        String normalized = raw.trim().toUpperCase();

        try {
            return NotificationChannel.valueOf(normalized);
        } catch (Exception ex) {
            log.warn(
                    "[DomainEventNotificationService] - [resolveDefaultChannel] -> invalid default channel configured eventType={} configured={} fallback={}",
                    type,
                    raw,
                    FALLBACK_CHANNEL
            );
            return FALLBACK_CHANNEL;
        }
    }

    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) return Map.of();
        // cópia defensiva: evita que mutações externas afetem o comando
        return Map.copyOf(new HashMap<>(payload));
    }

    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }
}
