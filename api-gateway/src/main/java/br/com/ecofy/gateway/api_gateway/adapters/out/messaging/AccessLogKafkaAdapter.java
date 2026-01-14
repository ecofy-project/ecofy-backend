package br.com.ecofy.gateway.api_gateway.adapters.out.messaging;

import br.com.ecofy.gateway.api_gateway.core.port.out.PublishAccessLogPort;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "ecofy.gateway.logging.kafka", name = "enabled", havingValue = "true")
public class AccessLogKafkaAdapter implements PublishAccessLogPort {

    private static final Logger log = LoggerFactory.getLogger(AccessLogKafkaAdapter.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final GatewayLoggingProperties props;

    public AccessLogKafkaAdapter(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            GatewayLoggingProperties props
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.props = props;
    }

    @Override
    public void publishAccessLog(AccessLogEntry entry) {
        if (!props.enabled()) return;

        String topic = props.topicName();
        String key = entry.path() == null ? "" : entry.path();
        String payload = toJson(entry);

        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, payload);

        if (entry.tenantContext() != null) {
            if (entry.tenantContext().tenantId() != null) {
                record.headers().add("x-tenant-id", entry.tenantContext().tenantId().getBytes(StandardCharsets.UTF_8));
            }
            if (entry.tenantContext().subject() != null) {
                record.headers().add("x-user-id", entry.tenantContext().subject().getBytes(StandardCharsets.UTF_8));
            }
        }

        Map<String, String> tags = entry.extraTags() == null ? Map.of() : entry.extraTags();
        String traceId = String.valueOf(tags.getOrDefault("traceId", ""));
        if (!traceId.isBlank()) {
            record.headers().add("x-trace-id", traceId.getBytes(StandardCharsets.UTF_8));
        }

        kafkaTemplate.send(record).whenComplete((res, ex) -> {
            if (ex != null) {
                log.warn("[AccessLogKafkaAdapter] Failed to send access log. topic={}, key={}, err={}",
                        topic, key, ex.getMessage(), ex);
            }
        });
    }

    private String toJson(AccessLogEntry entry) {
        try {
            return objectMapper.writeValueAsString(entry);
        } catch (Exception e) {
            return "{ \"error\": \"failed-to-serialize-access-log\", \"path\": \"" + (entry.path() == null ? "" : entry.path()) + "\" }";
        }
    }
}
