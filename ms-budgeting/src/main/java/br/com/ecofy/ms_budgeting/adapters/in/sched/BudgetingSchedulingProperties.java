package br.com.ecofy.ms_budgeting.adapters.in.sched;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ecofy.budgeting.scheduling")
public class BudgetingSchedulingProperties {

    /** Habilita/desabilita a rotina de limpeza agendada (default seguro: false). */
    private boolean cleanupEnabled = false;

    /** Define a retenção em dias para a limpeza (ex.: 90 apaga dados anteriores a hoje-90 dias). */
    private int cleanupRetentionDays = 90;
}
