package br.com.ecofy.ms_insights.core.application.service;

import br.com.ecofy.ms_insights.core.application.command.RebuildInsightsCommand;
import br.com.ecofy.ms_insights.core.domain.enums.InsightType;
import br.com.ecofy.ms_insights.core.port.in.RebuildInsightsUseCase;
import br.com.ecofy.ms_insights.core.port.out.LoadInsightsPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class InsightRebuildService implements RebuildInsightsUseCase {

    private final LoadInsightsPort loadInsightsPort;

    // Injeta a porta de leitura de insights para permitir validações/rebuild sem acoplamento à persistência.
    public InsightRebuildService(LoadInsightsPort loadInsightsPort) {
        this.loadInsightsPort = Objects.requireNonNull(loadInsightsPort, "loadInsightsPort must not be null");
    }

    // Executa um “rebuild” (placeholder) que hoje apenas valida a saúde da porta/repositório e registra métricas de execução sem efeitos colaterais.
    @Override
    @Transactional(readOnly = true)
    public void rebuild(RebuildInsightsCommand cmd) {
        Objects.requireNonNull(cmd, "cmd must not be null");

        Instant startedAt = Instant.now();
        String runId = normalizeRunId(cmd.runId());

        log.info("[InsightRebuildService] - [rebuild] -> START runId={}", runId);

        // Observação: como este serviço ainda é "placeholder", evitamos efeitos colaterais.
        // Em um rebuild real, você:
        // - reprocessaria períodos (ex.: últimos N meses),
        // - recalcularia métricas/trends,
        // - re-publicaria eventos se necessário,
        // - reindexaria em OpenSearch etc.

        try {
            // Métrica rápida e segura: apenas logar um "health check" do repositório/porta.
            UUID sampleUserId = UUID.randomUUID(); // placeholder controlado; substitua por userId do cmd quando existir
            int limit = 20;

            var recent = loadInsightsPort.findRecentForUser(sampleUserId, limit);

            log.info("[InsightRebuildService] - [rebuild] -> runId={} sampleUserId={} checkedRecentLimit={} returned={}",
                    runId, sampleUserId, limit, recent == null ? 0 : recent.size());

            // Se quiser deixar ainda mais útil, loga um resumo do 1o item (sem payload):
            if (recent != null && !recent.isEmpty()) {
                var first = recent.getFirst();
                InsightType type = first.getType();
                log.debug("[InsightRebuildService] - [rebuild] -> runId={} firstInsightId={} type={} score={} createdAt={}",
                        runId, first.getId(), type, first.getScore(), first.getCreatedAt());
            }

            log.info("[InsightRebuildService] - [rebuild] -> END runId={} tookMs={}",
                    runId, (System.currentTimeMillis() - startedAt.toEpochMilli()));

        } catch (Exception ex) {
            log.error("[InsightRebuildService] - [rebuild] -> FAILED runId={} error={}", runId, ex.getMessage(), ex);
            throw ex;
        }
    }

    // Garante um runId não-vazio para rastreabilidade do job, gerando um identificador padrão quando não fornecido.
    private static String normalizeRunId(String runId) {
        if (runId == null || runId.trim().isEmpty()) {
            return "rebuild|" + UUID.randomUUID();
        }
        return runId.trim();
    }

}
