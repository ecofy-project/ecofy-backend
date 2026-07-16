package br.com.ecofy.ms_notification.core.application.service;

import br.com.ecofy.ms_notification.core.application.config.NotificationSettings;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RetryPolicyServiceTest {

    private RetryPolicyService service(int maxAttempts, Duration base, double multiplier) {
        return new RetryPolicyService(new NotificationSettings(true, maxAttempts, base, multiplier, Map.of()));
    }

    @Test
    void canRetry_shouldBeTrueWhileUnderMaxAttempts() {
        var service = service(3, Duration.ofSeconds(2), 2.0);

        assertThat(service.canRetry(0)).isTrue();
        assertThat(service.canRetry(2)).isTrue();
        assertThat(service.canRetry(3)).isFalse();
        assertThat(service.canRetry(5)).isFalse();
    }

    @Test
    void computeBackoff_shouldGrowExponentially() {
        var service = service(5, Duration.ofSeconds(2), 2.0);

        assertThat(service.computeBackoff(0)).isEqualTo(Duration.ofSeconds(2)); // 2 * 2^0
        assertThat(service.computeBackoff(1)).isEqualTo(Duration.ofSeconds(4)); // 2 * 2^1
        assertThat(service.computeBackoff(2)).isEqualTo(Duration.ofSeconds(8)); // 2 * 2^2
    }

    @Test
    void computeBackoff_shouldClampToMax60s() {
        var service = service(20, Duration.ofSeconds(30), 10.0);
        assertThat(service.computeBackoff(5)).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void computeBackoff_shouldFallbackToMultiplierOneWhenInvalid() {
        var service = service(5, Duration.ofSeconds(3), 0.0); // multiplier inválido -> 1.0
        assertThat(service.computeBackoff(4)).isEqualTo(Duration.ofSeconds(3));
    }
}
