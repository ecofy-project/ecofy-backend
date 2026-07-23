package br.com.ecofy.ms_notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

// Centraliza as propriedades de configuração do serviço de notificações.
@Getter
@Setter
@ConfigurationProperties(prefix = "ecofy.notification")
public class NotificationProperties {

    private Topics topics = new Topics();
    private Idempotency idempotency = new Idempotency();
    private Retry retry = new Retry();
    private Templates templates = new Templates();
    private Clients clients = new Clients();
    private Providers providers = new Providers();
    private CircuitBreaker circuitBreaker = new CircuitBreaker();

    // Centraliza os tópicos Kafka utilizados pelo serviço.
    @Getter
    @Setter
    public static class Topics {

        private String budgetAlert = "eco.budget.alert";
        private String insightCreated = "eco.insight.created";
        private String notificationSent = "eco.notification.sent";
    }

    // Configura a política de deduplicação de eventos e comandos.
    @Getter
    @Setter
    public static class Idempotency {

        private Duration ttl = Duration.ofHours(24);
        private boolean enabled = true;
    }

    // Configura as tentativas e os intervalos das entregas.
    @Getter
    @Setter
    public static class Retry {

        private int maxAttempts = 3;
        private Duration baseBackoff = Duration.ofSeconds(3);
        private double multiplier = 2.0;
        private Duration maxBackoff = Duration.ofMinutes(5);
    }

    // Centraliza os provedores externos disponíveis por canal.
    @Getter
    @Setter
    public static class Providers {

        private Provider email = new Provider();
        private Provider push = new Provider();
        private Provider whatsapp = new Provider();

        // Configura a comunicação com um provedor de entrega.
        @Getter
        @Setter
        public static class Provider {

            private boolean enabled = false;
            private String baseUrl = "";
            private String apiKey = "";
            private String sender = "";
            private Duration connectTimeout = Duration.ofSeconds(3);
            private Duration readTimeout = Duration.ofSeconds(10);
        }
    }

    // Configura a proteção contra falhas consecutivas dos provedores.
    @Getter
    @Setter
    public static class CircuitBreaker {

        private int failureThreshold = 5;
        private Duration waitDurationOpenState = Duration.ofSeconds(30);
    }

    // Configura os padrões de renderização e roteamento dos templates.
    @Getter
    @Setter
    public static class Templates {

        private String defaultLocale = "pt-BR";
        private Map<String, String> defaultChannels = Map.of(
                "BUDGET_ALERT", "EMAIL",
                "INSIGHT_CREATED", "PUSH"
        );
    }

    // Centraliza as configurações dos serviços externos consultados.
    @Getter
    @Setter
    public static class Clients {

        private UserProfile userProfile = new UserProfile();

        // Configura o acesso aos dados de contato do usuário.
        @Getter
        @Setter
        public static class UserProfile {

            private boolean enabled = false;
            private String baseUrl = "http://localhost:8086";
            private int connectTimeoutMs = 800;
            private int readTimeoutMs = 1500;
            private String serviceToken = "";
        }
    }
}
