package br.com.ecofy.ms_insights.core.application.service;

import br.com.ecofy.ms_insights.core.application.command.CreateGoalCommand;
import br.com.ecofy.ms_insights.core.application.command.UpdateGoalCommand;
import br.com.ecofy.ms_insights.core.application.result.GoalResult;
import br.com.ecofy.ms_insights.core.domain.Goal;
import br.com.ecofy.ms_insights.core.domain.enums.GoalStatus;
import br.com.ecofy.ms_insights.core.domain.exception.BusinessValidationException;
import br.com.ecofy.ms_insights.core.domain.exception.GoalNotFoundException;
import br.com.ecofy.ms_insights.core.domain.valueobject.Money;
import br.com.ecofy.ms_insights.core.domain.valueobject.UserId;
import br.com.ecofy.ms_insights.core.port.in.GetGoalUseCase;
import br.com.ecofy.ms_insights.core.port.in.ListGoalsUseCase;
import br.com.ecofy.ms_insights.core.port.in.UpdateGoalUseCase;
import br.com.ecofy.ms_insights.core.port.out.LoadGoalsPort;
import br.com.ecofy.ms_insights.core.port.out.SaveGoalPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

// Gerencia a criação, atualização e consulta de metas financeiras.
@Slf4j
@Service
public class GoalService implements
        UpdateGoalUseCase,
        ListGoalsUseCase,
        GetGoalUseCase {

    private final LoadGoalsPort loadGoalsPort;
    private final SaveGoalPort saveGoalPort;
    private final Clock clock;

    public GoalService(
            LoadGoalsPort loadGoalsPort,
            SaveGoalPort saveGoalPort,
            Clock clock
    ) {
        this.loadGoalsPort = Objects.requireNonNull(
                loadGoalsPort,
                "loadGoalsPort must not be null"
        );
        this.saveGoalPort = Objects.requireNonNull(
                saveGoalPort,
                "saveGoalPort must not be null"
        );
        this.clock = Objects.requireNonNull(
                clock,
                "clock must not be null"
        );
    }

    // Cria e persiste uma meta com os valores validados.
    @Transactional
    public GoalResult create(CreateGoalCommand cmd) {
        Objects.requireNonNull(cmd, "cmd must not be null");

        UUID userId = requireNonNull(cmd.userId(), "userId");
        String name = requireNonBlank(cmd.name(), "name");
        String currency = requireCurrency(cmd.currency());
        long targetCents = requireNonNegative(
                cmd.targetCents(),
                "targetCents"
        );

        GoalStatus status = cmd.status() == null
                ? GoalStatus.ACTIVE
                : cmd.status();
        Instant now = Instant.now(clock);

        Goal goal = new Goal(
                UUID.randomUUID(),
                new UserId(userId),
                name,
                Money.ofCents(targetCents, currency),
                status,
                now,
                now
        );

        var saved = saveGoalPort.save(goal);

        log.info(
                "[GoalService] - [create] -> goalId={} userId={} status={} targetCents={} currency={}",
                saved.getId(),
                saved.getUserId().value(),
                saved.getStatus(),
                saved.getTarget().cents(),
                saved.getTarget().currency()
        );

        return toResult(saved);
    }

    // Atualiza os campos informados de uma meta existente.
    @Override
    @Transactional
    public GoalResult update(UpdateGoalCommand cmd) {
        Objects.requireNonNull(cmd, "cmd must not be null");

        UUID goalId = requireNonNull(cmd.goalId(), "goalId");

        Goal current = loadGoalsPort.findById(goalId);
        if (current == null) {
            throw new GoalNotFoundException(
                    "Goal not found for id: " + goalId
            );
        }

        String name = cmd.name() == null
                ? current.getName()
                : requireNonBlank(cmd.name(), "name");
        GoalStatus status = cmd.status() == null
                ? current.getStatus()
                : cmd.status();

        Money target = mergeTarget(current, cmd);

        Instant now = Instant.now(clock);

        Goal updatedDomain = current.withUpdate(
                name,
                target,
                status,
                now
        );
        var saved = saveGoalPort.save(updatedDomain);

        log.info(
                "[GoalService] - [update] -> goalId={} userId={} status={} targetCents={} currency={}",
                saved.getId(),
                saved.getUserId().value(),
                saved.getStatus(),
                saved.getTarget().cents(),
                saved.getTarget().currency()
        );

        return toResult(saved);
    }

    // Lista as metas pertencentes ao usuário.
    @Override
    @Transactional(readOnly = true)
    public List<GoalResult> list(UUID userId) {
        Objects.requireNonNull(userId, "userId must not be null");

        var list = loadGoalsPort.findByUserId(userId)
                .stream()
                .map(GoalService::toResult)
                .toList();

        log.debug(
                "[GoalService] - [list] -> userId={} returned={}",
                userId,
                list.size()
        );
        return list;
    }

    // Resolve a meta pelo identificador ou informa sua ausência.
    @Override
    @Transactional(readOnly = true)
    public GoalResult get(UUID goalId) {
        Objects.requireNonNull(goalId, "goalId must not be null");

        Goal g = loadGoalsPort.findById(goalId);
        if (g == null) {
            throw new GoalNotFoundException(
                    "Goal not found for id: " + goalId
            );
        }

        log.debug(
                "[GoalService] - [get] -> goalId={} userId={}",
                g.getId(),
                g.getUserId().value()
        );
        return toResult(g);
    }

    // Resolve a atualização conjunta do valor e da moeda da meta.
    private static Money mergeTarget(
            Goal current,
            UpdateGoalCommand cmd
    ) {
        Long newCents = cmd.targetCents();
        String newCurrency = cmd.currency();

        if (newCents == null && newCurrency == null) {
            return current.getTarget();
        }

        if (newCents == null || newCurrency == null) {
            throw new BusinessValidationException(
                    "To update target you must provide both targetCents and currency"
            );
        }

        long cents = requireNonNegative(newCents, "targetCents");
        String currency = requireCurrency(newCurrency);

        return Money.ofCents(cents, currency);
    }

    // Converte a meta para o resultado exposto pelo caso de uso.
    public static GoalResult toResult(Goal g) {
        return new GoalResult(
                g.getId(),
                g.getUserId().value(),
                g.getName(),
                g.getTarget().cents(),
                g.getTarget().currency(),
                g.getStatus(),
                g.getCreatedAt(),
                g.getUpdatedAt()
        );
    }

    // Valida valores obrigatórios.
    private static <T> T requireNonNull(T v, String field) {
        if (v == null) {
            throw new BusinessValidationException(
                    field + " must not be null"
            );
        }
        return v;
    }

    // Valida e normaliza valores textuais obrigatórios.
    private static String requireNonBlank(String v, String field) {
        if (v == null || v.trim().isEmpty()) {
            throw new BusinessValidationException(
                    field + " must not be blank"
            );
        }
        return v.trim();
    }

    // Valida e normaliza o código da moeda.
    private static String requireCurrency(String currency) {
        String c = requireNonBlank(currency, "currency").toUpperCase();
        if (c.length() != 3) {
            throw new BusinessValidationException(
                    "Field 'currency' must contain a valid ISO 4217 currency code"
            );
        }
        return c;
    }

    // Valida valores numéricos obrigatórios e não negativos.
    private static long requireNonNegative(Long v, String field) {
        if (v == null) {
            throw new BusinessValidationException(
                    field + " must not be null"
            );
        }
        if (v < 0) {
            throw new BusinessValidationException(
                    field + " must be >= 0"
            );
        }
        return v;
    }
}
