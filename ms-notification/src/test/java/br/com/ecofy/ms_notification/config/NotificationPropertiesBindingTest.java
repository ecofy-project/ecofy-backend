package br.com.ecofy.ms_notification.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Correção Dia 7 (item #1): o application.yml define as propriedades sob "ecofy.notification",
 * mas o @ConfigurationProperties usava o prefixo "notification" -> nada bindava. Este teste garante
 * que o prefixo alinhado ("ecofy.notification") realmente faz o bind das propriedades customizadas.
 */
class NotificationPropertiesBindingTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(TestConfig.class);

    @EnableConfigurationProperties(NotificationProperties.class)
    static class TestConfig { }

    @Test
    void shouldBindCustomPropertiesUnderEcofyNotificationPrefix() {
        runner.withPropertyValues(
                "ecofy.notification.topics.budget-alert=eco.budget.alert.v2",
                "ecofy.notification.topics.insight-created=eco.insight.created.v2",
                "ecofy.notification.topics.notification-sent=eco.notification.sent.v2",
                "ecofy.notification.idempotency.enabled=false",
                "ecofy.notification.idempotency.ttl=PT10M",
                "ecofy.notification.retry.max-attempts=7",
                "ecofy.notification.retry.base-backoff=PT5S",
                "ecofy.notification.retry.multiplier=3.0",
                "ecofy.notification.clients.user-profile.enabled=true",
                "ecofy.notification.clients.user-profile.base-url=http://ms-users:9000/users"
        ).run(ctx -> {
            assertThat(ctx).hasSingleBean(NotificationProperties.class);
            NotificationProperties props = ctx.getBean(NotificationProperties.class);

            assertThat(props.getTopics().getBudgetAlert()).isEqualTo("eco.budget.alert.v2");
            assertThat(props.getTopics().getInsightCreated()).isEqualTo("eco.insight.created.v2");
            assertThat(props.getTopics().getNotificationSent()).isEqualTo("eco.notification.sent.v2");

            assertThat(props.getIdempotency().isEnabled()).isFalse();
            assertThat(props.getIdempotency().getTtl()).isEqualTo(Duration.ofMinutes(10));

            assertThat(props.getRetry().getMaxAttempts()).isEqualTo(7);
            assertThat(props.getRetry().getBaseBackoff()).isEqualTo(Duration.ofSeconds(5));
            assertThat(props.getRetry().getMultiplier()).isEqualTo(3.0);

            assertThat(props.getClients().getUserProfile().isEnabled()).isTrue();
            assertThat(props.getClients().getUserProfile().getBaseUrl()).isEqualTo("http://ms-users:9000/users");
        });
    }

    @Test
    void shouldNotBindWhenUsingOldNotificationPrefix() {
        // Prova a regressão: propriedades sob o prefixo antigo "notification" NÃO são mais bindadas.
        runner.withPropertyValues(
                "notification.retry.max-attempts=99"
        ).run(ctx -> {
            NotificationProperties props = ctx.getBean(NotificationProperties.class);
            assertThat(props.getRetry().getMaxAttempts()).isEqualTo(3); // default, não 99
        });
    }
}
