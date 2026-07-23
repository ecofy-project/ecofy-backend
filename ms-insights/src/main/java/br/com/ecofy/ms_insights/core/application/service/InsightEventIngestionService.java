package br.com.ecofy.ms_insights.core.application.service;

import br.com.ecofy.ms_insights.core.application.command.GenerateInsightsCommand;
import br.com.ecofy.ms_insights.core.domain.enums.PeriodGranularity;
import br.com.ecofy.ms_insights.core.domain.exception.IdempotencyViolationException;
import br.com.ecofy.ms_insights.core.port.in.GenerateInsightsUseCase;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

// Processa sinais externos que solicitam a geração de insights.
@Slf4j
@Service
public class InsightEventIngestionService {

    private final GenerateInsightsUseCase generateInsightsUseCase;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    public InsightEventIngestionService(
            GenerateInsightsUseCase generateInsightsUseCase,
            MeterRegistry meterRegistry,
            Clock clock
    ) {
        this.generateInsightsUseCase = Objects.requireNonNull(
                generateInsightsUseCase,
                "generateInsightsUseCase must not be null"
        );
        this.meterRegistry = Objects.requireNonNull(
                meterRegistry,
                "meterRegistry must not be null"
        );
        this.clock = Objects.requireNonNull(
                clock,
                "clock must not be null"
        );
    }

    // Processa o sinal de forma idempotente sem prolongar transações durante integrações externas.
    public void onSignalGenerate(UUID userId) {
        Objects.requireNonNull(userId, "userId must not be null");

        LocalDate today = LocalDate.now(clock);
        LocalDate start = today.withDayOfMonth(1);
        LocalDate end = today;

        String idempotencyKey = buildIdempotencyKey(
                userId,
                start,
                end,
                PeriodGranularity.MONTH
        );

        log.info(
                "[InsightEventIngestionService] - [onSignalGenerate] -> userId={} granularity={} start={} end={} idempotencyKey={}",
                userId,
                PeriodGranularity.MONTH,
                start,
                end,
                idempotencyKey
        );

        try {
            generateInsightsUseCase.generate(
                    new GenerateInsightsCommand(
                            userId,
                            start,
                            end,
                            PeriodGranularity.MONTH,
                            idempotencyKey
                    )
            );
        } catch (IdempotencyViolationException duplicate) {
            meterRegistry.counter(
                    "ecofy.insights.events.duplicate.ignored.total"
            ).increment();
            log.info(
                    "[InsightEventIngestionService] - [onSignalGenerate] -> DUPLICATE ignored userId={} key={}",
                    userId,
                    idempotencyKey
            );
        }
    }

    // Gera uma chave de idempotência estável para o usuário e o período.
    private static String buildIdempotencyKey(
            UUID userId,
            LocalDate start,
            LocalDate end,
            PeriodGranularity g
    ) {
        return "ins-kafka|"
                + userId
                + "|"
                + g
                + "|"
                + start
                + "|"
                + end;
    }
}
