package br.com.ecofy.ms_notification.core.application.service;

import br.com.ecofy.ms_notification.config.NotificationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;

@Slf4j
@Service
public class RetryPolicyService {

    private static final Duration MAX_BACKOFF = Duration.ofSeconds(60);

    private final NotificationProperties.Retry retryProps;

    public RetryPolicyService(NotificationProperties props) {
        Objects.requireNonNull(props, "props must not be null");
        this.retryProps = Objects.requireNonNull(props.getRetry(), "props.retry must not be null");
        Objects.requireNonNull(retryProps.getBaseBackoff(), "props.retry.baseBackoff must not be null");
    }

    public boolean canRetry(int attemptCount) {
        int safeAttempts = Math.max(0, attemptCount);
        int maxAttempts = Math.max(1, retryProps.getMaxAttempts());

        boolean can = safeAttempts < maxAttempts;

        log.debug(
                "[RetryPolicyService] - [canRetry] -> attemptCount={} maxAttempts={} canRetry={}",
                safeAttempts,
                maxAttempts,
                can
        );

        return can;
    }

    public Duration computeBackoff(int attemptCount) {
        int safeAttempts = Math.max(0, attemptCount);

        Duration base = retryProps.getBaseBackoff();
        double multiplier = retryMultiplier();
        double factor = Math.pow(multiplier, safeAttempts);

        long baseMs = Math.max(0L, base.toMillis());
        long computedMs = safeMultiply(baseMs, factor);

        Duration backoff = clamp(Duration.ofMillis(computedMs), MAX_BACKOFF);

        log.debug(
                "[RetryPolicyService] - [computeBackoff] -> attemptCount={} baseMs={} multiplier={} computedMs={} backoffMs={}",
                safeAttempts,
                baseMs,
                multiplier,
                computedMs,
                backoff.toMillis()
        );

        return backoff;
    }

    private double retryMultiplier() {
        double m = retryProps.getMultiplier();
        if (Double.isNaN(m) || Double.isInfinite(m) || m <= 0d) return 1d;
        return m;
    }

    private static long safeMultiply(long baseMs, double factor) {
        if (baseMs <= 0L) return 0L;
        if (Double.isNaN(factor) || Double.isInfinite(factor) || factor <= 0d) return 0L;

        double v = baseMs * factor;
        if (v >= Long.MAX_VALUE) return Long.MAX_VALUE;
        return (long) v;
    }

    private static Duration clamp(Duration value, Duration max) {
        if (value == null) return Duration.ZERO;
        if (value.isNegative()) return Duration.ZERO;
        return value.compareTo(max) > 0 ? max : value;
    }
}
