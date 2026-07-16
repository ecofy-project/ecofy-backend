package br.com.ecofy.ms_budgeting.adapters.in.sched;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ecofy.budgeting.scheduling")
public class BudgetingSchedulingProperties {

    /** Liga/desliga todo o agendamento (@EnableScheduling via SchedulingConfig). Default: true. */
    private boolean enabled = true;

    /** Habilita/desabilita a rotina de recálculo agendada (default: true). */
    private boolean recalculationEnabled = true;

    /** Habilita/desabilita a rotina de limpeza agendada (default seguro: false). */
    private boolean cleanupEnabled = false;

    /** Define a retenção em dias para a limpeza (ex.: 90 apaga dados anteriores a hoje-90 dias). */
    private int cleanupRetentionDays = 90;

    /** Cron da rotina de limpeza (também usado pelo placeholder do @Scheduled). */
    private String cleanupCron = "0 0 3 * * *";

    /** Cron da rotina de recálculo (também usado pelo placeholder do @Scheduled). */
    private String recalculateCron = "0 0/15 * * * *";
}
