package br.com.ecofy.ms_insights.adapters.out.messaging;

import br.com.ecofy.ms_insights.adapters.out.messaging.mapper.EventMapper;
import br.com.ecofy.ms_insights.config.InsightsProperties;
import br.com.ecofy.ms_insights.core.domain.Insight;
import br.com.ecofy.ms_insights.core.port.out.PublishInsightCreatedEventPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Objects;

@Slf4j
@Component
public class InsightCreatedKafkaAdapter implements PublishInsightCreatedEventPort {

    private final InsightsProperties properties;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    // Injeta dependências de configuração, KafkaTemplate, ObjectMapper e Clock garantindo que estejam presentes.
    public InsightCreatedKafkaAdapter(InsightsProperties properties,
                                      KafkaTemplate<String, String> kafkaTemplate,
                                      ObjectMapper objectMapper,
                                      Clock clock) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.kafkaTemplate = Objects.requireNonNull(kafkaTemplate, "kafkaTemplate must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    // Publica o evento insight.created no Kafka: valida entradas/config, mapeia Insight -> evento, serializa em JSON e envia com chave por userId.
    @Override
    public void publish(Insight insight) {
        Objects.requireNonNull(insight, "insight must not be null");

        String topic = requireNonBlank(properties.topics().insightCreatedTopic(), "insightCreatedTopic");

        try {
            var evt = EventMapper.toCreatedEvent(insight, clock);

            String key = requireNonBlank(evt.userId(), "event.userId");

            String payload = objectMapper.writeValueAsString(evt);

            log.info(
                    "[InsightCreatedKafkaAdapter] - [publish] -> Publishing insight.created topic={} key={} insightId={} type={} score={}",
                    topic, key, evt.insightId(), insight.getType(), insight.getScore()
            );

            kafkaTemplate.send(topic, key, payload);

            log.debug(
                    "[InsightCreatedKafkaAdapter] - [publish] -> Sent insight.created topic={} key={} bytes={}",
                    topic, key, payload.length()
            );

        } catch (Exception ex) {
            log.error(
                    "[InsightCreatedKafkaAdapter] - [publish] -> FAILED publishing insight.created topic={} insightId={}",
                    topic, safeInsightId(insight), ex
            );
            throw new IllegalStateException("Failed to publish insight.created", ex);
        }
    }

    // Garante que um valor String obrigatório esteja preenchido (não nulo/não vazio), normalizando com trim e lançando IllegalArgumentException em caso de falha.
    private static String requireNonBlank(String v, String field) {
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return v.trim();
    }

    // Obtém o id do Insight com segurança para logging, evitando falhas caso o objeto esteja inconsistente.
    private static String safeInsightId(Insight insight) {
        try {
            return insight.getId() != null ? insight.getId().toString() : "null";
        } catch (Exception ignore) {
            return "unavailable";
        }
    }

}
