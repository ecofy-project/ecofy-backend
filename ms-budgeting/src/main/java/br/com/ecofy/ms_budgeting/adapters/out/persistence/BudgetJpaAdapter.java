package br.com.ecofy.ms_budgeting.adapters.out.persistence;

import br.com.ecofy.ms_budgeting.adapters.out.persistence.mapper.BudgetMapper;
import br.com.ecofy.ms_budgeting.adapters.out.persistence.repository.BudgetRepository;
import br.com.ecofy.ms_budgeting.core.domain.Budget;
import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetStatus;
import br.com.ecofy.ms_budgeting.core.port.out.LoadBudgetsPort;
import br.com.ecofy.ms_budgeting.core.port.out.SaveBudgetPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
// Centraliza a persistência e a consulta de orçamentos.
public class BudgetJpaAdapter implements SaveBudgetPort, LoadBudgetsPort {

    private final BudgetRepository repo;

    // Persiste o orçamento e retorna o domínio reconstituído.
    @Override
    @Transactional
    public Budget save(Budget budget) {
        Objects.requireNonNull(budget, "budget must not be null");

        String naturalKey = budget.getKey() != null ? budget.getKey().asNaturalKey() : null;

        log.debug(
                "[BudgetJpaAdapter] - [save] -> id={} naturalKey={}",
                budget.getId(),
                naturalKey
        );

        try {
            var saved = repo.save(BudgetMapper.toEntity(budget));
            return BudgetMapper.toDomain(saved);

        } catch (DataIntegrityViolationException ex) {
            String causeMsg = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();

            log.warn(
                    "[BudgetJpaAdapter] - [save] -> Data integrity violation. id={} naturalKey={} cause={}",
                    budget.getId(), naturalKey, causeMsg
            );

            throw ex;
        }
    }

    // Busca o orçamento pelo identificador informado.
    @Override
    @Transactional(readOnly = true)
    public Optional<Budget> findById(UUID id) {
        Objects.requireNonNull(id, "id must not be null");

        log.debug("[BudgetJpaAdapter] - [findById] -> id={}", id);

        return repo.findById(id)
                .map(BudgetMapper::toDomain);
    }

    // Lista os orçamentos pertencentes ao usuário.
    @Override
    @Transactional(readOnly = true)
    public List<Budget> findByUserId(UUID userId) {
        Objects.requireNonNull(userId, "userId must not be null");

        log.debug("[BudgetJpaAdapter] - [findByUserId] -> userId={}", userId);

        var entities = repo.findByUserId(userId);
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }

        return entities.stream()
                .map(BudgetMapper::toDomain)
                .toList();
    }

    // Restringe a ordenação às propriedades permitidas pela API.
    private static final java.util.Map<String, String> SORT_PROPERTIES = java.util.Map.of(
            "createdAt", "createdAt",
            "updatedAt", "updatedAt",
            "periodStart", "periodStart",
            "periodEnd", "periodEnd",
            "status", "status",
            "categoryId", "categoryId"
    );

    // Consulta o histórico paginado de orçamentos pertencentes ao usuário.
    @Override
    @Transactional(readOnly = true)
    public br.com.ecofy.ms_budgeting.core.port.out.PageResult<Budget> findByUserId(
            UUID ownerId, int page, int size, String sortField, boolean ascending) {

        Objects.requireNonNull(ownerId, "ownerId must not be null");

        String property = SORT_PROPERTIES.get(sortField);
        if (property == null) {
            throw new IllegalArgumentException("Sort field not allowed: " + sortField);
        }

        var sort = org.springframework.data.domain.Sort.by(
                ascending ? org.springframework.data.domain.Sort.Direction.ASC
                        : org.springframework.data.domain.Sort.Direction.DESC,
                property);
        var pageRequest = org.springframework.data.domain.PageRequest.of(page, size, sort);

        var result = repo.findByUserId(ownerId, pageRequest);
        return new br.com.ecofy.ms_budgeting.core.port.out.PageResult<>(
                result.getContent().stream().map(BudgetMapper::toDomain).toList(),
                page,
                size,
                result.getTotalElements());
    }

    // Verifica a existência de orçamento com a chave natural informada.
    @Override
    @Transactional(readOnly = true)
    public boolean existsByNaturalKey(String naturalKey) {
        String nk = requireNonBlank(naturalKey, "naturalKey");

        log.debug("[BudgetJpaAdapter] - [existsByNaturalKey] -> naturalKey={}", nk);

        return repo.existsByNaturalKey(nk);
    }

    // Lista os orçamentos disponíveis para processamento.
    @Override
    @Transactional(readOnly = true)
    public List<Budget> findAllActive() {
        log.debug("[BudgetJpaAdapter] - [findAllActive] -> Listando budgets ativos status={}", BudgetStatus.ACTIVE);

        var entities = repo.findByStatus(BudgetStatus.ACTIVE);
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }

        return entities.stream()
                .map(BudgetMapper::toDomain)
                .toList();
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return trimmed;
    }
}
