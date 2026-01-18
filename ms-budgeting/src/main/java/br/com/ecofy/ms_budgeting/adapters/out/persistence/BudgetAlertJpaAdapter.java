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
public class BudgetAlertJpaAdapter implements SaveBudgetAlertPort {

    private final BudgetAlertRepository repository;

    // Persiste um BudgetAlert no banco via JPA e retorna o domínio reidratado a partir da entidade salva.
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
