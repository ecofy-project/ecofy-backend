package br.com.ecofy.ms_users.adapters.in.kafka;

import br.com.ecofy.ms_users.adapters.in.kafka.dto.AuthUserCreatedEventMessage;
import br.com.ecofy.ms_users.adapters.in.web.correlation.CorrelationId;
import br.com.ecofy.ms_users.core.application.service.AuthUserSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Objects;

// Processa eventos de cadastro e sincroniza usuários com idempotência e rastreabilidade.
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthUserCreatedEventConsumer {

    private static final String EXPECTED_EVENT_TYPE =
            "AUTH_USER_REGISTERED";
    private static final java.util.Set<Integer> SUPPORTED_EVENT_VERSIONS =
            java.util.Set.of(1);

    private static final String IDEMPOTENCY_OPERATION =
            "auth-user-registered-event";
    private static final java.time.Duration IDEMPOTENCY_TTL =
            java.time.Duration.ofDays(7);

    private final AuthUserSyncService authUserSyncService;
    private final br.com.ecofy.ms_users.core.port.out.IdempotencyPort
            idempotencyPort;
    private final io.micrometer.core.instrument.MeterRegistry meterRegistry;

    // Consome eventos compatíveis e delega a sincronização do usuário.
    @KafkaListener(
            id = "authUserCreatedEventConsumer",
            topics = "${ecofy.users.topics.auth-user-created:auth.user.registered}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            AuthUserCreatedEventMessage msg,
            @Header(
                    name = KafkaHeaders.RECEIVED_TOPIC,
                    required = false
            )
            String topic,
            @Header(
                    name = KafkaHeaders.RECEIVED_PARTITION,
                    required = false
            )
            Integer partition,
            @Header(
                    name = KafkaHeaders.OFFSET,
                    required = false
            )
            Long offset,
            @Header(
                    name = KafkaHeaders.RECEIVED_TIMESTAMP,
                    required = false
            )
            Long timestamp,
            @Header(
                    name = CorrelationId.HEADER,
                    required = false
            )
            String correlationIdHeader,
            @Header(name = "eventType", required = false)
            String eventType,
            @Header(name = "eventVersion", required = false)
            String eventVersion
    ) {
        Objects.requireNonNull(msg, "msg must not be null");

        String fromPayload = msg.metadata() != null
                ? msg.metadata().traceId()
                : null;

        if (fromPayload != null
                && correlationIdHeader != null
                && !fromPayload.equals(correlationIdHeader)) {
            log.warn(
                    "[AuthUserCreatedEventConsumer] correlationId divergente entre payload e header Kafka"
                            + " — prevalece o payload (fonte oficial) topic={}",
                    v(topic)
            );
        }

        String source = fromPayload != null
                ? fromPayload
                : correlationIdHeader;
        String correlationId = CorrelationId.sanitizeOrGenerate(source);
        MDC.put(CorrelationId.MDC_KEY, correlationId);

        final long startNs = System.nanoTime();

        log.info(
                "[AuthUserCreatedEventConsumer] consume status=received topic={} partition={} offset={} ts={} userId={} extAuthId={} hasEmail={} hasPhone={} hasFullName={}",
                v(topic),
                v(partition),
                v(offset),
                v(timestamp),
                v(msg.userId()),
                v(msg.externalAuthId()),
                hasText(msg.email()),
                hasText(msg.phone()),
                hasText(msg.fullName())
        );

        try {
            if (!isContractSupported(eventType, eventVersion, topic)) {
                return;
            }

            if (isDuplicate(msg, eventType, eventVersion)) {
                return;
            }

            meterRegistry.counter(
                    "ecofy.users.event.received",
                    "event_type",
                    EXPECTED_EVENT_TYPE,
                    "outcome",
                    "accepted"
            ).increment();

            authUserSyncService.onAuthUserCreated(
                    msg.userId(),
                    msg.externalAuthId(),
                    msg.fullName(),
                    msg.email(),
                    msg.phone()
            );

            final long elapsedMs =
                    (System.nanoTime() - startNs) / 1_000_000;

            log.info(
                    "[AuthUserCreatedEventConsumer] consume status=processed topic={} partition={} offset={} userId={} extAuthId={} elapsedMs={}",
                    v(topic),
                    v(partition),
                    v(offset),
                    v(msg.userId()),
                    v(msg.externalAuthId()),
                    elapsedMs
            );

        } catch (RuntimeException ex) {
            final long elapsedMs =
                    (System.nanoTime() - startNs) / 1_000_000;

            log.error(
                    "[AuthUserCreatedEventConsumer] consume status=failed topic={} partition={} offset={} userId={} extAuthId={} elapsedMs={}",
                    v(topic),
                    v(partition),
                    v(offset),
                    v(msg.userId()),
                    v(msg.externalAuthId()),
                    elapsedMs,
                    ex
            );

            throw ex;

        } catch (Exception ex) {
            final long elapsedMs =
                    (System.nanoTime() - startNs) / 1_000_000;

            log.error(
                    "[AuthUserCreatedEventConsumer] consume status=failed topic={} partition={} offset={} userId={} extAuthId={} elapsedMs={}",
                    v(topic),
                    v(partition),
                    v(offset),
                    v(msg.userId()),
                    v(msg.externalAuthId()),
                    elapsedMs,
                    ex
            );

            throw new RuntimeException(ex);

        } finally {
            MDC.remove(CorrelationId.MDC_KEY);
        }
    }

    // Valida o tipo e a versão do evento antes do processamento.
    private boolean isContractSupported(
            String eventType,
            String eventVersion,
            String topic
    ) {
        if (eventType != null
                && !EXPECTED_EVENT_TYPE.equals(eventType)) {
            meterRegistry.counter(
                    "ecofy.users.event.invalid",
                    "event_type",
                    "unexpected",
                    "reason",
                    "event_type"
            ).increment();

            log.error(
                    "[AuthUserCreatedEventConsumer] evento com tipo inesperado topic={} — descartado",
                    v(topic)
            );

            return false;
        }

        Integer version = parseVersion(eventVersion);

        if (version == null) {
            meterRegistry.counter(
                    "ecofy.users.event.invalid",
                    "event_type",
                    EXPECTED_EVENT_TYPE,
                    "reason",
                    "missing_version"
            ).increment();

            log.warn(
                    "[AuthUserCreatedEventConsumer] evento sem eventVersion topic={} — assumindo v1",
                    v(topic)
            );

            return true;
        }

        if (!SUPPORTED_EVENT_VERSIONS.contains(version)) {
            meterRegistry.counter(
                    "ecofy.users.event.unsupported_version",
                    "event_type",
                    EXPECTED_EVENT_TYPE,
                    "event_version",
                    String.valueOf(version)
            ).increment();

            log.error(
                    "[AuthUserCreatedEventConsumer] eventVersion não suportada version={} suportadas={} topic={}"
                            + " — descartado (sem DLT no projeto; evita retry infinito)",
                    version,
                    SUPPORTED_EVENT_VERSIONS,
                    v(topic)
            );

            return false;
        }

        return true;
    }

    // Detecta eventos já registrados antes de sincronizar o usuário.
    private boolean isDuplicate(
            AuthUserCreatedEventMessage msg,
            String eventType,
            String eventVersion
    ) {
        String eventId = msg.metadata() != null
                ? msg.metadata().eventId()
                : null;

        if (eventId == null || eventId.isBlank()) {
            meterRegistry.counter(
                    "ecofy.users.event.invalid",
                    "event_type",
                    EXPECTED_EVENT_TYPE,
                    "reason",
                    "missing_event_id"
            ).increment();

            log.warn(
                    "[AuthUserCreatedEventConsumer] evento sem eventId — sem dedup por evento"
            );

            return false;
        }

        var outcome = idempotencyPort.registerOnce(
                IDEMPOTENCY_OPERATION,
                eventId,
                msg.externalAuthId(),
                IDEMPOTENCY_TTL
        );

        if (outcome
                == br.com.ecofy.ms_users.core.port.out.IdempotencyOutcome.REGISTERED) {
            return false;
        }

        meterRegistry.counter(
                "ecofy.users.event.duplicate",
                "event_type",
                EXPECTED_EVENT_TYPE,
                "outcome",
                outcome.name().toLowerCase()
        ).increment();

        log.info(
                "[AuthUserCreatedEventConsumer] evento já processado (outcome={}) — ignorado",
                outcome
        );

        return true;
    }

    private static Integer parseVersion(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return Integer.valueOf(raw.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    // Normaliza valores ausentes para o padrão utilizado nos logs.
    private static String v(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }
}
