package br.com.ecofy.ms_budgeting.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Configuration
public class KafkaErrorHandlingConfig {

    // Cria o recoverer que publica mensagens com falha em um tópico DLT (topic + ".DLT").
    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(KafkaTemplate<String, Object> template) {
        return new DeadLetterPublishingRecoverer(template, (record, ex) ->
                new TopicPartition(record.topic() + ".DLT", record.partition())
        );
    }

    // Configura o ErrorHandler com retry (backoff) e redirecionamento para DLT após esgotar tentativas.
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(DeadLetterPublishingRecoverer recoverer) {
        FixedBackOff backOff = new FixedBackOff(2_000L, 3L);
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);

        handler.addNotRetryableExceptions(IllegalArgumentException.class);

        handler.setRetryListeners((record, ex, deliveryAttempt) -> {
            log.warn("[KafkaErrorHandler] RETRY attempt={} topic={} partition={} offset={} error={}",
                    deliveryAttempt, record.topic(), record.partition(), record.offset(), ex.getMessage());
        });

        return handler;
    }
}
