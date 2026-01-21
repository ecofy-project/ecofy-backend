package br.com.ecofy.ms_budgeting.core.application.service;

import br.com.ecofy.ms_budgeting.core.application.command.CleanupBudgetsCommand;
import br.com.ecofy.ms_budgeting.core.application.exception.InvalidFieldException;
import br.com.ecofy.ms_budgeting.core.application.result.CleanupBudgetsResult;
import br.com.ecofy.ms_budgeting.core.port.in.CleanupBudgetsUseCase;
import br.com.ecofy.ms_budgeting.core.port.out.DeleteArchivedBudgetsOlderThanPort;
import br.com.ecofy.ms_budgeting.core.port.out.DeleteBudgetConsumptionsOlderThanPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class CleanupBudgetsService implements CleanupBudgetsUseCase {

    private final DeleteArchivedBudgetsOlderThanPort deleteArchivedBudgetsOlderThanPort;
    private final DeleteBudgetConsumptionsOlderThanPort deleteBudgetConsumptionsOlderThanPort;

    // Executa a rotina de limpeza: valida o comando, calcula a data de corte (cutoff), remove consumos e budgets arquivados antigos e retorna um resumo da execução.
    @Override
    public CleanupBudgetsResult cleanup(CleanupBudgetsCommand command) {
        if (command == null) throw InvalidFieldException.required("command");
        if (command.runId() == null) throw InvalidFieldException.required("runId");
        if (command.referenceDate() == null) throw InvalidFieldException.required("referenceDate");
        if (command.retentionDays() < 0) throw InvalidFieldException.invalid("retentionDays", "must be >= 0");

        LocalDate cutoff = command.referenceDate().minusDays(command.retentionDays());

        log.info("[CleanupBudgetsService] runId={} referenceDate={} retentionDays={} cutoff={}",
                command.runId(), command.referenceDate(), command.retentionDays(), cutoff);

        long consumptionsDeleted = deleteBudgetConsumptionsOlderThanPort.deleteConsumptionsOlderThan(cutoff);
        long budgetsDeleted = deleteArchivedBudgetsOlderThanPort.deleteArchivedBudgetsOlderThan(cutoff);

        log.info("[CleanupBudgetsService] runId={} consumptionsDeleted={} budgetsDeleted={}",
                command.runId(), consumptionsDeleted, budgetsDeleted);

        return new CleanupBudgetsResult(
                command.runId(),
                command.referenceDate(),
                command.retentionDays(),
                budgetsDeleted,
                consumptionsDeleted
        );
    }

}
