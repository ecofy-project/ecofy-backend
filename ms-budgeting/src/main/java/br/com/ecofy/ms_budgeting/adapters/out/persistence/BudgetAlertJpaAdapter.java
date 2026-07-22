package br.com.ecofy.ms_budgeting.adapters.out.persistence;

import br.com.ecofy.ms_budgeting.adapters.out.persistence.mapper.BudgetAlertMapper;
import br.com.ecofy.ms_budgeting.adapters.out.persistence.repository.BudgetAlertRepository;
import br.com.ecofy.ms_budgeting.core.domain.BudgetAlert;
import br.com.ecofy.ms_budgeting.core.port.out.SaveBudgetAlertPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
// Centraliza a persistência de alertas de orçamento.
public class BudgetAlertJpaAdapter implements SaveBudgetAlertPort {

    private final BudgetAlertRepository repository;

    // Persiste o alerta e retorna o domínio reconstituído.
    @Override
    @Transactional
    public BudgetAlert save(BudgetAlert alert) {
        Objects.requireNonNull(alert, "alert must not be null");

        log.debug(
                "[BudgetAlertJpaAdapter] - [save] -> budgetId={} consumptionId={} severity={}",
                alert.getBudgetId(), alert.getConsumptionId(), alert.getSeverity()
        );

        var entity = BudgetAlertMapper.toEntity(alert);
        var saved = repository.save(entity);

        return BudgetAlertMapper.toDomain(saved);
    }
}
