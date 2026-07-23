package br.com.ecofy.ms_notification.core.application.service;

import br.com.ecofy.ms_notification.core.application.config.NotificationSettings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;

// Centraliza a política de retentativas e o cálculo dos intervalos entre execuções.
@Slf4j
@Service
public class RetryPolicyService {

    private final NotificationSettings settings;

    public RetryPolicyService(NotificationSettings settings) {
        this.settings = Objects.requireNonNull(
                settings,
                "settings must not be null"
        );
        Objects.requireNonNull(
                settings.retryBaseBackoff(),
                "settings.retryBaseBackoff must not be null"
        );
    }

    // Valida se a quantidade atual permite uma nova tentativa.
    public boolean canRetry(int attemptCount) {
        int safeAttempts = Math.max(0, attemptCount);
        int maxAttempts = Math.max(1, settings.retryMaxAttempts());

        boolean can = safeAttempts < maxAttempts;

        log.debug(
                "[RetryPolicyService] - [canRetry] -> Avaliando retentativa attemptCount={} maxAttempts={} canRetry={}",
                safeAttempts,
                maxAttempts,
                can
        );

        return can;
    }

    // Calcula o intervalo exponencial respeitando o limite máximo configurado.
    public Duration computeBackoff(int attemptCount) {
        int safeAttempts = Math.max(0, attemptCount);

        Duration base = settings.retryBaseBackoff();
        double multiplier = retryMultiplier();
        double factor = Math.pow(multiplier, safeAttempts);

        long baseMs = Math.max(0L, base.toMillis());
        long computedMs = safeMultiply(baseMs, factor);

        Duration backoff = clamp(
                Duration.ofMillis(computedMs),
                settings.retryMaxBackoff()
        );

        log.debug(
                "[RetryPolicyService] - [computeBackoff] -> Backoff calculado attemptCount={} baseMs={} multiplier={} computedMs={} backoffMs={}",
                safeAttempts,
                baseMs,
                multiplier,
                computedMs,
                backoff.toMillis()
        );

        return backoff;
    }

    // Normaliza multiplicadores inválidos para um valor neutro.
    private double retryMultiplier() {
        double multiplier = settings.retryMultiplier();

        if (Double.isNaN(multiplier)
                || Double.isInfinite(multiplier)
                || multiplier <= 0d) {
            return 1d;
        }

        return multiplier;
    }

    // Calcula o intervalo em milissegundos com proteção contra valores inválidos e overflow.
    private static long safeMultiply(long baseMs, double factor) {
        if (baseMs <= 0L) {
            return 0L;
        }

        if (Double.isNaN(factor)
                || Double.isInfinite(factor)
                || factor <= 0d) {
            return 0L;
        }

        double value = baseMs * factor;

        if (value >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }

        return (long) value;
    }

    // Limita a duração a valores não negativos e ao máximo configurado.
    private static Duration clamp(Duration value, Duration max) {
        if (value == null) {
            return Duration.ZERO;
        }

        if (value.isNegative()) {
            return Duration.ZERO;
        }

        return value.compareTo(max) > 0 ? max : value;
    }
}
