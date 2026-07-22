package br.com.ecofy.auth.adapters.out.messaging;

import br.com.ecofy.auth.adapters.in.web.correlation.CorrelationId;
import br.com.ecofy.auth.adapters.out.messaging.dto.AuthUserRegisteredMessage;
import br.com.ecofy.auth.core.domain.AuthUser;
import br.com.ecofy.auth.core.domain.event.PasswordResetRequestedEvent;
import br.com.ecofy.auth.core.domain.event.UserAuthenticatedEvent;
import br.com.ecofy.auth.core.domain.event.UserEmailConfirmedEvent;
import br.com.ecofy.auth.core.domain.event.UserRegisteredEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Testes unitários do adaptador Kafka de eventos de autenticação")
class AuthEventsKafkaAdapterTest {

    private static final String DEFAULT_TOPIC_USER_REGISTERED =
            "auth.user.registered";
    private static final String CUSTOM_TOPIC_USER_REGISTERED =
            "custom.auth.user.registered";
    private static final String TOPIC_USER_EMAIL_CONFIRMED =
            "auth.user.email-confirmed";
    private static final String TOPIC_USER_AUTHENTICATED =
            "auth.user.authenticated";
    private static final String TOPIC_PASSWORD_RESET_REQUESTED =
            "auth.user.password-reset-requested";

