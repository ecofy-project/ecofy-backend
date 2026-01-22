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

@Slf4j
@Service
public class GoalService implements UpdateGoalUseCase, ListGoalsUseCase, GetGoalUseCase {

    private final LoadGoalsPort loadGoalsPort;
    private final SaveGoalPort saveGoalPort;
    private final Clock clock;

    // Injeta as portas de leitura/escrita de goals e um Clock para padronizar timestamps e facilitar testes determinísticos.
    public GoalService(LoadGoalsPort loadGoalsPort, SaveGoalPort saveGoalPort, Clock clock) {
        this.loadGoalsPort = Objects.requireNonNull(loadGoalsPort, "loadGoalsPort must not be null");
        this.saveGoalPort = Objects.requireNonNull(saveGoalPort, "saveGoalPort must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    // Cria um novo Goal a partir do comando, validando campos, aplicando defaults e persistindo via port, retornando um GoalResult.
    @Transactional
    public GoalResult create(CreateGoalCommand cmd) {
        Objects.requireNonNull(cmd, "cmd must not be null");

        UUID userId = requireNonNull(cmd.userId(), "userId");
        String name = requireNonBlank(cmd.name(), "name");
        String currency = requireCurrency(cmd.currency());
        long targetCents = requireNonNegative(cmd.targetCents(), "targetCents");

        GoalStatus status = (cmd.status() == null) ? GoalStatus.ACTIVE : cmd.status();
        Instant now = Instant.now(clock);

        Goal goal = new Goal(
                UUID.randomUUID(),
                new UserId(userId),
                name,
                new Money(targetCents, currency),
                status,
                now,
                now
        );

        var saved = saveGoalPort.save(goal);

        log.info("[GoalService] - [create] -> goalId={} userId={} status={} targetCents={} currency={}",
                saved.getId(), saved.getUserId().value(), saved.getStatus(), saved.getTarget().cents(), saved.getTarget().currency());

        return toResult(saved);
    }

    // Atualiza um Goal existente aplicando merge de campos (name/status/target), validando regras de negócio e persistindo o estado atualizado.
    @Override
    @Transactional
    public GoalResult update(UpdateGoalCommand cmd) {
        Objects.requireNonNull(cmd, "cmd must not be null");

        UUID goalId = requireNonNull(cmd.goalId(), "goalId");

        Goal current = loadGoalsPort.findById(goalId);
        if (current == null) {
            throw new GoalNotFoundException("Goal not found: " + goalId);
        }

        String name = (cmd.name() == null) ? current.getName() : requireNonBlank(cmd.name(), "name");
        GoalStatus status = (cmd.status() == null) ? current.getStatus() : cmd.status();

        Money target = mergeTarget(current, cmd);

        Instant now = Instant.now(clock);

        Goal updatedDomain = current.withUpdate(name, target, status, now);
        var saved = saveGoalPort.save(updatedDomain);

        log.info("[GoalService] - [update] -> goalId={} userId={} status={} targetCents={} currency={}",
                saved.getId(), saved.getUserId().value(), saved.getStatus(), saved.getTarget().cents(), saved.getTarget().currency());

        return toResult(saved);
    }

    // Lista os goals de um usuário, convertendo domínio para GoalResult e registrando quantidade retornada.
    @Override
    @Transactional(readOnly = true)
    public List<GoalResult> list(UUID userId) {
        Objects.requireNonNull(userId, "userId must not be null");

        var list = loadGoalsPort.findByUserId(userId).stream()
                .map(GoalService::toResult)
                .toList();

        log.debug("[GoalService] - [list] -> userId={} returned={}", userId, list.size());
        return list;
    }

    // Busca um goal por id, lançando GoalNotFoundException quando inexistente e retornando o GoalResult quando encontrado.
    @Override
    @Transactional(readOnly = true)
    public GoalResult get(UUID goalId) {
        Objects.requireNonNull(goalId, "goalId must not be null");

        Goal g = loadGoalsPort.findById(goalId);
        if (g == null) {
            throw new GoalNotFoundException("Goal not found: " + goalId);
        }

        log.debug("[GoalService] - [get] -> goalId={} userId={}", g.getId(), g.getUserId().value());
        return toResult(g);
    }

    // Faz o merge do target garantindo regra de negócio: para atualizar target é obrigatório enviar targetCents e currency juntos.
    private static Money mergeTarget(Goal current, UpdateGoalCommand cmd) {
        Long newCents = cmd.targetCents();
        String newCurrency = cmd.currency();

        if (newCents == null && newCurrency == null) {
            return current.getTarget();
        }

        if (newCents == null || newCurrency == null) {
            throw new BusinessValidationException("To update target you must provide both targetCents and currency");
        }

        long cents = requireNonNegative(newCents, "targetCents");
        String currency = requireCurrency(newCurrency);

        return new Money(cents, currency);
    }

    // Converte o Goal (domínio) em GoalResult (DTO) para exposição nas camadas de entrada (web/kafka).
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

    // Valida campo obrigatório não-nulo e lança BusinessValidationException com mensagem padronizada.
    private static <T> T requireNonNull(T v, String field) {
        if (v == null) throw new BusinessValidationException(field + " must not be null");
        return v;
    }

    // Valida String obrigatória não-vazia/não-branca e retorna valor normalizado (trim).
    private static String requireNonBlank(String v, String field) {
        if (v == null || v.trim().isEmpty()) {
            throw new BusinessValidationException(field + " must not be blank");
        }
        return v.trim();
    }

    // Valida código de moeda em formato ISO (3 caracteres), normaliza para uppercase e lança BusinessValidationException quando inválido.
    private static String requireCurrency(String currency) {
        String c = requireNonBlank(currency, "currency").toUpperCase();
        if (c.length() != 3) {
            throw new BusinessValidationException("currency must have length 3");
        }
        return c;
    }

    // Valida número obrigatório e não-negativo (>= 0), convertendo Long para long e lançando BusinessValidationException quando inválido.
    private static long requireNonNegative(Long v, String field) {
        if (v == null) throw new BusinessValidationException(field + " must not be null");
        if (v < 0) throw new BusinessValidationException(field + " must be >= 0");
        return v;
    }

}
