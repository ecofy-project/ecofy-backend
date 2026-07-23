package br.com.ecofy.ms_notification.core.application.service;

import br.com.ecofy.ms_notification.core.application.command.HandleDomainEventCommand;
import br.com.ecofy.ms_notification.core.application.command.SendNotificationCommand;
import br.com.ecofy.ms_notification.core.application.config.NotificationSettings;
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

// Orquestra o envio de notificações originadas por eventos de domínio.
@Slf4j
@Service
public class DomainEventNotificationService
        implements HandleDomainEventNotificationUseCase {

    private static final NotificationChannel FALLBACK_CHANNEL =
            NotificationChannel.EMAIL;

    private final NotificationSettings settings;
    private final SendNotificationUseCase sendNotificationUseCase;

    public DomainEventNotificationService(
            NotificationSettings settings,
            SendNotificationUseCase sendNotificationUseCase
    ) {
        this.settings = Objects.requireNonNull(
                settings,
                "settings must not be null"
        );
        this.sendNotificationUseCase = Objects.requireNonNull(
                sendNotificationUseCase,
                "sendNotificationUseCase must not be null"
        );
    }

    // Processa eventos de domínio e encaminha comandos normalizados para envio.
    @Override
    public void handle(HandleDomainEventCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        UUID userId = Objects.requireNonNull(
                command.userId(),
                "command.userId must not be null"
        );
        DomainEventType eventType = Objects.requireNonNull(
                command.eventType(),
                "command.eventType must not be null"
        );

        Map<String, Object> payload = safePayload(command.payload());
        NotificationChannel channel = resolveDefaultChannel(eventType);
        String idem = blankToNull(command.idempotencyKey());

        log.debug(
                "[DomainEventNotificationService] - [handle] -> Tratando evento de domínio userId={} eventType={} channel={} hasPayload={} hasIdempotencyKey={}",
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

    // Resolve o canal configurado com fallback para valores ausentes ou inválidos.
    private NotificationChannel resolveDefaultChannel(DomainEventType type) {
        String raw = settings.defaultChannelFor(type.name());

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
                    "[DomainEventNotificationService] - [resolveDefaultChannel] -> Canal padrão configurado inválido eventType={} configured={} fallback={}",
                    type,
                    raw,
                    FALLBACK_CHANNEL
            );
            return FALLBACK_CHANNEL;
        }
    }

    // Protege o payload contra alterações externas após o recebimento.
    private static Map<String, Object> safePayload(
            Map<String, Object> payload
    ) {
        if (payload == null || payload.isEmpty()) {
            return Map.of();
        }
        return Map.copyOf(new HashMap<>(payload));
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank())
                ? null
                : value.trim();
    }
}