    private static final String EVENT_TYPE_USER_REGISTERED =
            "AUTH_USER_REGISTERED";
    private static final String CORRELATION_ID = "correlation-id-123";
    private static final String GENERATED_CORRELATION_ID =
            "generated-correlation-id-456";
    private static final String FULL_NAME = "Usuário EcoFy";
    private static final String EMAIL = "usuario@ecofy.com";
    private static final String PRODUCER = "ms-auth";

    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
    }

    @Test
    @DisplayName("Deve rejeitar KafkaTemplate nulo ao construir o adaptador")
    void constructor_kafkaTemplateNulo_deveLancarNullPointerException() {
        // Arrange
        KafkaTemplate<String, Object> nullKafkaTemplate = null;

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new AuthEventsKafkaAdapter(
                        nullKafkaTemplate,
                        CUSTOM_TOPIC_USER_REGISTERED
                )
        );

        // Assert
        assertEquals(
                "kafkaTemplate must not be null",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Deve utilizar o tópico configurado ao publicar o registro do usuário")
    void publish_topicoConfiguradoECorrelationIdAtual_deveEnviarRegistroComHeaders()
            throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-07-20T15:00:00Z");
        UserRegisteredEvent event = mockRegisteredEvent(
                userId,
                occurredAt
        );

        SendResult<String, Object> sendResult = successfulSendResult();
        CompletableFuture<SendResult<String, Object>> future =
                CompletableFuture.completedFuture(sendResult);

        doReturn(future)
                .when(kafkaTemplate)
                .send(any(ProducerRecord.class));

        AuthEventsKafkaAdapter adapter = new AuthEventsKafkaAdapter(
                kafkaTemplate,
                CUSTOM_TOPIC_USER_REGISTERED
        );

        try (MockedStatic<CorrelationId> correlationId =
                     mockCorrelationId(CORRELATION_ID, null)) {

            // Act
            adapter.publish(event);

            // Assert
            ArgumentCaptor<ProducerRecord<String, Object>> captor =
                    producerRecordCaptor();

            verify(kafkaTemplate).send(captor.capture());

            ProducerRecord<String, Object> record = captor.getValue();
            AuthUserRegisteredMessage message = assertInstanceOf(
                    AuthUserRegisteredMessage.class,
                    record.value()
            );

            String eventId = headerValue(record, "eventId");

            assertAll(
                    () -> assertEquals(
                            CUSTOM_TOPIC_USER_REGISTERED,
                            record.topic()
                    ),
                    () -> assertEquals(
                            userId.toString(),
                            record.key()
                    ),
                    () -> assertEquals(
                            CORRELATION_ID,
                            headerValue(record, CorrelationId.HEADER)
                    ),
                    () -> assertEquals(
                            EVENT_TYPE_USER_REGISTERED,
                            headerValue(record, "eventType")
                    ),
                    () -> assertEquals(
                            "1",
                            headerValue(record, "eventVersion")
                    ),
                    () -> assertDoesNotThrow(
                            () -> UUID.fromString(eventId)
                    ),
                    () -> assertMessageValues(
                            message,
                            userId,
                            occurredAt,
                            eventId,
                            CORRELATION_ID
                    )
            );

            correlationId.verify(CorrelationId::current);
            correlationId.verify(
                    CorrelationId::generate,
                    never()
            );
        }
    }

    @Test
    @DisplayName("Deve utilizar o tópico padrão e gerar correlation ID quando a configuração e o contexto forem nulos")
    void publish_topicoNuloECorrelationIdNulo_deveUsarValoresPadrao()
            throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserRegisteredEvent event = mockRegisteredEvent(
                userId,
                Instant.parse("2026-07-20T15:10:00Z")
        );

        RuntimeException sendFailure =
                new RuntimeException("Kafka indisponível");
        CompletableFuture<SendResult<String, Object>> future =
                CompletableFuture.failedFuture(sendFailure);

        doReturn(future)
                .when(kafkaTemplate)
                .send(any(ProducerRecord.class));

        AuthEventsKafkaAdapter adapter = new AuthEventsKafkaAdapter(
                kafkaTemplate,
                null
        );

        try (MockedStatic<CorrelationId> correlationId =
                     mockCorrelationId(null, GENERATED_CORRELATION_ID)) {

            // Act
            assertDoesNotThrow(() -> adapter.publish(event));

            // Assert
            ArgumentCaptor<ProducerRecord<String, Object>> captor =
                    producerRecordCaptor();

            verify(kafkaTemplate).send(captor.capture());

            ProducerRecord<String, Object> record = captor.getValue();

            assertAll(
                    () -> assertEquals(
                            DEFAULT_TOPIC_USER_REGISTERED,
                            record.topic()
                    ),
                    () -> assertEquals(
                            userId.toString(),
                            record.key()
                    ),
                    () -> assertEquals(
                            GENERATED_CORRELATION_ID,
                            headerValue(record, CorrelationId.HEADER)
                    ),
                    () -> assertTrue(future.isCompletedExceptionally())
            );

            correlationId.verify(CorrelationId::current);
            correlationId.verify(CorrelationId::generate);
        }
    }

    @Test
    @DisplayName("Deve utilizar o tópico padrão e gerar correlation ID quando os valores estiverem em branco")
    void publish_topicoEmBrancoECorrelationIdEmBranco_deveUsarValoresPadrao()
            throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserRegisteredEvent event = mockRegisteredEvent(
                userId,
                Instant.parse("2026-07-20T15:20:00Z")
        );

        SendResult<String, Object> sendResult = successfulSendResult();
        CompletableFuture<SendResult<String, Object>> future =
                CompletableFuture.completedFuture(sendResult);

        doReturn(future)
                .when(kafkaTemplate)
                .send(any(ProducerRecord.class));

        AuthEventsKafkaAdapter adapter = new AuthEventsKafkaAdapter(
                kafkaTemplate,
                "   "
        );

        try (MockedStatic<CorrelationId> correlationId =
                     mockCorrelationId("   ", GENERATED_CORRELATION_ID)) {

            // Act
            adapter.publish(event);

            // Assert
            ArgumentCaptor<ProducerRecord<String, Object>> captor =
                    producerRecordCaptor();

            verify(kafkaTemplate).send(captor.capture());

            ProducerRecord<String, Object> record = captor.getValue();

            assertAll(
                    () -> assertEquals(
                            DEFAULT_TOPIC_USER_REGISTERED,
                            record.topic()
                    ),
                    () -> assertEquals(
                            GENERATED_CORRELATION_ID,
                            headerValue(record, CorrelationId.HEADER)
                    )
            );

            correlationId.verify(CorrelationId::current);
            correlationId.verify(CorrelationId::generate);
        }
    }

    @Test
    @DisplayName("Deve rejeitar evento de registro nulo sem enviar mensagem ao Kafka")
    void publish_userRegisteredEventNulo_deveLancarNullPointerException() {
        // Arrange
        AuthEventsKafkaAdapter adapter = createAdapter();

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.publish((UserRegisteredEvent) null)
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "event must not be null",
                        exception.getMessage()
                ),
                () -> verifyNoInteractions(kafkaTemplate)
        );
    }

    @Test
    @DisplayName("Deve publicar a confirmação de e-mail utilizando o identificador do usuário como chave")
    void publish_userEmailConfirmedEventValido_deveEnviarEventoComSucesso() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserEmailConfirmedEvent event =
                mock(UserEmailConfirmedEvent.class);

        AuthUser user = mockAuthUser(userId);
        when(event.user()).thenReturn(user);

        RecordMetadata metadata = mock(RecordMetadata.class);
        SendResult<String, Object> sendResult = mock(SendResult.class);

        when(metadata.partition()).thenReturn(2);
        when(metadata.offset()).thenReturn(15L);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        when(
                kafkaTemplate.send(
                        TOPIC_USER_EMAIL_CONFIRMED,
                        userId.toString(),
                        event
                )
        ).thenReturn(CompletableFuture.completedFuture(sendResult));

        AuthEventsKafkaAdapter adapter = createAdapter();

        // Act
        adapter.publish(event);

        // Assert
        assertAll(
                () -> verify(kafkaTemplate).send(
                        TOPIC_USER_EMAIL_CONFIRMED,
                        userId.toString(),
                        event
                ),
                () -> verify(sendResult).getRecordMetadata(),
                () -> verify(metadata).partition(),
                () -> verify(metadata).offset()
        );
    }

    @Test
    @DisplayName("Deve rejeitar evento de confirmação de e-mail nulo sem enviar mensagem ao Kafka")
    void publish_userEmailConfirmedEventNulo_deveLancarNullPointerException() {
        // Arrange
        AuthEventsKafkaAdapter adapter = createAdapter();

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.publish((UserEmailConfirmedEvent) null)
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "event must not be null",
                        exception.getMessage()
                ),
                () -> verifyNoInteractions(kafkaTemplate)
        );
    }

    @Test
    @DisplayName("Deve registrar a falha assíncrona ao publicar a autenticação do usuário")
    void publish_userAuthenticatedEventComFalha_deveConcluirSemPropagarExcecao() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserAuthenticatedEvent event =
                mock(UserAuthenticatedEvent.class);

        AuthUser user = mockAuthUser(userId);
        when(event.user()).thenReturn(user);

        RuntimeException sendFailure =
                new RuntimeException("Falha ao publicar autenticação");
        CompletableFuture<SendResult<String, Object>> future =
                CompletableFuture.failedFuture(sendFailure);

        when(
                kafkaTemplate.send(
                        TOPIC_USER_AUTHENTICATED,
                        userId.toString(),
                        event
                )
        ).thenReturn(future);

        AuthEventsKafkaAdapter adapter = createAdapter();

        // Act
        assertDoesNotThrow(() -> adapter.publish(event));

        // Assert
        assertAll(
                () -> verify(kafkaTemplate).send(
                        TOPIC_USER_AUTHENTICATED,
                        userId.toString(),
                        event
                ),
                () -> assertTrue(future.isCompletedExceptionally())
        );
    }

    @Test
    @DisplayName("Deve rejeitar evento de autenticação nulo sem enviar mensagem ao Kafka")
    void publish_userAuthenticatedEventNulo_deveLancarNullPointerException() {
        // Arrange
        AuthEventsKafkaAdapter adapter = createAdapter();

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.publish((UserAuthenticatedEvent) null)
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "event must not be null",
                        exception.getMessage()
                ),
                () -> verifyNoInteractions(kafkaTemplate)
        );
    }

    @Test
    @DisplayName("Deve publicar a solicitação de redefinição de senha utilizando o identificador do usuário como chave")
    void publish_passwordResetRequestedEventValido_deveEnviarEventoComSucesso() {
        // Arrange
        UUID userId = UUID.randomUUID();
        PasswordResetRequestedEvent event =
                mock(PasswordResetRequestedEvent.class);

        AuthUser user = mockAuthUser(userId);
        when(event.user()).thenReturn(user);

        RecordMetadata metadata = mock(RecordMetadata.class);
        SendResult<String, Object> sendResult = mock(SendResult.class);

        when(metadata.partition()).thenReturn(4);
        when(metadata.offset()).thenReturn(25L);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        when(
                kafkaTemplate.send(
                        TOPIC_PASSWORD_RESET_REQUESTED,
                        userId.toString(),
                        event
                )
        ).thenReturn(CompletableFuture.completedFuture(sendResult));

        AuthEventsKafkaAdapter adapter = createAdapter();

        // Act
        adapter.publish(event);

        // Assert
        assertAll(
                () -> verify(kafkaTemplate).send(
                        TOPIC_PASSWORD_RESET_REQUESTED,
                        userId.toString(),
                        event
                ),
                () -> verify(sendResult).getRecordMetadata(),
                () -> verify(metadata).partition(),
                () -> verify(metadata).offset()
        );
    }

    @Test
    @DisplayName("Deve rejeitar evento de redefinição de senha nulo sem enviar mensagem ao Kafka")
    void publish_passwordResetRequestedEventNulo_deveLancarNullPointerException() {
        // Arrange
        AuthEventsKafkaAdapter adapter = createAdapter();

        // Act
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.publish(
                        (PasswordResetRequestedEvent) null
                )
        );

        // Assert
        assertAll(
                () -> assertEquals(
                        "event must not be null",
                        exception.getMessage()
                ),
                () -> verifyNoInteractions(kafkaTemplate)
        );
    }

    private AuthEventsKafkaAdapter createAdapter() {
        return new AuthEventsKafkaAdapter(
                kafkaTemplate,
                CUSTOM_TOPIC_USER_REGISTERED
        );
    }

    private UserRegisteredEvent mockRegisteredEvent(
            UUID userId,
            Instant occurredAt
    ) {
        UserRegisteredEvent event = mock(UserRegisteredEvent.class);
        AuthUser user = mockAuthUser(userId);

        when(event.user()).thenReturn(user);
        when(event.occurredAt()).thenReturn(occurredAt);

        return event;
    }

    private AuthUser mockAuthUser(UUID userId) {
        AuthUser user = mock(
                AuthUser.class,
                RETURNS_DEEP_STUBS
        );

        when(user.id().value()).thenReturn(userId);
        when(user.fullName()).thenReturn(FULL_NAME);
        when(user.email().value()).thenReturn(EMAIL);

        return user;
    }

    private SendResult<String, Object> successfulSendResult() {
        RecordMetadata metadata = mock(RecordMetadata.class);
        SendResult<String, Object> sendResult = mock(SendResult.class);

        when(metadata.partition()).thenReturn(1);
        when(metadata.offset()).thenReturn(10L);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);

        return sendResult;
    }

    private MockedStatic<CorrelationId> mockCorrelationId(
            String current,
            String generated
    ) {
        MockedStatic<CorrelationId> correlationId =
                mockStaticCorrelationId();

        correlationId.when(CorrelationId::current)
                .thenReturn(current);

        if (generated != null) {
            correlationId.when(CorrelationId::generate)
                    .thenReturn(generated);
        }

        return correlationId;
    }

    private MockedStatic<CorrelationId> mockStaticCorrelationId() {
        return org.mockito.Mockito.mockStatic(CorrelationId.class);
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<ProducerRecord<String, Object>>
    producerRecordCaptor() {
        return ArgumentCaptor.forClass(
                (Class<ProducerRecord<String, Object>>) (Class<?>)
                        ProducerRecord.class
        );
    }

    private String headerValue(
            ProducerRecord<String, Object> record,
            String headerName
    ) {
        Header header = record.headers().lastHeader(headerName);

        assertNotNull(
                header,
                "O header " + headerName + " deve existir"
        );

        return new String(
                header.value(),
                StandardCharsets.UTF_8
        );
    }

    private void assertMessageValues(
            AuthUserRegisteredMessage message,
            UUID userId,
            Instant occurredAt,
            String eventId,
            String correlationId
    ) throws Exception {
        List<Object> messageValues = instanceFieldValues(message);

        assertAll(
                () -> assertTrue(
                        messageValues.contains(userId.toString())
                ),
                () -> assertTrue(messageValues.contains(FULL_NAME)),
                () -> assertTrue(messageValues.contains(EMAIL))
        );

        AuthUserRegisteredMessage.EventMetadata metadata =
                messageValues.stream()
                        .filter(
                                AuthUserRegisteredMessage.EventMetadata.class
                                        ::isInstance
                        )
                        .map(
                                AuthUserRegisteredMessage.EventMetadata.class
                                        ::cast
                        )
                        .findFirst()
                        .orElseThrow();

        List<Object> metadataValues = instanceFieldValues(metadata);

        assertAll(
                () -> assertTrue(metadataValues.contains(eventId)),
                () -> assertTrue(metadataValues.contains(occurredAt)),
                () -> assertTrue(metadataValues.contains(correlationId)),
                () -> assertTrue(metadataValues.contains(PRODUCER))
        );
    }

    private List<Object> instanceFieldValues(Object target)
            throws IllegalAccessException {
        List<Object> values = new ArrayList<>();

        for (Field field : target.getClass().getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                values.add(field.get(target));
            }
        }

        return values;
    }
}
