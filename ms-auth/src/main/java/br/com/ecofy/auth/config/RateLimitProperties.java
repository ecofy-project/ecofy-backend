package br.com.ecofy.auth.config;

import br.com.ecofy.auth.core.domain.ratelimit.RateLimitPolicy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

// Configura as políticas de rate limiting aplicadas às operações sensíveis.
@Validated
@ConfigurationProperties(prefix = "ecofy.auth.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;

    private List<String> trustedProxies = List.of(
            "127.0.0.1",
            "::1",
            "10.",
            "172.",
            "192.168."
    );

    private Map<String, @Valid Policy> policies =
            new LinkedHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getTrustedProxies() {
        return trustedProxies;
    }

    public void setTrustedProxies(
            List<String> trustedProxies
    ) {
        this.trustedProxies = trustedProxies == null
                ? List.of()
                : trustedProxies;
    }

    public Map<String, Policy> getPolicies() {
        return policies;
    }

    public void setPolicies(Map<String, Policy> policies) {
        this.policies = policies == null
                ? new LinkedHashMap<>()
                : policies;
    }

    // Converte a configuração da operação para uma política do domínio.
    public Optional<RateLimitPolicy> policyFor(
            String operation
    ) {
        Policy p = policies.get(operation);

        if (p == null) {
            return Optional.empty();
        }

        return Optional.of(
                new RateLimitPolicy(
                        operation,
                        p.getLimit(),
                        p.getWindow()
                )
        );
    }

    // Representa o limite e a janela configurados para uma operação.
    public static class Policy {

        @Min(1)
        private int limit;

        @NotNull
        private Duration window;

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public Duration getWindow() {
            return window;
        }

        public void setWindow(Duration window) {
            this.window = window;
        }
    }
}
