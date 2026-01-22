package br.com.ecofy.ms_notification.core.application.service;

import br.com.ecofy.ms_notification.adapters.out.messaging.dto.NotificationSentEvent;
import br.com.ecofy.ms_notification.config.NotificationProperties;
import br.com.ecofy.ms_notification.core.application.command.ResendNotificationCommand;
import br.com.ecofy.ms_notification.core.application.command.SendNotificationCommand;
import br.com.ecofy.ms_notification.core.application.result.NotificationResult;
import br.com.ecofy.ms_notification.core.domain.DeliveryAttempt;
import br.com.ecofy.ms_notification.core.domain.Notification;
import br.com.ecofy.ms_notification.core.domain.enums.DomainEventType;
import br.com.ecofy.ms_notification.core.domain.enums.NotificationChannel;
import br.com.ecofy.ms_notification.core.domain.enums.NotificationStatus;
import br.com.ecofy.ms_notification.core.domain.exception.*;
import br.com.ecofy.ms_notification.core.domain.valueobject.ChannelAddress;
import br.com.ecofy.ms_notification.core.domain.valueobject.IdempotencyKey;
import br.com.ecofy.ms_notification.core.domain.valueobject.NotificationId;
import br.com.ecofy.ms_notification.core.domain.valueobject.UserId;
import br.com.ecofy.ms_notification.core.port.in.ResendNotificationUseCase;
import br.com.ecofy.ms_notification.core.port.in.SendNotificationUseCase;
import br.com.ecofy.ms_notification.core.port.out.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class NotificationService implements SendNotificationUseCase, ResendNotificationUseCase {

    private static final String DEFAULT_PUSH_TITLE = "EcoFy";
    private static final String DELIVERY_ERROR_CODE = "DELIVERY_FAILED";
    private static final String FAILURE_PROVIDER_PLACEHOLDER = "provider";

    private final NotificationProperties props;

    private final LoadNotificationTemplatePort loadTemplatePort;
    private final LoadUserContactInfoPort loadUserContactInfoPort;

    private final SaveNotificationPort saveNotificationPort;
    private final SaveDeliveryAttemptPort saveDeliveryAttemptPort;

    private final EmailSenderPort emailSenderPort;
    private final WhatsAppSenderPort whatsAppSenderPort;
    private final PushSenderPort pushSenderPort;

    private final IdempotencyPort idempotencyPort;
    private final PublishNotificationEventPort publishNotificationEventPort; // opcional

    public NotificationService(
            NotificationProperties props,
            LoadNotificationTemplatePort loadTemplatePort,
            LoadUserContactInfoPort loadUserContactInfoPort,
            SaveNotificationPort saveNotificationPort,
            SaveDeliveryAttemptPort saveDeliveryAttemptPort,
            EmailSenderPort emailSenderPort,
            WhatsAppSenderPort whatsAppSenderPort,
            PushSenderPort pushSenderPort,
            IdempotencyPort idempotencyPort,
            Optional<PublishNotificationEventPort> publishNotificationEventPort
    ) {
        this.props = Objects.requireNonNull(props, "props must not be null");
        this.loadTemplatePort = Objects.requireNonNull(loadTemplatePort, "loadTemplatePort must not be null");
        this.loadUserContactInfoPort = Objects.requireNonNull(loadUserContactInfoPort, "loadUserContactInfoPort must not be null");
        this.saveNotificationPort = Objects.requireNonNull(saveNotificationPort, "saveNotificationPort must not be null");
        this.saveDeliveryAttemptPort = Objects.requireNonNull(saveDeliveryAttemptPort, "saveDeliveryAttemptPort must not be null");
        this.emailSenderPort = Objects.requireNonNull(emailSenderPort, "emailSenderPort must not be null");
        this.whatsAppSenderPort = Objects.requireNonNull(whatsAppSenderPort, "whatsAppSenderPort must not be null");
        this.pushSenderPort = Objects.requireNonNull(pushSenderPort, "pushSenderPort must not be null");
        this.idempotencyPort = Objects.requireNonNull(idempotencyPort, "idempotencyPort must not be null");
        this.publishNotificationEventPort = publishNotificationEventPort.orElse(null);

        Objects.requireNonNull(props.getIdempotency(), "props.idempotency must not be null");
    }

    // Cria e envia uma notificação a partir do template ativo, resolve destino (override ou contatos do usuário), persiste e tenta entregar com idempotência.
    @Override
    public NotificationResult send(SendNotificationCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        UUID userIdRaw = Objects.requireNonNull(command.userId(), "command.userId must not be null");
        DomainEventType eventType = Objects.requireNonNull(command.eventType(), "command.eventType must not be null");
        NotificationChannel channel = Objects.requireNonNull(command.channel(), "command.channel must not be null");

        String idem = resolveIdempotencyKey(command.idempotencyKey(), "send");
        acquireIdempotencyIfEnabled(idem);

        var userId = new UserId(userIdRaw);
        Map<String, Object> payload = safePayload(command.payload());

        log.info(
                "[NotificationService] - [send] -> start userId={} eventType={} channel={} hasOverrideDest={} hasPayload={} idempotencyEnabled={} hasIdempotencyKey={}",
                userIdRaw,
                eventType,
                channel,
                command.destinationOverride() != null && !command.destinationOverride().isBlank(),
                !payload.isEmpty(),
                props.getIdempotency().isEnabled(),
                idem != null
        );

        var template = loadTemplatePort.loadActiveTemplate(userId, eventType, channel)
                .orElseThrow(() -> new TemplateNotFoundException(
                        "No active template found for eventType=%s channel=%s".formatted(eventType, channel)
                ));

        String destination = resolveDestination(command.destinationOverride(), channel, userId);
        var dest = new ChannelAddress(channel, destination);

        String subject = (channel == NotificationChannel.EMAIL) ? template.renderSubject(payload) : null;
        String body = template.renderBody(payload);

        var now = Instant.now();

        var notification = Notification.builder()
                .id(NotificationId.newId())
                .userId(userId)
                .eventType(eventType)
                .channel(channel)
                .destination(dest)
                .subject(subject)
                .body(body)
                .status(NotificationStatus.PENDING)
                .attemptCount(0)
                .idempotencyKey(idem == null ? null : new IdempotencyKey(idem))
                .payload(payload)
                .createdAt(now)
                .updatedAt(now)
                .build()
                .validateForSend();

        Notification saved = saveNotificationPort.save(notification);

        log.debug(
                "[NotificationService] - [send] -> persisted notificationId={} status={} attemptCount={}",
                saved.getId().value(),
                saved.getStatus(),
                saved.getAttemptCount()
        );

        Notification delivered = attemptDelivery(saved);

        log.info(
                "[NotificationService] - [send] -> completed notificationId={} status={} attemptCount={}",
                delivered.getId().value(),
                delivered.getStatus(),
                delivered.getAttemptCount()
        );

        return toResult(delivered);
    }

    // Reenvia uma notificação existente: aplica idempotência, carrega por id, reseta status para PENDING e executa uma nova tentativa de entrega.
    @Override
    public NotificationResult resend(ResendNotificationCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        UUID notificationUuid = Objects.requireNonNull(command.notificationId(), "command.notificationId must not be null");
        var notificationId = new NotificationId(notificationUuid);

        String idem = resolveIdempotencyKey(command.idempotencyKey(), "resend");
        acquireIdempotencyIfEnabled(idem);

        log.info(
                "[NotificationService] - [resend] -> start notificationId={} idempotencyEnabled={} hasIdempotencyKey={}",
                notificationUuid,
                props.getIdempotency().isEnabled(),
                idem != null
        );

        var current = saveNotificationPort.loadById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found: " + notificationUuid));

        // reset state for resend (mantém body/subject/destination/payload)
        var reset = current.toBuilder()
                .status(NotificationStatus.PENDING)
                .updatedAt(Instant.now())
                .build();

        Notification saved = saveNotificationPort.save(reset);

        log.debug(
                "[NotificationService] - [resend] -> reset persisted notificationId={} status={} attemptCount={}",
                saved.getId().value(),
                saved.getStatus(),
                saved.getAttemptCount()
        );

        Notification delivered = attemptDelivery(saved);

        log.info(
                "[NotificationService] - [resend] -> completed notificationId={} status={} attemptCount={}",
                delivered.getId().value(),
                delivered.getStatus(),
                delivered.getAttemptCount()
        );

        return toResult(delivered);
    }

    // Executa a entrega para o canal configurado (EMAIL/WHATSAPP/PUSH), grava tentativa (sucesso/erro), atualiza status e publica evento "sent" quando aplicável.
    private Notification attemptDelivery(Notification notification) {
        Objects.requireNonNull(notification, "notification must not be null");

        int attemptNumber = notification.getAttemptCount() + 1;

        try {
            log.debug(
                    "[NotificationService] - [attemptDelivery] -> attempting delivery notificationId={} channel={} attemptNumber={}",
                    notification.getId().value(),
                    notification.getChannel(),
                    attemptNumber
            );

            switch (notification.getChannel()) {
                case EMAIL -> {
                    var result = emailSenderPort.sendEmail(
                            notification.getDestination(),
                            requireNonNull(notification.getSubject(), "email subject must not be null"),
                            requireNonNull(notification.getBody(), "email body must not be null")
                    );
                    saveDeliveryAttemptPort.save(successAttempt(notification, attemptNumber, result.provider(), result.providerMessageId()));
                }
                case WHATSAPP -> {
                    var result = whatsAppSenderPort.sendWhatsApp(
                            notification.getDestination(),
                            requireNonNull(notification.getBody(), "whatsapp body must not be null")
                    );
                    saveDeliveryAttemptPort.save(successAttempt(notification, attemptNumber, result.provider(), result.providerMessageId()));
                }
                case PUSH -> {
                    String title = (notification.getSubject() == null || notification.getSubject().isBlank())
                            ? DEFAULT_PUSH_TITLE
                            : notification.getSubject();

                    var result = pushSenderPort.sendPush(
                            notification.getDestination(),
                            title,
                            requireNonNull(notification.getBody(), "push body must not be null")
                    );
                    saveDeliveryAttemptPort.save(successAttempt(notification, attemptNumber, result.provider(), result.providerMessageId()));
                }
                default -> throw new BusinessValidationException("Unsupported channel: " + notification.getChannel());
            }

            var updated = notification.toBuilder()
                    .status(NotificationStatus.SENT)
                    .attemptCount(attemptNumber)
                    .updatedAt(Instant.now())
                    .build();

            Notification saved = saveNotificationPort.save(updated);

            log.debug(
                    "[NotificationService] - [attemptDelivery] -> delivery success notificationId={} channel={} attemptNumber={} status={}",
                    saved.getId().value(),
                    saved.getChannel(),
                    attemptNumber,
                    saved.getStatus()
            );

            publishSentEventIfEnabled(saved);

            return saved;

        } catch (RuntimeException ex) {

            log.warn(
                    "[NotificationService] - [attemptDelivery] -> delivery failed notificationId={} channel={} attemptNumber={} error={}",
                    notification.getId().value(),
                    notification.getChannel(),
                    attemptNumber,
                    safeErrorMessage(ex)
            );

            // grava tentativa com erro
            saveDeliveryAttemptPort.save(failureAttempt(notification, attemptNumber, FAILURE_PROVIDER_PLACEHOLDER, DELIVERY_ERROR_CODE, safeErrorMessage(ex)));

            var updated = notification.toBuilder()
                    .status(NotificationStatus.FAILED)
                    .attemptCount(attemptNumber)
                    .updatedAt(Instant.now())
                    .build();

            saveNotificationPort.save(updated);

            throw new DeliveryProviderException(
                    "Delivery failed for notificationId=" + notification.getId().value(),
                    ex
            );
        }
    }

    // Publica o evento "notification.sent" no Kafka quando o publisher estiver disponível, sem quebrar o fluxo principal em caso de falha downstream.
    private void publishSentEventIfEnabled(Notification saved) {
        if (publishNotificationEventPort == null) return;

        try {
            publishNotificationEventPort.publish(NotificationSentEvent.from(saved));
            log.debug(
                    "[NotificationService] - [publishSentEventIfEnabled] -> published notification.sent notificationId={}",
                    saved.getId().value()
            );
        } catch (Exception ex) {
            // opção: não falhar o fluxo principal por erro em evento downstream
            log.error(
                    "[NotificationService] - [publishSentEventIfEnabled] -> failed to publish notification.sent notificationId={}",
                    saved.getId().value(),
                    ex
            );
        }
    }

    // Resolve o destino final da notificação usando override (se informado) ou consultando contatos do usuário conforme o canal (email/telefone/token).
    private String resolveDestination(String override, NotificationChannel channel, UserId userId) {
        if (override != null && !override.isBlank()) {
            String trimmed = override.trim();
            log.debug(
                    "[NotificationService] - [resolveDestination] -> using override destination channel={} userId={} destination={}",
                    channel,
                    userId.value(),
                    safeDestination(trimmed, channel)
            );
            return trimmed;
        }

        var contact = loadUserContactInfoPort.load(userId)
                .orElseThrow(() -> new BusinessValidationException("User contact info not found for userId=" + userId.value()));

        return switch (channel) {
            case EMAIL -> {
                if (contact.email() == null || contact.email().isBlank())
                    throw new BusinessValidationException("User has no email");
                yield contact.email().trim();
            }
            case WHATSAPP -> {
                if (contact.phoneE164() == null || contact.phoneE164().isBlank())
                    throw new BusinessValidationException("User has no phone");
                yield contact.phoneE164().trim();
            }
            case PUSH -> {
                if (contact.deviceToken() == null || contact.deviceToken().isBlank())
                    throw new BusinessValidationException("User has no device token");
                yield contact.deviceToken().trim();
            }
        };
    }

    // Aplica a política de idempotência (se habilitada): normaliza/gera chave, tenta adquirir no repositório e falha com conflito se já utilizada.
    private void acquireIdempotencyIfEnabled(String idempotencyKey) {
        if (!props.getIdempotency().isEnabled()) return;

        String keyValue = blankToNull(idempotencyKey);
        if (keyValue == null) {
            // sempre gera um, para manter consistência e rastreabilidade
            keyValue = UUID.randomUUID().toString();
        }

        var key = new IdempotencyKey(keyValue);

        boolean ok = idempotencyPort.tryAcquire(key);
        if (!ok) {
            log.warn(
                    "[NotificationService] - [acquireIdempotencyIfEnabled] -> idempotency violation key={}",
                    key.value()
            );
            throw new IdempotencyViolationException("Idempotency key already used: " + key.value());
        }

        log.debug(
                "[NotificationService] - [acquireIdempotencyIfEnabled] -> acquired idempotency key={}",
                key.value()
        );
    }

    // Gera/normaliza uma chave de idempotência para a operação (send/resend), anexando um sufixo estável para diferenciar cenários.
    private String resolveIdempotencyKey(String provided, String suffix) {
        String base = (provided == null || provided.isBlank())
                ? UUID.randomUUID().toString()
                : provided.trim();

        return base + ":" + suffix;
    }

    // Cria um objeto DeliveryAttempt de sucesso com metadados do provider (nome e messageId) para auditoria e rastreabilidade.
    private static DeliveryAttempt successAttempt(Notification notification, int attemptNumber, String provider, String providerMessageId) {
        return DeliveryAttempt.success(
                notification.getId(),
                notification.getChannel(),
                attemptNumber,
                provider,
                providerMessageId
        );
    }

    // Cria um objeto DeliveryAttempt de falha com código/mensagem de erro e provider (placeholder quando indisponível) para diagnóstico e histórico.
    private static DeliveryAttempt failureAttempt(
            Notification notification,
            int attemptNumber,
            String provider,
            String errorCode,
            String errorMessage
    ) {
        return DeliveryAttempt.failure(
                notification.getId(),
                notification.getChannel(),
                attemptNumber,
                provider,
                errorCode,
                errorMessage
        );
    }

    // Protege o comando contra payload nulo/vazio, retornando Map imutável para evitar mutações externas durante o processamento.
    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) return Map.of();
        return Map.copyOf(payload);
    }

    // Normaliza strings convertendo branco/blank em null e removendo espaços para manter consistência de persistência e comparação.
    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    // Extrai uma mensagem de erro segura para logs (fallback para nome da exceção quando message está ausente).
    private static String safeErrorMessage(Throwable ex) {
        if (ex == null) return "<unknown>";
        String msg = ex.getMessage();
        return (msg == null || msg.isBlank()) ? ex.getClass().getSimpleName() : msg;
    }

    // Mascara/normaliza destinos sensíveis para logs conforme o canal (email/telefone/token), evitando vazamento de PII.
    private static String safeDestination(String raw, NotificationChannel channel) {
        if (raw == null || raw.isBlank()) return "<empty>";

        return switch (channel) {
            case EMAIL -> {
                int at = raw.indexOf('@');
                if (at <= 1) yield "***";
                String domain = raw.substring(at + 1);
                yield raw.charAt(0) + "***@" + domain;
            }
            case WHATSAPP -> {
                String digits = raw.replaceAll("\\D+", "");
                if (digits.length() <= 6) yield "***";
                String prefix = digits.substring(0, Math.min(4, digits.length()));
                String suffix = digits.substring(digits.length() - 2);
                yield prefix + "****" + suffix;
            }
            case PUSH -> {
                if (raw.length() <= 8) yield "***";
                yield raw.substring(0, 4) + "..." + raw.substring(raw.length() - 4);
            }
        };
    }

    // Valida string obrigatória em tempo de execução (e.g., subject/body por canal) e lança BusinessValidationException com mensagem adequada.
    private static String requireNonNull(String v, String message) {
        if (v == null) throw new BusinessValidationException(message);
        return v;
    }

    // Converte a entidade de domínio Notification para DTO de saída (NotificationResult) usado por camadas externas (REST/queries).
    private static NotificationResult toResult(Notification n) {
        return new NotificationResult(
                n.getId().value(),
                n.getUserId().value(),
                n.getEventType(),
                n.getChannel(),
                n.getDestination().address(),
                n.getSubject(),
                n.getBody(),
                n.getStatus(),
                n.getAttemptCount(),
                n.getPayload(),
                n.getCreatedAt(),
                n.getUpdatedAt()
        );
    }

}
