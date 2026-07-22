package br.com.ecofy.ms_users.adapters.in.kafka;

import br.com.ecofy.ms_users.adapters.in.kafka.dto.AuthUserCreatedEventMessage;
import br.com.ecofy.ms_users.core.application.service.AuthUserSyncService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthUserCreatedEventConsumerTest {

    @Mock
    private AuthUserSyncService authUserSyncService;

    @Mock
    private br.com.ecofy.ms_users.core.port.out.IdempotencyPort idempotencyPort;

    private final io.micrometer.core.instrument.simple.SimpleMeterRegistry registry =
            new io.micrometer.core.instrument.simple.SimpleMeterRegistry();

    @AfterEach
    void cleanMdc() {
        MDC.clear();
    }

    // Configura o cenário padrão em que o evento chega pela primeira vez.
    private void eventIsNew() {
        lenient().when(idempotencyPort.registerOnce(any(), any(), any(), any()))
                .thenReturn(br.com.ecofy.ms_users.core.port.out.IdempotencyOutcome.REGISTERED);
    }

    private AuthUserCreatedEventMessage message(UUID userId) {
        return new AuthUserCreatedEventMessage(userId, "auth-1", "Full Name", "user@ecofy.com", "+5511999999999", null);
    }

    private AuthUserCreatedEventMessage messageWithTrace(String traceId) {
        return new AuthUserCreatedEventMessage(
                null, "auth-1", "Full Name", "user@ecofy.com", null,
                new br.com.ecofy.ms_users.adapters.in.kafka.dto.MessageMetadata(
                        "evt-1", java.time.Instant.parse("2026-07-16T18:00:00Z"), traceId, "ms-auth"));
    }

    @Test
    @DisplayName("ECO-05 §14.2: correlation ID do PAYLOAD entra no MDC durante o processamento")
    void correlationIdFromPayloadIsPutInMdc() {
        eventIsNew();
        var consumer = new AuthUserCreatedEventConsumer(authUserSyncService, idempotencyPort, registry);
        java.util.concurrent.atomic.AtomicReference<String> seen = new java.util.concurrent.atomic.AtomicReference<>();

        doAnswer(inv -> {
            seen.set(MDC.get("correlationId"));
            return null;
        }).when(authUserSyncService).onAuthUserCreated(any(), any(), any(), any(), any());

        consumer.consume(messageWithTrace("corr-do-payload"), "auth.user.registered", 0, 10L, 123L, null, "AUTH_USER_REGISTERED", "1");

        assertThat(seen.get()).isEqualTo("corr-do-payload");
        // §14.3: limpo depois — não vaza para o próximo evento na mesma thread.
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    @DisplayName("ECO-05 §14.2: payload é a fonte OFICIAL e vence o header Kafka divergente")
    void payloadWinsOverDivergentKafkaHeader() {
        eventIsNew();
        var consumer = new AuthUserCreatedEventConsumer(authUserSyncService, idempotencyPort, registry);
        java.util.concurrent.atomic.AtomicReference<String> seen = new java.util.concurrent.atomic.AtomicReference<>();

        doAnswer(inv -> {
            seen.set(MDC.get("correlationId"));
            return null;
        }).when(authUserSyncService).onAuthUserCreated(any(), any(), any(), any(), any());

        consumer.consume(messageWithTrace("corr-oficial"), "auth.user.registered", 0, 10L, 123L, "corr-do-header", "AUTH_USER_REGISTERED", "1");

        assertThat(seen.get()).isEqualTo("corr-oficial");
    }

    @Test
    @DisplayName("ECO-05: evento sem rastro -> gera um novo em vez de processar sem correlação")
    void generatesCorrelationIdWhenEventHasNone() {
        eventIsNew();
        var consumer = new AuthUserCreatedEventConsumer(authUserSyncService, idempotencyPort, registry);
        java.util.concurrent.atomic.AtomicReference<String> seen = new java.util.concurrent.atomic.AtomicReference<>();

        doAnswer(inv -> {
            seen.set(MDC.get("correlationId"));
            return null;
        }).when(authUserSyncService).onAuthUserCreated(any(), any(), any(), any(), any());

        consumer.consume(messageWithTrace(null), "auth.user.registered", 0, 10L, 123L, null, "AUTH_USER_REGISTERED", "1");

        assertThat(seen.get()).isNotBlank();
        assertThat(UUID.fromString(seen.get())).isNotNull();
    }

    @Test
    @DisplayName("MDC é limpo mesmo quando o processamento falha")
    void clearsMdcOnFailure() {
        eventIsNew();
        var consumer = new AuthUserCreatedEventConsumer(authUserSyncService, idempotencyPort, registry);
        doThrow(new RuntimeException("db down"))
                .when(authUserSyncService).onAuthUserCreated(any(), any(), any(), any(), any());

        assertThrows(RuntimeException.class, () ->
                consumer.consume(messageWithTrace("corr-x"), "auth.user.registered", 0, 10L, 123L, null, "AUTH_USER_REGISTERED", "1"));

        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void consume_shouldDelegateToSyncService() {
        eventIsNew();
        var consumer = new AuthUserCreatedEventConsumer(authUserSyncService, idempotencyPort, registry);
        UUID userId = UUID.randomUUID();

        consumer.consume(message(userId), "auth.user.registered", 0, 10L, 123L, null, "AUTH_USER_REGISTERED", "1");

        verify(authUserSyncService).onAuthUserCreated(
                userId, "auth-1", "Full Name", "user@ecofy.com", "+5511999999999");
    }

    // ---- ECO-01 §7.3/§7.4: contrato e idempotência ----

    @Test
    @DisplayName("§7.4: MESMO eventId reprocessado NÃO cria perfil de novo (replay)")
    void duplicateEventIdIsNotReprocessed() {
        var consumer = new AuthUserCreatedEventConsumer(authUserSyncService, idempotencyPort, registry);
        when(idempotencyPort.registerOnce(any(), any(), any(), any()))
                .thenReturn(br.com.ecofy.ms_users.core.port.out.IdempotencyOutcome.DUPLICATE);

        consumer.consume(messageWithTrace("corr-1"), "auth.user.registered", 0, 10L, 123L, null,
                "AUTH_USER_REGISTERED", "1");

        verifyNoInteractions(authUserSyncService);
        assertThat(registry.find("ecofy.users.event.duplicate").counter().count()).isEqualTo(1d);
    }

    @Test
    @DisplayName("§7.4: idempotência é registrada pelo eventId (chave do evento)")
    void idempotencyKeyIsTheEventId() {
        eventIsNew();
        var consumer = new AuthUserCreatedEventConsumer(authUserSyncService, idempotencyPort, registry);

        consumer.consume(messageWithTrace("corr-1"), "auth.user.registered", 0, 10L, 123L, null,
                "AUTH_USER_REGISTERED", "1");

        verify(idempotencyPort).registerOnce(
                eq("auth-user-registered-event"), eq("evt-1"), eq("auth-1"), any());
    }

    @Test
    @DisplayName("§7.3: versão NÃO suportada -> não processa, registra métrica e não estoura retry infinito")
    void unsupportedEventVersionIsRejected() {
        var consumer = new AuthUserCreatedEventConsumer(authUserSyncService, idempotencyPort, registry);

        consumer.consume(messageWithTrace("corr-1"), "auth.user.registered", 0, 10L, 123L, null,
                "AUTH_USER_REGISTERED", "99");

        verifyNoInteractions(authUserSyncService);
        // Sem DLT no projeto: não relança (evita retry infinito), mas nunca é silencioso.
        assertThat(registry.find("ecofy.users.event.unsupported_version").counter().count()).isEqualTo(1d);
    }

    @Test
    @DisplayName("§7.3: tipo de evento inesperado -> descartado com métrica")
    void unexpectedEventTypeIsRejected() {
        var consumer = new AuthUserCreatedEventConsumer(authUserSyncService, idempotencyPort, registry);

        consumer.consume(messageWithTrace("corr-1"), "auth.user.registered", 0, 10L, 123L, null,
                "SOME_OTHER_EVENT", "1");

        verifyNoInteractions(authUserSyncService);
        assertThat(registry.find("ecofy.users.event.invalid").counter().count()).isEqualTo(1d);
    }

    @Test
    @DisplayName("§7.3: versão ausente -> processa como v1 (compatibilidade) mas fica visível")
    void missingVersionIsTreatedAsV1AndRecorded() {
        eventIsNew();
        var consumer = new AuthUserCreatedEventConsumer(authUserSyncService, idempotencyPort, registry);

        consumer.consume(messageWithTrace("corr-1"), "auth.user.registered", 0, 10L, 123L, null,
                "AUTH_USER_REGISTERED", null);

        verify(authUserSyncService).onAuthUserCreated(any(), any(), any(), any(), any());
        assertThat(registry.find("ecofy.users.event.invalid").counter().count()).isEqualTo(1d);
    }

    @Test
    void consume_shouldRethrow_whenSyncFails_forRetryDlt() {
        var consumer = new AuthUserCreatedEventConsumer(authUserSyncService, idempotencyPort, registry);
        UUID userId = UUID.randomUUID();

        doThrow(new RuntimeException("db down"))
                .when(authUserSyncService)
                .onAuthUserCreated(any(), any(), any(), any(), any());

        assertThrows(RuntimeException.class,
                () -> consumer.consume(message(userId), "auth.user.registered", 0, 10L, 123L, null, "AUTH_USER_REGISTERED", "1"));
    }
}
