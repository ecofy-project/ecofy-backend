package br.com.ecofy.ms_notification.core.application.service;

import br.com.ecofy.ms_notification.core.application.config.NotificationSettings;
import br.com.ecofy.ms_notification.core.application.command.ResendNotificationCommand;
import br.com.ecofy.ms_notification.core.application.command.SendNotificationCommand;
import br.com.ecofy.ms_notification.core.application.result.NotificationResult;
import br.com.ecofy.ms_notification.core.domain.DeliveryAttempt;
import br.com.ecofy.ms_notification.core.domain.Notification;
import br.com.ecofy.ms_notification.core.domain.enums.AttemptStatus;
import br.com.ecofy.ms_notification.core.domain.enums.DeliveryErrorCategory;
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
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

// Orquestra a criação, o reenvio e a entrega de notificações multicanal.
@Slf4j
@Service
public class NotificationService implements SendNotificationUseCase, ResendNotificationUseCase {

    private static final String DEFAULT_PUSH_TITLE = "EcoFy";
    private static final String DELIVERY_ERROR_CODE = "DELIVERY_FAILED";
    private static final String FAILURE_PROVIDER_PLACEHOLDER = "provider";

    private final NotificationSettings settings;

    private final LoadNotificationTemplatePort loadTemplatePort;
    private final LoadUserContactInfoPort loadUserContactInfoPort;

    private final SaveNotificationPort saveNotificationPort;
    private final SaveDeliveryAttemptPort saveDeliveryAttemptPort;

    private final EmailSenderPort emailSenderPort;
    private final WhatsAppSenderPort whatsAppSenderPort;
    private final PushSenderPort pushSenderPort;

    private final IdempotencyPort idempotencyPort;
    private final RetryPolicyService retryPolicy;
    private final PublishNotificationEventPort publishNotificationEventPort;
    private final MeterRegistry meterRegistry;

    @org.springframework.beans.factory.annotation.Autowired
    public NotificationService(
            NotificationSettings settings,
            LoadNotificationTemplatePort loadTemplatePort,
            LoadUserContactInfoPort loadUserContactInfoPort,
            SaveNotificationPort saveNotificationPort,
            SaveDeliveryAttemptPort saveDeliveryAttemptPort,
            EmailSenderPort emailSenderPort,
            WhatsAppSenderPort whatsAppSenderPort,
            PushSenderPort pushSenderPort,
            IdempotencyPort idempotencyPort,
            RetryPolicyService retryPolicy,
            Optional<PublishNotificationEventPort> publishNotificationEventPort,
            MeterRegistry meterRegistry
    ) {
        this.settings = Objects.requireNonNull(settings, "settings must not be null");
        this.loadTemplatePort = Objects.requireNonNull(loadTemplatePort, "loadTemplatePort must not be null");
        this.loadUserContactInfoPort = Objects.requireNonNull(
                loadUserContactInfoPort,
                "loadUserContactInfoPort must not be null"
        );
        this.saveNotificationPort = Objects.requireNonNull(
                saveNotificationPort,
                "saveNotificationPort must not be null"
        );
        this.saveDeliveryAttemptPort = Objects.requireNonNull(
                saveDeliveryAttemptPort,
                "saveDeliveryAttemptPort must not be null"
        );
        this.emailSenderPort = Objects.requireNonNull(emailSenderPort, "emailSenderPort must not be null");
        this.whatsAppSenderPort = Objects.requireNonNull(
                whatsAppSenderPort,
                "whatsAppSenderPort must not be null"
        );
        this.pushSenderPort = Objects.requireNonNull(pushSenderPort, "pushSenderPort must not be null");
        this.idempotencyPort = Objects.requireNonNull(idempotencyPort, "idempotencyPort must not be null");
        this.retryPolicy = Objects.requireNonNull(retryPolicy, "retryPolicy must not be null");
        this.publishNotificationEventPort = publishNotificationEventPort.orElse(null);
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");
    }

