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

// Publica eventos de criação de insights no Kafka.
@Slf4j
@Component
public class InsightCreatedKafkaAdapter implements PublishInsightCreatedEventPort {

    private final InsightsProperties properties;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public InsightCreatedKafkaAdapter(InsightsProperties properties,
                                      KafkaTemplate<String, String> kafkaTemplate,
                                      ObjectMapper objectMapper,
                                      Clock clock) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.kafkaTemplate = Objects.requireNonNull(kafkaTemplate, "kafkaTemplate must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    // Converte e envia o insight como evento de integração.
    @Override
    public void publish(Insight insight) {
        Objects.requireNonNull(insight, "insight must not be null");

        String topic = requireNonBlank(properties.topics().insightCreatedTopic(), "insightCreatedTopic");

        try {
            var evt = EventMapper.toCreatedEvent(insight, clock);

            String key = requireNonBlank(evt.userId(), "event.userId");

            String payload = objectMapper.writeValueAsString(evt);

            log.info(
                    "[InsightCreatedKafkaAdapter] - [publish] -> Publicando insight.created topic={} key={} insightId={} type={} score={}",
                    topic, key, evt.insightId(), insight.getType(), insight.getScore()
            );

            final String logKey = key;
            final int bytes = payload.length();
            kafkaTemplate.send(topic, key, payload).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error(
                            "[InsightCreatedKafkaAdapter] - [publish] -> ASYNC_FAILED insight.created topic={} key={} insightId={}",
                            topic, logKey, evt.insightId(), ex
                    );
                    return;
                }
                var md = result.getRecordMetadata();
                log.info(
                        "[InsightCreatedKafkaAdapter] - [publish] -> PUBLISHED insight.created topic={} key={} partition={} offset={} bytes={}",
                        topic, logKey, md.partition(), md.offset(), bytes
                );
            });

        } catch (Exception ex) {
            log.error(
                    "[InsightCreatedKafkaAdapter] - [publish] -> Falha ao publicar insight.created topic={} insightId={}",
                    topic, safeInsightId(insight), ex
            );
            throw new IllegalStateException("Failed to publish insight.created", ex);
        }
    }

    // Valida e normaliza valores textuais obrigatórios.
    private static String requireNonBlank(String v, String field) {
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return v.trim();
    }

    // Resolve o identificador do insight sem comprometer o registro da falha.
    private static String safeInsightId(Insight insight) {
        try {
            return insight.getId() != null ? insight.getId().toString() : "null";
        } catch (Exception ignore) {
            return "unavailable";
        }
    }
}
