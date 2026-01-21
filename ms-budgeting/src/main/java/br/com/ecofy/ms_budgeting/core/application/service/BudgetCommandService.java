package br.com.ecofy.ms_budgeting.core.application.service;

import br.com.ecofy.ms_budgeting.config.BudgetingProperties;
import br.com.ecofy.ms_budgeting.core.application.command.CreateBudgetCommand;
import br.com.ecofy.ms_budgeting.core.application.command.DeleteBudgetCommand;
import br.com.ecofy.ms_budgeting.core.application.command.UpdateBudgetCommand;
import br.com.ecofy.ms_budgeting.core.application.exception.InvalidCurrencyCodeException;
import br.com.ecofy.ms_budgeting.core.application.exception.InvalidFieldException;
import br.com.ecofy.ms_budgeting.core.application.exception.MissingIdempotencyKeyException;
import br.com.ecofy.ms_budgeting.core.application.result.BudgetResult;
import br.com.ecofy.ms_budgeting.core.domain.Budget;
import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetStatus;
import br.com.ecofy.ms_budgeting.core.domain.exception.BudgetAlreadyExistsException;
import br.com.ecofy.ms_budgeting.core.domain.exception.BudgetNotFoundException;
import br.com.ecofy.ms_budgeting.core.domain.exception.IdempotencyViolationException;
import br.com.ecofy.ms_budgeting.core.domain.valueobject.*;
import br.com.ecofy.ms_budgeting.core.port.in.CreateBudgetUseCase;
import br.com.ecofy.ms_budgeting.core.port.in.DeleteBudgetUseCase;
import br.com.ecofy.ms_budgeting.core.port.in.UpdateBudgetUseCase;
import br.com.ecofy.ms_budgeting.core.port.out.DeleteBudgetPort;
import br.com.ecofy.ms_budgeting.core.port.out.IdempotencyPort;
import br.com.ecofy.ms_budgeting.core.port.out.LoadBudgetsPort;
import br.com.ecofy.ms_budgeting.core.port.out.SaveBudgetPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class BudgetCommandService implements CreateBudgetUseCase, UpdateBudgetUseCase, DeleteBudgetUseCase {

    private static final String SCOPE_CREATE = "api:budget:create";
    private static final String SCOPE_UPDATE = "api:budget:update";
    private static final String SCOPE_DELETE = "api:budget:delete";

    private final SaveBudgetPort saveBudgetPort;
    private final LoadBudgetsPort loadBudgetsPort;
    private final DeleteBudgetPort deleteBudgetPort;
    private final IdempotencyPort idempotencyPort;
    private final BudgetingProperties props;
    private final Clock clock;

    public BudgetCommandService(
            SaveBudgetPort saveBudgetPort,
            LoadBudgetsPort loadBudgetsPort,
            DeleteBudgetPort deleteBudgetPort,
            IdempotencyPort idempotencyPort,
            BudgetingProperties props,
            Clock clock
    ) {
        this.saveBudgetPort = Objects.requireNonNull(saveBudgetPort, "saveBudgetPort must not be null");
        this.loadBudgetsPort = Objects.requireNonNull(loadBudgetsPort, "loadBudgetsPort must not be null");
        this.deleteBudgetPort = Objects.requireNonNull(deleteBudgetPort, "deleteBudgetPort must not be null");
        this.idempotencyPort = Objects.requireNonNull(idempotencyPort, "idempotencyPort must not be null");
        this.props = Objects.requireNonNull(props, "props must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    // Cria um budget aplicando idempotência, validações, checagem de duplicidade (naturalKey) e persistência.
    @Override
    @Transactional
    public BudgetResult create(CreateBudgetCommand cmd, String idempotencyKey) {
        if (cmd == null) throw InvalidFieldException.required("cmd");
        acquireIdempotencyOrThrow(idempotencyKey, SCOPE_CREATE);

        validateCreateCommand(cmd);

        Instant now = Instant.now(clock);

        var key = new BudgetKey(
                new UserId(cmd.userId()),
                new CategoryId(cmd.categoryId()),
                new Period(cmd.periodStart(), cmd.periodEnd())
        );

        String naturalKey = key.asNaturalKey();
        if (loadBudgetsPort.existsByNaturalKey(naturalKey)) {
            throw new BudgetAlreadyExistsException(naturalKey);
        }

        Currency currency = parseCurrency(cmd.currency());
        var limit = new Money(cmd.limitAmount(), currency);

        var budget = new Budget(
                UUID.randomUUID(),
                key,
                cmd.periodType(),
                limit,
                cmd.status() != null ? cmd.status() : BudgetStatus.ACTIVE,
                now,
                now
        );

        Budget saved = saveBudgetPort.save(budget);

        log.info(
                "[BudgetCommandService] - [create] -> SUCCESS budgetId={} userId={} categoryId={} naturalKey={}",
                saved.getId(), saved.getKey().userId().value(), saved.getKey().categoryId().value(), naturalKey
        );

        return toResult(saved);
    }

    // Atualiza um budget existente aplicando idempotência e alterando limite/moeda e/ou status quando informado.
    @Override
    @Transactional
    public BudgetResult update(UpdateBudgetCommand cmd, String idempotencyKey) {
        if (cmd == null) throw InvalidFieldException.required("cmd");
        acquireIdempotencyOrThrow(idempotencyKey, SCOPE_UPDATE);

        UUID budgetId = requireNonNull(cmd.budgetId(), "budgetId");
        Budget budget = loadBudgetsPort.findById(budgetId)
                .orElseThrow(() -> new BudgetNotFoundException(budgetId));

        Instant now = Instant.now(clock);

        boolean changed = false;

        if (cmd.newLimitAmount() != null || cmd.currency() != null) {
            if (cmd.newLimitAmount() == null) {
                throw InvalidFieldException.invalid("newLimitAmount", "must be provided when currency is provided");
            }
            Currency currency = cmd.currency() != null
                    ? parseCurrency(cmd.currency())
                    : budget.getLimit().currency();

            budget.updateLimit(new Money(cmd.newLimitAmount(), currency), now);
            changed = true;
        }

        if (cmd.status() != null) {
            budget.updateStatus(cmd.status(), now);
            changed = true;
        }

        if (!changed) {
            log.info("[BudgetCommandService] - [update] -> NOOP budgetId={} (no changes)", budgetId);
            return toResult(budget);
        }

        Budget saved = saveBudgetPort.save(budget);

        log.info(
                "[BudgetCommandService] - [update] -> SUCCESS budgetId={} status={} updatedAt={}",
                saved.getId(), saved.getStatus(), saved.getUpdatedAt()
        );

        return toResult(saved);
    }

    // Remove um budget aplicando idempotência e validando existência antes de efetivar a exclusão.
    @Override
    @Transactional
    public void delete(DeleteBudgetCommand cmd, String idempotencyKey) {
        if (cmd == null) throw InvalidFieldException.required("cmd");
        acquireIdempotencyOrThrow(idempotencyKey, SCOPE_DELETE);

        UUID budgetId = requireNonNull(cmd.budgetId(), "budgetId");

        if (!deleteBudgetPort.existsById(budgetId)) {
            throw new BudgetNotFoundException(budgetId);
        }

        deleteBudgetPort.deleteById(budgetId);

        log.info("[BudgetCommandService] - [delete] -> SUCCESS budgetId={}", budgetId);
    }

    // Adquire (ou falha) a chave de idempotência para o escopo da operação, prevenindo replays concorrentes.
    private void acquireIdempotencyOrThrow(String key, String scope) {
        if (key == null || key.isBlank()) {
            throw new MissingIdempotencyKeyException();
        }

        boolean acquired = idempotencyPort.tryAcquire(
                key.trim(),
                props.idempotency().ttl(),
                scope
        );

        if (!acquired) {
            throw new IdempotencyViolationException(key);
        }
    }

    // Valida os campos obrigatórios e regras de consistência do comando de criação (período e currency inclusos).
    private void validateCreateCommand(CreateBudgetCommand cmd) {
        requireNonNull(cmd.userId(), "userId");
        requireNonNull(cmd.categoryId(), "categoryId");
        requireNonNull(cmd.periodType(), "periodType");
        requireNonNull(cmd.periodStart(), "periodStart");
        requireNonNull(cmd.periodEnd(), "periodEnd");
        requireNonNull(cmd.limitAmount(), "limitAmount");

        if (cmd.periodStart().isAfter(cmd.periodEnd())) {
            throw InvalidFieldException.invalid("period", "periodStart must be <= periodEnd");
        }

        parseCurrency(cmd.currency());
    }

    // Converte o código de moeda para Currency com validação e erro de domínio para códigos inválidos.
    private static Currency parseCurrency(String code) {
        if (code == null || code.isBlank()) {
            throw InvalidFieldException.notBlank("currency");
        }
        try {
            return Currency.getInstance(code.trim());
        } catch (IllegalArgumentException ex) {
            throw new InvalidCurrencyCodeException(code, ex);
        }
    }

    // Garante que um campo obrigatório não seja nulo, lançando InvalidFieldException com o nome do campo.
    private static <T> T requireNonNull(T v, String field) {
        if (v == null) throw InvalidFieldException.required(field);
        return v;
    }

    // Converte o agregado Budget em DTO de resultado para retorno de API/camada de aplicação.
    private static BudgetResult toResult(Budget b) {
        return new BudgetResult(
                b.getId(),
                b.getKey().userId().value(),
                b.getKey().categoryId().value(),
                b.getPeriodType(),
                b.getKey().period().start(),
                b.getKey().period().end(),
                b.getLimit().amount(),
                b.getLimit().currency().getCurrencyCode(),
                b.getStatus(),
                b.getCreatedAt(),
                b.getUpdatedAt()
        );
    }

}
