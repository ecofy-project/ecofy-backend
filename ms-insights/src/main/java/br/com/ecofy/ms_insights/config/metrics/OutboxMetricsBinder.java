package br.com.ecofy.ms_insights.config.metrics;

import br.com.ecofy.ms_insights.config.InsightsProperties;
import br.com.ecofy.ms_insights.core.port.out.OutboxEventPort;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

// Publica o backlog da Outbox como gauges de estado atual, tolerando erros de leitura sem quebrar a coleta.
@Slf4j
@Component
public class OutboxMetricsBinder {

    private final OutboxEventPort outboxPort;
    private final Duration processingTimeout;

    public OutboxMetricsBinder(OutboxEventPort outboxPort, InsightsProperties props, MeterRegistry registry) {
        this.outboxPort = outboxPort;
        this.processingTimeout = props.outbox().processingTimeout();

        Gauge.builder("ecofy.insights.outbox.stuck.processing", this, OutboxMetricsBinder::stuckProcessing)
                .description("Registros presos em PROCESSING além do timeout")
                .register(registry);

        Gauge.builder("ecofy.insights.outbox.discarded", this, OutboxMetricsBinder::discarded)
                .description("Registros DISCARDED (terminais, preservados para auditoria)")
                .register(registry);
    }

    private double stuckProcessing() {
        try {
            return outboxPort.countStuckProcessing(processingTimeout, Instant.now());
        } catch (Exception e) {
            log.debug("[OutboxMetricsBinder] - [stuckProcessing] -> falha error={}", e.getMessage());
            return -1;
        }
    }

    private double discarded() {
        try {
            return outboxPort.countDiscarded();
        } catch (Exception e) {
            log.debug("[OutboxMetricsBinder] - [discarded] -> falha error={}", e.getMessage());
            return -1;
        }
    }
}
