package br.com.ecofy.ms_budgeting.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;

@Slf4j
@Configuration
public class KafkaListenerConfig {

    @Value("${ecofy.budgeting.kafka.listener.concurrency:3}")
    private int concurrency;

    @Bean(name = "budgetingKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<Object, Object> budgetingKafkaListenerContainerFactory(
            ConsumerFactory<Object, Object> consumerFactory,
            DefaultErrorHandler kafkaErrorHandler,
            KafkaProperties kafkaProperties
    ) {
        var factory = new ConcurrentKafkaListenerContainerFactory<Object, Object>();
        factory.setConsumerFactory(consumerFactory);

        // Error handler (Spring Kafka 4)
        factory.setCommonErrorHandler(kafkaErrorHandler);

        // Concurrency
        int resolvedConcurrency = Math.max(concurrency, 1);
        if (resolvedConcurrency != concurrency) {
            log.warn("[KafkaListenerConfig] concurrency inválida ({}). Forçando para {}.", concurrency, resolvedConcurrency);
        }
        factory.setConcurrency(resolvedConcurrency);

        // Opcional: aplique ackMode/pollTimeout via application.yml (se existir)
        // Mantém compatibilidade com o que o Boot já expõe em spring.kafka.listener.*
        KafkaProperties.Listener listener = kafkaProperties.getListener();
        if (listener != null && listener.getAckMode() != null) {
            factory.getContainerProperties().setAckMode(listener.getAckMode());
        }
        if (listener != null && listener.getPollTimeout() != null) {
            factory.getContainerProperties().setPollTimeout(listener.getPollTimeout().toMillis());
        }

        log.info("[KafkaListenerConfig] Listener factory configurada. concurrency={}", resolvedConcurrency);
        return factory;
    }
}
