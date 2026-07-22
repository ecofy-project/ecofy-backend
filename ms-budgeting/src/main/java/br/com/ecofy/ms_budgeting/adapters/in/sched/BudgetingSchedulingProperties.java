package br.com.ecofy.ms_budgeting.adapters.in.sched;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ecofy.budgeting.scheduling")
// Configura as propriedades das rotinas agendadas do serviço de orçamentos.
public class BudgetingSchedulingProperties {

    private boolean enabled = true;

    private boolean recalculationEnabled = true;

    private boolean cleanupEnabled = false;

    private int cleanupRetentionDays = 90;

    private String cleanupCron = "0 0 3 * * *";

    private String recalculateCron = "0 0/15 * * * *";
}
