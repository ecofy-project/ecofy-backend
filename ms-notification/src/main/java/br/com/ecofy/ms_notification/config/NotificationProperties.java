package br.com.ecofy.ms_notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

@Getter
@Setter
// Correção Dia 7 (item #1): o application.yml define as propriedades sob "ecofy.notification",
// mas o prefixo aqui era "notification" -> nada era bindado e o serviço rodava só com defaults.
// Prefixo alinhado ao YAML.
@ConfigurationProperties(prefix = "ecofy.notification")
public class NotificationProperties {

    // Agrupa os nomes dos tópicos Kafka consumidos/publicados pelo ms-notification (com defaults seguros).
    private Topics topics = new Topics();

    // Define políticas de idempotência (TTL e toggle) para evitar processamento duplicado de eventos/commands.
    private Idempotency idempotency = new Idempotency();

    // Centraliza a política de retry (tentativas e backoff) para entregas/dispatch de notificações.
    private Retry retry = new Retry();

    // Configura templates (locale padrão e canais default por tipo de evento) para renderização e roteamento.
    private Templates templates = new Templates();

    // Define propriedades de clients externos (ex.: ms-users) usados para buscar contatos/dados do usuário.
    private Clients clients = new Clients();

    @Getter
    @Setter
    public static class Topics {

        // Tópico de entrada para eventos de alerta de orçamento (consumido pelo BudgetAlertEventConsumer).
        private String budgetAlert = "eco.budget.alert";

        // Tópico de entrada para eventos de insight criado (consumido pelo InsightCreatedEventConsumer).
        private String insightCreated = "eco.insight.created";

        // Tópico de saída para evento de notificação enviada (publicado pelo NotificationEventsKafkaAdapter).
        private String notificationSent = "eco.notification.sent";
    }

    @Getter
    @Setter
    public static class Idempotency {

        // Tempo de expiração (TTL) para chaves de idempotência (controla janela de deduplicação).
        private Duration ttl = Duration.ofHours(24);

        // Habilita/desabilita a checagem de idempotência (útil para dev/local ou troubleshooting).
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static class Retry {

        // Número máximo de tentativas de entrega/dispatch antes de marcar como falha definitiva.
        private int maxAttempts = 3;

        // Backoff base aplicado antes de reexecutar uma tentativa (ex.: 3s).
        private Duration baseBackoff = Duration.ofSeconds(3);

        // Multiplicador exponencial do backoff entre tentativas (ex.: 2.0 => 3s, 6s, 12s...).
        private double multiplier = 2.0;
    }

    @Getter
    @Setter
    public static class Templates {

        // Locale padrão para renderização de templates (fallback quando não houver locale do usuário).
        private String defaultLocale = "pt-BR";

        // Canal padrão por tipo de evento (fallback para roteamento quando não houver preferência/override).
        private Map<String, String> defaultChannels = Map.of(
                "BUDGET_ALERT", "EMAIL",
                "INSIGHT_CREATED", "PUSH"
        );
    }

    @Getter
    @Setter
    public static class Clients {

        // Configura o client que busca dados de contato do usuário (ex.: e-mail/telefone/deviceToken) no ms-users.
        private UserProfile userProfile = new UserProfile();

        @Getter
        @Setter
        public static class UserProfile {

            // Liga/desliga o client real (quando false, o adapter pode usar stub/synthetic contacts).
            private boolean enabled = false; // placeholder stub by default

            // Base URL do serviço de perfil do usuário (ms-users) para chamadas HTTP.
            private String baseUrl = "http://localhost:8086";

            // Timeout de conexão (ms) usado pelo HTTP client para estabelecer socket/TCP.
            private int connectTimeoutMs = 800;

            // Timeout de leitura (ms) usado pelo HTTP client para aguardar resposta após conexão.
            private int readTimeoutMs = 1500;
        }
    }

}
