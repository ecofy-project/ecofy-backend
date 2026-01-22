package br.com.ecofy.ms_notification.core.application.service;

import br.com.ecofy.ms_notification.core.application.command.PreviewTemplateCommand;
import br.com.ecofy.ms_notification.core.application.result.TemplatePreviewResult;
import br.com.ecofy.ms_notification.core.domain.enums.NotificationChannel;
import br.com.ecofy.ms_notification.core.domain.exception.TemplateNotFoundException;
import br.com.ecofy.ms_notification.core.domain.valueobject.UserId;
import br.com.ecofy.ms_notification.core.port.in.PreviewTemplateUseCase;
import br.com.ecofy.ms_notification.core.port.out.LoadNotificationTemplatePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class TemplatePreviewService implements PreviewTemplateUseCase {

    private final LoadNotificationTemplatePort loadTemplatePort;

    public TemplatePreviewService(LoadNotificationTemplatePort loadTemplatePort) {
        this.loadTemplatePort = Objects.requireNonNull(loadTemplatePort, "loadTemplatePort must not be null");
    }

    @Override
    public TemplatePreviewResult preview(PreviewTemplateCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(command.eventType(), "command.eventType must not be null");
        Objects.requireNonNull(command.channel(), "command.channel must not be null");

        UserId userIdOrNull = toUserIdOrNull(command.userId());
        Map<String, Object> payload = safePayload(command.payload());

        log.debug(
                "[TemplatePreviewService] - [preview] -> previewing template ownerUserId={} eventType={} channel={} hasPayload={}",
                userIdOrNull != null ? userIdOrNull.value() : null,
                command.eventType(),
                command.channel(),
                !payload.isEmpty()
        );

        var template = loadTemplatePort.loadActiveTemplate(
                        userIdOrNull,
                        command.eventType(),
                        command.channel()
                )
                .orElseThrow(() -> new TemplateNotFoundException(
                        "No active template found for ownerUserId=%s eventType=%s channel=%s"
                                .formatted(
                                        userIdOrNull != null ? userIdOrNull.value() : "GLOBAL",
                                        command.eventType(),
                                        command.channel()
                                )
                ));

        String subject = template.getChannel() == NotificationChannel.EMAIL
                ? template.renderSubject(payload)
                : null;

        String body = template.renderBody(payload);

        log.debug(
                "[TemplatePreviewService] - [preview] -> rendered template eventType={} channel={} subjectLen={} bodyLen={}",
                command.eventType(),
                command.channel(),
                subject != null ? subject.length() : 0,
                body != null ? body.length() : 0
        );

        return new TemplatePreviewResult(subject, body);
    }

    // Converte um UUID opcional em UserId; retorna null quando userId não é informado (template global).
    private static UserId toUserIdOrNull(UUID userId) {
        return userId == null ? null : new UserId(userId);
    }

    // Normaliza o payload para um Map imutável e nunca nulo (fallback Map.of()).
    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) return Map.of();
        return Map.copyOf(payload);
    }

}
