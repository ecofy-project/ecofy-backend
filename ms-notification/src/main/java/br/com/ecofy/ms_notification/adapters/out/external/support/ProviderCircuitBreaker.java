package br.com.ecofy.ms_notification.adapters.out.external.support;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

// Protege cada provider externo com um circuit breaker próprio, alimentado apenas por falhas transitórias.
@Slf4j
public class ProviderCircuitBreaker {

    public enum State { CLOSED, OPEN, HALF_OPEN }

    private final String name;
    private final int failureThreshold;
    private final Duration openWait;
    private final MeterRegistry meterRegistry;

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicReference<Instant> openedAt = new AtomicReference<>(null);

    public ProviderCircuitBreaker(String name, int failureThreshold, Duration openWait, MeterRegistry meterRegistry) {
        this.name = name;
        this.failureThreshold = Math.max(1, failureThreshold);
        this.openWait = openWait == null ? Duration.ofSeconds(30) : openWait;
        this.meterRegistry = meterRegistry;
    }

    // Informa se a chamada pode prosseguir, negando enquanto o circuito estiver aberto.
    public boolean allowRequest() {
        State s = state.get();
        if (s == State.CLOSED) return true;
        if (s == State.OPEN) {
            Instant opened = openedAt.get();
            if (opened != null && Instant.now().isAfter(opened.plus(openWait))) {
                // Janela expirou → tentativa controlada.
                if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                    log.info("[ProviderCircuitBreaker] name={} OPEN -> HALF_OPEN (trial)", name);
                }
                return true;
            }
            return false;
        }
        // HALF_OPEN: permite uma tentativa de teste.
        return true;
    }

    public void recordSuccess() {
        State prev = state.getAndSet(State.CLOSED);
        consecutiveFailures.set(0);
        openedAt.set(null);
        if (prev != State.CLOSED) {
            log.info("[ProviderCircuitBreaker] name={} {} -> CLOSED (recovered)", name, prev);
        }
    }

    // Registra uma falha transitória que conta para a abertura do circuito.
    public void recordFailure() {
        if (state.get() == State.HALF_OPEN) {
            trip();
            return;
        }
        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= failureThreshold) {
            trip();
        }
    }

    private void trip() {
        State prev = state.getAndSet(State.OPEN);
        openedAt.set(Instant.now());
        if (prev != State.OPEN && meterRegistry != null) {
            meterRegistry.counter("ecofy.notification.circuit.breaker.open.total", "provider", name).increment();
        }
        log.warn("[ProviderCircuitBreaker] name={} {} -> OPEN (failures={})", name, prev, consecutiveFailures.get());
    }

    public State state() {
        return state.get();
    }

    public String name() {
        return name;
    }
}