    public NotificationService(
            NotificationSettings settings,
            LoadNotificationTemplatePort loadTemplatePort,
            LoadUserContactInfoPort loadUserContactInfoPort,
            SaveNotificationPort saveNotificationPort,
            SaveDeliveryAttemptPort saveDeliveryAttemptPort,
            EmailSenderPort emailSenderPort,
            WhatsAppSenderPort whatsAppSenderPort,
            PushSenderPort pushSenderPort,
            IdempotencyPort idempotencyPort,
            RetryPolicyService retryPolicy,
            Optional<PublishNotificationEventPort> publishNotificationEventPort
    ) {
        this(
                settings,
                loadTemplatePort,
                loadUserContactInfoPort,
                saveNotificationPort,
                saveDeliveryAttemptPort,
                emailSenderPort,
                whatsAppSenderPort,
                pushSenderPort,
                idempotencyPort,
                retryPolicy,
                publishNotificationEventPort,
                new SimpleMeterRegistry()
        );
    }

    // Coordena a criação, persistência e entrega idempotente da notificação.
    @Override
    public NotificationResult send(SendNotificationCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        UUID userIdRaw = Objects.requireNonNull(
                command.userId(),
                "command.userId must not be null"
        );
        DomainEventType eventType = Objects.requireNonNull(
                command.eventType(),
                "command.eventType must not be null"
        );
        NotificationChannel channel = Objects.requireNonNull(
                command.channel(),
                "command.channel must not be null"
        );

        String idem = resolveIdempotencyKey(command.idempotencyKey(), "send");
        acquireIdempotencyIfEnabled(idem);

        var userId = new UserId(userIdRaw);
        Map<String, Object> payload = safePayload(command.payload());

        log.info(
                "[NotificationService] - [send] -> Iniciando envio userId={} eventType={} channel={} hasOverrideDest={} hasPayload={} idempotencyEnabled={} hasIdempotencyKey={}",
                userIdRaw,
                eventType,
                channel,
                command.destinationOverride() != null
                        && !command.destinationOverride().isBlank(),
                !payload.isEmpty(),
                settings.idempotencyEnabled(),
                idem != null
        );

        var template = loadTemplatePort
                .loadActiveTemplate(userId, eventType, channel)
                .orElseThrow(() -> new TemplateNotFoundException(
                        "No active template found for eventType=%s channel=%s"
                                .formatted(eventType, channel)
                ));

        String destination = resolveDestination(
                command.destinationOverride(),
                channel,
                userId
        );
        var dest = new ChannelAddress(channel, destination);

        String subject = channel == NotificationChannel.EMAIL
                ? template.renderSubject(payload)
                : null;
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

        meterRegistry.counter(
                "ecofy.notification.created.total",
                "channel",
                channel.name(),
                "event_type",
                eventType.name()
        ).increment();

        log.debug(
                "[NotificationService] - [send] -> persisted notificationId={} status={} attemptCount={}",
                saved.getId().value(),
                saved.getStatus(),
                saved.getAttemptCount()
        );

        Notification delivered = deliverWithRetry(saved);

        log.info(
                "[NotificationService] - [send] -> Concluído notificationId={} status={} attemptCount={}",
                delivered.getId().value(),
                delivered.getStatus(),
                delivered.getAttemptCount()
        );

        return toResult(delivered);
    }

    // Reinicia a entrega de uma notificação existente com proteção idempotente.
    @Override
    public NotificationResult resend(ResendNotificationCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        UUID notificationUuid = Objects.requireNonNull(
                command.notificationId(),
                "command.notificationId must not be null"
        );
        var notificationId = new NotificationId(notificationUuid);

        String idem = resolveIdempotencyKey(command.idempotencyKey(), "resend");
        acquireIdempotencyIfEnabled(idem);

        log.info(
                "[NotificationService] - [resend] -> Iniciando reenvio notificationId={} idempotencyEnabled={} hasIdempotencyKey={}",
                notificationUuid,
                settings.idempotencyEnabled(),
                idem != null
        );

        var current = saveNotificationPort.loadById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(
                        "Notification not found for id: " + notificationUuid
                ));

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

