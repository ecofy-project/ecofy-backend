package br.com.ecofy.auth.adapters.out.messaging;

import br.com.ecofy.auth.core.domain.event.PasswordResetRequestedEvent;
import br.com.ecofy.auth.core.domain.event.UserAuthenticatedEvent;
import br.com.ecofy.auth.core.domain.event.UserEmailConfirmedEvent;
import br.com.ecofy.auth.core.domain.event.UserRegisteredEvent;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuthEventsKafkaAdapterTest {

    @Test
    @DisplayName("constructor: kafkaTemplate null -> NPE com mensagem")
    void constructor_kafkaTemplateNull_throwsNpe() {
        assertThatThrownBy(() -> new AuthEventsKafkaAdapter(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("kafkaTemplate must not be null");
    }

    @Test
    @DisplayName("publish(UserRegisteredEvent): event null -> NPE")
    void publish_userRegistered_null_throwsNpe() {
        AuthEventsKafkaAdapter a = new AuthEventsKafkaAdapter(mockKafkaTemplate());
        assertThatThrownBy(() -> a.publish((UserRegisteredEvent) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("event must not be null");
    }

    @Test
    @DisplayName("publish(UserEmailConfirmedEvent): event null -> NPE")
    void publish_userEmailConfirmed_null_throwsNpe() {
        AuthEventsKafkaAdapter a = new AuthEventsKafkaAdapter(mockKafkaTemplate());
        assertThatThrownBy(() -> a.publish((UserEmailConfirmedEvent) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("event must not be null");
    }

    @Test
    @DisplayName("publish(UserAuthenticatedEvent): event null -> NPE")
    void publish_userAuthenticated_null_throwsNpe() {
        AuthEventsKafkaAdapter a = new AuthEventsKafkaAdapter(mockKafkaTemplate());
        assertThatThrownBy(() -> a.publish((UserAuthenticatedEvent) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("event must not be null");
    }

    @Test
    @DisplayName("publish(PasswordResetRequestedEvent): event null -> NPE")
    void publish_passwordResetRequested_null_throwsNpe() {
        AuthEventsKafkaAdapter a = new AuthEventsKafkaAdapter(mockKafkaTemplate());
        assertThatThrownBy(() -> a.publish((PasswordResetRequestedEvent) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("event must not be null");
    }

    @Test
    @DisplayName("publish: envia para tópicos corretos com key = userId")
    void publish_allEvents_successPath() {
        KafkaTemplate<String, Object> kt = mockKafkaTemplate();
        AuthEventsKafkaAdapter a = new AuthEventsKafkaAdapter(kt);

        UUID id = UUID.fromString("11111111-2222-3333-4444-555555555555");
        String key = id.toString();

        UserRegisteredEvent e1 = mock(UserRegisteredEvent.class, RETURNS_DEEP_STUBS);
        when(e1.user().id().value()).thenReturn(id);

        UserEmailConfirmedEvent e2 = mock(UserEmailConfirmedEvent.class, RETURNS_DEEP_STUBS);
        when(e2.user().id().value()).thenReturn(id);

        UserAuthenticatedEvent e3 = mock(UserAuthenticatedEvent.class, RETURNS_DEEP_STUBS);
        when(e3.user().id().value()).thenReturn(id);

        PasswordResetRequestedEvent e4 = mock(PasswordResetRequestedEvent.class, RETURNS_DEEP_STUBS);
        when(e4.user().id().value()).thenReturn(id);

        when(kt.send(eq("auth.user.registered"), eq(key), eq(e1)))
                .thenReturn(CompletableFuture.completedFuture(sendResult("auth.user.registered", 0, 10L)));
        when(kt.send(eq("auth.user.email-confirmed"), eq(key), eq(e2)))
                .thenReturn(CompletableFuture.completedFuture(sendResult("auth.user.email-confirmed", 1, 11L)));
        when(kt.send(eq("auth.user.authenticated"), eq(key), eq(e3)))
                .thenReturn(CompletableFuture.completedFuture(sendResult("auth.user.authenticated", 2, 12L)));
        when(kt.send(eq("auth.user.password-reset-requested"), eq(key), eq(e4)))
                .thenReturn(CompletableFuture.completedFuture(sendResult("auth.user.password-reset-requested", 3, 13L)));

        a.publish(e1);
        a.publish(e2);
        a.publish(e3);
        a.publish(e4);

        verify(kt).send("auth.user.registered", key, e1);
        verify(kt).send("auth.user.email-confirmed", key, e2);
        verify(kt).send("auth.user.authenticated", key, e3);
        verify(kt).send("auth.user.password-reset-requested", key, e4);
        verifyNoMoreInteractions(kt);
    }

    @Test
    @DisplayName("publish: callback com exception não lança")
    void publish_whenFutureCompletesExceptionally_doesNotThrow() {
        KafkaTemplate<String, Object> kt = mockKafkaTemplate();
        AuthEventsKafkaAdapter a = new AuthEventsKafkaAdapter(kt);

        UUID id = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        String key = id.toString();

        UserRegisteredEvent e = mock(UserRegisteredEvent.class, RETURNS_DEEP_STUBS);
        when(e.user().id().value()).thenReturn(id);

        CompletableFuture<SendResult<String, Object>> f = new CompletableFuture<>();
        when(kt.send("auth.user.registered", key, e)).thenReturn(f);

        assertThatCode(() -> a.publish(e)).doesNotThrowAnyException();

        f.completeExceptionally(new RuntimeException("boom"));

        verify(kt).send("auth.user.registered", key, e);
        verifyNoMoreInteractions(kt);
    }

    // heapers

    @SuppressWarnings("unchecked")
    private static KafkaTemplate<String, Object> mockKafkaTemplate() {
        return (KafkaTemplate<String, Object>) mock(KafkaTemplate.class);
    }

    private static SendResult<String, Object> sendResult(String topic, int partition, long offset) {
        ProducerRecord<String, Object> pr = new ProducerRecord<>(topic, partition, "k", "v");
        RecordMetadata md = new RecordMetadata(new TopicPartition(topic, partition), 0, 0, offset, 0, 0);
        return new SendResult<>(pr, md);
    }
}