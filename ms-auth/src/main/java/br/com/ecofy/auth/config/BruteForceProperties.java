package br.com.ecofy.auth.config;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

// Configura os limites e a progressão dos bloqueios contra tentativas repetidas.
@Validated
@ConfigurationProperties(prefix = "ecofy.auth.brute-force")
public class BruteForceProperties {

    private boolean enabled = true;

    @Min(1)
    private int threshold = 5;

    @NotNull
    private Duration initialBlock = Duration.ofMinutes(5);

    @DecimalMin("1.0")
    private double multiplier = 2.0;

    @NotNull
    private Duration maxBlock = Duration.ofHours(24);

    @NotNull
    private Duration observationWindow = Duration.ofMinutes(15);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public Duration getInitialBlock() {
        return initialBlock;
    }

    public void setInitialBlock(Duration initialBlock) {
        this.initialBlock = initialBlock;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    public Duration getMaxBlock() {
        return maxBlock;
    }

    public void setMaxBlock(Duration maxBlock) {
        this.maxBlock = maxBlock;
    }

    public Duration getObservationWindow() {
        return observationWindow;
    }

    public void setObservationWindow(Duration observationWindow) {
        this.observationWindow = observationWindow;
    }
}
