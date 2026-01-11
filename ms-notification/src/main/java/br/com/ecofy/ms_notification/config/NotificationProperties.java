package br.com.ecofy.ms_notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "notification")
public class NotificationProperties {

    private Topics topics = new Topics();
    private Idempotency idempotency = new Idempotency();
    private Retry retry = new Retry();
    private Templates templates = new Templates();
    private Clients clients = new Clients();

    @Getter @Setter
    public static class Topics {
        private String budgetAlert = "eco.budget.alert";
        private String insightCreated = "eco.insight.created";
        private String notificationSent = "eco.notification.sent";
    }

    @Getter @Setter
    public static class Idempotency {
        private Duration ttl = Duration.ofHours(24);
        private boolean enabled = true;
    }

    @Getter @Setter
    public static class Retry {
        private int maxAttempts = 3;
        private Duration baseBackoff = Duration.ofSeconds(3);
        private double multiplier = 2.0;
    }

    @Getter @Setter
    public static class Templates {
        private String defaultLocale = "pt-BR";
        private Map<String, String> defaultChannels = Map.of(
                "BUDGET_ALERT", "EMAIL",
                "INSIGHT_CREATED", "PUSH"
        );
    }

    @Getter @Setter
    public static class Clients {
        private UserProfile userProfile = new UserProfile();

        @Getter @Setter
        public static class UserProfile {
            private boolean enabled = false; // placeholder stub by default
            private String baseUrl = "http://localhost:8086";
            private int connectTimeoutMs = 800;
            private int readTimeoutMs = 1500;
        }
    }
}