        Notification delivered = deliverWithRetry(saved);

        log.info(
                "[NotificationService] - [resend] -> Concluído notificationId={} status={} attemptCount={}",
                delivered.getId().value(),
                delivered.getStatus(),
                delivered.getAttemptCount()
        );

        return toResult(delivered);
    }

    // Controla novas tentativas conforme a categoria da falha e a política configurada.
    private Notification deliverWithRetry(Notification notification) {
        Objects.requireNonNull(notification, "notification must not be null");

        Notification current = notification;
        String correlationId = MDC.get("correlationId");

        while (true) {
            int attemptNumber = current.getAttemptCount() + 1;

            try {
                return attemptDeliveryOnce(current, attemptNumber);

            } catch (BusinessValidationException ex) {
                boolean invalidDest = safeErrorMessage(ex)
                        .toLowerCase()
                        .contains("email")
                        || safeErrorMessage(ex)
                        .toLowerCase()
                        .contains("phone")
                        || safeErrorMessage(ex)
                        .toLowerCase()
                        .contains("device token");

                DeliveryErrorCategory cat = invalidDest
                        ? DeliveryErrorCategory.INVALID_DESTINATION
                        : DeliveryErrorCategory.PERMANENT_FAILURE;

                recordFailureAttempt(
                        current,
                        attemptNumber,
                        FAILURE_PROVIDER_PLACEHOLDER,
                        cat,
                        "VALIDATION",
                        safeErrorMessage(ex),
                        null,
                        null,
                        correlationId
                );

                current = markNotificationFailed(current, attemptNumber);

                meterDeliveryOutcome(
                        "failed",
                        current.getChannel().name(),
                        FAILURE_PROVIDER_PLACEHOLDER,
                        cat.name()
                );

                throw ex;

            } catch (ProviderDeliveryException ex) {
                DeliveryErrorCategory cat = ex.getCategory();
                boolean willRetry =
                        cat.retryable() && retryPolicy.canRetry(attemptNumber);
                Duration backoff = willRetry
                        ? resolveBackoff(ex, attemptNumber)
                        : null;
                Instant nextRetryAt = backoff != null
                        ? Instant.now().plus(backoff)
                        : null;

                recordFailureAttempt(
                        current,
                        attemptNumber,
                        ex.getErrorCode(),
                        cat,
                        ex.getErrorCode(),
                        safeErrorMessage(ex),
                        ex.getProviderStatusCode(),
                        nextRetryAt,
                        correlationId
                );

                current = markNotificationFailed(current, attemptNumber);

                if (willRetry) {
                    meterRegistry.counter(
                            "ecofy.notification.retry.total",
                            "channel",
                            current.getChannel().name(),
                            "reason",
                            cat.name()
                    ).increment();

                    log.info(
                            "[NotificationService] - [deliverWithRetry] -> retry scheduled notificationId={} attempt={} category={} backoffMs={}",
                            current.getId().value(),
                            attemptNumber,
                            cat,
                            backoff.toMillis()
                    );

                    sleepBackoff(backoff);
                    continue;
                }

                meterDeliveryOutcome(
                        "failed",
                        current.getChannel().name(),
                        safeProvider(ex.getErrorCode()),
                        cat.name()
                );

                log.error(
                        "[NotificationService] - [deliverWithRetry] -> delivery failed permanently notificationId={} attempts={} category={}",
                        current.getId().value(),
                        attemptNumber,
                        cat
                );

                throw new DeliveryProviderException(
                        "Delivery failed for notificationId="
                                + current.getId().value(),
                        ex
                );

            } catch (RuntimeException ex) {
                recordFailureAttempt(
                        current,
                        attemptNumber,
                        FAILURE_PROVIDER_PLACEHOLDER,
                        DeliveryErrorCategory.TEMPORARY_FAILURE,
                        DELIVERY_ERROR_CODE,
                        safeErrorMessage(ex),
                        null,
                        null,
                        correlationId
                );

                current = markNotificationFailed(current, attemptNumber);

                if (retryPolicy.canRetry(attemptNumber)) {
                    Duration backoff =
                            retryPolicy.computeBackoff(attemptNumber);

                    meterRegistry.counter(
                            "ecofy.notification.retry.total",
                            "channel",
                            current.getChannel().name(),
                            "reason",
                            "TEMPORARY_FAILURE"
                    ).increment();

                    log.info(
                            "[NotificationService] - [deliverWithRetry] -> retry (unknown error) notificationId={} attempt={} backoffMs={}",
                            current.getId().value(),
                            attemptNumber,
                            backoff.toMillis()
                    );

                    sleepBackoff(backoff);
                    continue;
                }

                meterDeliveryOutcome(
                        "failed",
                        current.getChannel().name(),
                        FAILURE_PROVIDER_PLACEHOLDER,
                        "TEMPORARY_FAILURE"
                );

                throw new DeliveryProviderException(
                        "Delivery failed for notificationId="
                                + current.getId().value(),
                        ex
                );
            }
        }
    }

    // Aguarda o intervalo definido antes de executar uma nova tentativa.
    private void sleepBackoff(Duration backoff) {
        if (backoff == null || backoff.isZero() || backoff.isNegative()) {
            return;
        }

        try {
            Thread.sleep(backoff.toMillis());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new DeliveryProviderException(
                    "Interrupted while waiting to retry delivery",
                    ie
            );
        }
    }

    // Resolve o maior intervalo entre a política interna e a orientação do provedor.
    private Duration resolveBackoff(
            ProviderDeliveryException ex,
            int attemptNumber
    ) {
        Duration computed = retryPolicy.computeBackoff(attemptNumber);
        Duration retryAfter = ex.getRetryAfter();

        if (retryAfter != null && retryAfter.compareTo(computed) > 0) {
            return retryAfter;
        }

        return computed;
    }

    private Notification markNotificationFailed(
            Notification current,
            int attemptNumber
    ) {
        return saveNotificationPort.save(current.toBuilder()
                .status(NotificationStatus.FAILED)
                .attemptCount(attemptNumber)
                .updatedAt(Instant.now())
                .build());
    }

    private void recordFailureAttempt(
            Notification notification,
            int attemptNumber,
            String provider,
            DeliveryErrorCategory category,
            String errorCode,
            String errorMessage,
            Integer providerStatusCode,
            Instant nextRetryAt,
            String correlationId
    ) {
        AttemptStatus status = switch (category) {
            case INVALID_DESTINATION -> AttemptStatus.INVALID_DESTINATION;
            case PROVIDER_BLOCKED -> AttemptStatus.PROVIDER_BLOCKED;
            default -> nextRetryAt != null
                    ? AttemptStatus.RETRY_SCHEDULED
                    : AttemptStatus.FAILED;
        };

        saveDeliveryAttemptPort.save(DeliveryAttempt.classifiedFailure(
                notification.getId(),
                notification.getChannel(),
                attemptNumber,
                safeProvider(provider),
                status,
                category.name(),
                errorCode,
                errorMessage,
                providerStatusCode,
                nextRetryAt,
                correlationId
        ));

        meterRegistry.counter(
                "ecofy.notification.delivery.attempt.total",
                "channel",
                notification.getChannel().name(),
                "outcome",
                "failure",
                "reason",
                category.name()
        ).increment();
    }

    private void meterDeliveryOutcome(
            String outcome,
            String channel,
            String provider,
            String reason
    ) {
        meterRegistry.counter(
                "ecofy.notification.delivery.total",
                "channel",
                channel,
                "provider",
                provider,
                "outcome",
                outcome,
                "reason",
                reason
        ).increment();
    }

    private static String safeProvider(String provider) {
        return provider == null || provider.isBlank()
                ? FAILURE_PROVIDER_PLACEHOLDER
                : provider;
    }

    // Executa uma tentativa de entrega e registra o resultado bem-sucedido.
    private Notification attemptDeliveryOnce(
            Notification notification,
            int attemptNumber
    ) {
        log.debug(
                "[NotificationService] - [attemptDeliveryOnce] -> Tentando entrega notificationId={} channel={} attemptNumber={}",
                notification.getId().value(),
                notification.getChannel(),
                attemptNumber
        );

        switch (notification.getChannel()) {
            case EMAIL -> {
                var result = emailSenderPort.sendEmail(
                        notification.getDestination(),
                        requireNonNull(
                                notification.getSubject(),
                                "email subject must not be null"
                        ),
                        requireNonNull(
                                notification.getBody(),
                                "email body must not be null"
                        )
                );

                saveDeliveryAttemptPort.save(successAttempt(
                        notification,
                        attemptNumber,
                        result.provider(),
                        result.providerMessageId()
                ));
            }
            case WHATSAPP -> {
                var result = whatsAppSenderPort.sendWhatsApp(
                        notification.getDestination(),
                        requireNonNull(
                                notification.getBody(),
                                "whatsapp body must not be null"
                        )
                );

                saveDeliveryAttemptPort.save(successAttempt(
                        notification,
                        attemptNumber,
                        result.provider(),
                        result.providerMessageId()
                ));
            }
            case PUSH -> {
                String title = notification.getSubject() == null
                        || notification.getSubject().isBlank()
                        ? DEFAULT_PUSH_TITLE
                        : notification.getSubject();

                var result = pushSenderPort.sendPush(
                        notification.getDestination(),
                        title,
                        requireNonNull(
                                notification.getBody(),
                                "push body must not be null"
                        )
                );

                saveDeliveryAttemptPort.save(successAttempt(
                        notification,
                        attemptNumber,
                        result.provider(),
                        result.providerMessageId()
                ));
            }
            default -> throw new BusinessValidationException(
                    "Unsupported channel: " + notification.getChannel()
            );
        }

        Notification saved = saveNotificationPort.save(
                notification.toBuilder()
                        .status(NotificationStatus.SENT)
                        .attemptCount(attemptNumber)
                        .updatedAt(Instant.now())
                        .build()
        );

        meterDeliveryOutcome(
                "delivered",
                saved.getChannel().name(),
                "provider",
                "none"
        );

        meterRegistry.counter(
                "ecofy.notification.delivery.attempt.total",
                "channel",
                saved.getChannel().name(),
                "outcome",
                "success",
                "reason",
                "none"
        ).increment();

        log.debug(
                "[NotificationService] - [attemptDeliveryOnce] -> delivery success notificationId={} channel={} attemptNumber={} status={}",
                saved.getId().value(),
                saved.getChannel(),
                attemptNumber,
                saved.getStatus()
        );

        publishSentEventIfEnabled(saved);
        return saved;
    }

    // Publica o evento de envio sem interromper o fluxo em caso de falha.
    private void publishSentEventIfEnabled(Notification saved) {
        if (publishNotificationEventPort == null) {
            return;
        }

        try {
            publishNotificationEventPort.publish(saved);

            log.debug(
                    "[NotificationService] - [publishSentEventIfEnabled] -> published notification.sent notificationId={}",
                    saved.getId().value()
            );
        } catch (Exception ex) {
            log.error(
                    "[NotificationService] - [publishSentEventIfEnabled] -> Falha ao publicar notification.sent notificationId={}",
                    saved.getId().value(),
                    ex
            );
        }
    }

    // Resolve o destino pelo valor informado ou pelos contatos cadastrados do usuário.
    private String resolveDestination(
            String override,
            NotificationChannel channel,
            UserId userId
    ) {
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
                .orElseThrow(() -> new BusinessValidationException(
                        "User contact info not found for userId="
                                + userId.value()
                ));

        return switch (channel) {
            case EMAIL -> {
                if (contact.email() == null || contact.email().isBlank()) {
                    throw new BusinessValidationException(
                            "User has no email"
                    );
                }
                yield contact.email().trim();
            }
            case WHATSAPP -> {
                if (contact.phoneE164() == null
                        || contact.phoneE164().isBlank()) {
                    throw new BusinessValidationException(
                            "User has no phone"
                    );
                }
                yield contact.phoneE164().trim();
            }
            case PUSH -> {
                if (contact.deviceToken() == null
                        || contact.deviceToken().isBlank()) {
                    throw new BusinessValidationException(
                            "User has no device token"
                    );
                }
                yield contact.deviceToken().trim();
            }
        };
    }

    // Registra a chave de idempotência e rejeita operações duplicadas.
    private void acquireIdempotencyIfEnabled(String idempotencyKey) {
        if (!settings.idempotencyEnabled()) {
            return;
        }

        String keyValue = blankToNull(idempotencyKey);
        if (keyValue == null) {
            keyValue = UUID.randomUUID().toString();
        }

        var key = new IdempotencyKey(keyValue);

        boolean ok = idempotencyPort.tryAcquire(key);
        if (!ok) {
            log.warn(
                    "[NotificationService] - [acquireIdempotencyIfEnabled] -> idempotency violation key={}",
                    key.value()
            );

            throw new IdempotencyViolationException(
                    "Idempotency key already used: " + key.value()
            );
        }

        log.debug(
                "[NotificationService] - [acquireIdempotencyIfEnabled] -> Chave de idempotência adquirida key={}",
                key.value()
        );
    }

    // Resolve a chave de idempotência específica para cada operação.
    private String resolveIdempotencyKey(String provided, String suffix) {
        String base = provided == null || provided.isBlank()
                ? UUID.randomUUID().toString()
                : provided.trim();

        return base + ":" + suffix;
    }

    private static DeliveryAttempt successAttempt(
            Notification notification,
            int attemptNumber,
            String provider,
            String providerMessageId
    ) {
        return DeliveryAttempt.success(
                notification.getId(),
                notification.getChannel(),
                attemptNumber,
                provider,
                providerMessageId
        );
    }

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

    // Protege o payload contra valores nulos e mutações externas.
    private static Map<String, Object> safePayload(
            Map<String, Object> payload
    ) {
        if (payload == null || payload.isEmpty()) {
            return Map.of();
        }
        return Map.copyOf(payload);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }

    // Resolve uma mensagem segura para registro de falhas.
    private static String safeErrorMessage(Throwable ex) {
        if (ex == null) {
            return "<unknown>";
        }

        String message = ex.getMessage();
        return message == null || message.isBlank()
                ? ex.getClass().getSimpleName()
                : message;
    }

    // Mascara destinos sensíveis antes de registrá-los nos logs.
    private static String safeDestination(
            String raw,
            NotificationChannel channel
    ) {
        if (raw == null || raw.isBlank()) {
            return "<empty>";
        }

        return switch (channel) {
            case EMAIL -> {
                int at = raw.indexOf('@');
                if (at <= 1) {
                    yield "***";
                }
                String domain = raw.substring(at + 1);
                yield raw.charAt(0) + "***@" + domain;
            }
            case WHATSAPP -> {
                String digits = raw.replaceAll("\\D+", "");
                if (digits.length() <= 6) {
                    yield "***";
                }
                String prefix = digits.substring(
                        0,
                        Math.min(4, digits.length())
                );
                String suffix = digits.substring(digits.length() - 2);
                yield prefix + "****" + suffix;
            }
            case PUSH -> {
                if (raw.length() <= 8) {
                    yield "***";
                }
                yield raw.substring(0, 4)
                        + "..."
                        + raw.substring(raw.length() - 4);
            }
        };
    }

    // Valida os campos obrigatórios usados na entrega por canal.
    private static String requireNonNull(String value, String message) {
        if (value == null) {
            throw new BusinessValidationException(message);
        }
        return value;
    }

    private static NotificationResult toResult(Notification notification) {
        return new NotificationResult(
                notification.getId().value(),
                notification.getUserId().value(),
                notification.getEventType(),
                notification.getChannel(),
                notification.getDestination().address(),
                notification.getSubject(),
                notification.getBody(),
                notification.getStatus(),
                notification.getAttemptCount(),
                notification.getPayload(),
                notification.getCreatedAt(),
                notification.getUpdatedAt()
        );
    }
}
