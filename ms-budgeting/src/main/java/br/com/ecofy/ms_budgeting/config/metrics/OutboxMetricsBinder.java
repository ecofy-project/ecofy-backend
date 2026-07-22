package br.com.ecofy.ms_budgeting.config.metrics;

import br.com.ecofy.ms_budgeting.config.BudgetingProperties;
import br.com.ecofy.ms_budgeting.core.port.out.OutboxEventPort;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

// Registra métricas do estado da outbox com tolerância a falhas de leitura.
@Slf4j
@Component
public class OutboxMetricsBinder {

    private final OutboxEventPort outboxPort;
    private final Duration processingTimeout;

    public OutboxMetricsBinder(OutboxEventPort outboxPort, BudgetingProperties props, MeterRegistry registry) {
        this.outboxPort = outboxPort;
        this.processingTimeout = props.outbox().processingTimeout();

        Gauge.builder("ecofy.budgeting.outbox.stuck.processing", this, OutboxMetricsBinder::stuckProcessing)
                .description("Registros presos em PROCESSING além do timeout")
                .register(registry);

        Gauge.builder("ecofy.budgeting.outbox.discarded", this, OutboxMetricsBinder::discarded)
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
