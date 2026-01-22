package br.com.ecofy.ms_insights.core.application.service;

import br.com.ecofy.ms_insights.core.application.result.GoalResult;
import br.com.ecofy.ms_insights.core.application.result.InsightResult;
import br.com.ecofy.ms_insights.core.application.result.InsightsBundleResult;
import br.com.ecofy.ms_insights.core.port.in.GetDashboardInsightsUseCase;
import br.com.ecofy.ms_insights.core.port.out.LoadGoalsPort;
import br.com.ecofy.ms_insights.core.port.out.LoadInsightsPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class DashboardInsightsService implements GetDashboardInsightsUseCase {

    private static final int DEFAULT_INSIGHTS_LIMIT = 20;

    private final LoadInsightsPort loadInsightsPort;
    private final LoadGoalsPort loadGoalsPort;

    // Injeta as portas de leitura de insights e goals para compor o bundle do dashboard via Ports & Adapters.
    public DashboardInsightsService(LoadInsightsPort loadInsightsPort, LoadGoalsPort loadGoalsPort) {
        this.loadInsightsPort = Objects.requireNonNull(loadInsightsPort, "loadInsightsPort must not be null");
        this.loadGoalsPort = Objects.requireNonNull(loadGoalsPort, "loadGoalsPort must not be null");
    }

    // Carrega e agrega dados do dashboard (insights recentes + goals) para um usuário, retornando um bundle com metrics vazias por contrato atual.
    @Override
    @Transactional(readOnly = true)
    public InsightsBundleResult getDashboard(UUID userId) {
        Objects.requireNonNull(userId, "userId must not be null");

        log.debug("[DashboardInsightsService] - [getDashboard] -> userId={} limit={}", userId, DEFAULT_INSIGHTS_LIMIT);

        var insights = loadInsightsPort.findRecentForUser(userId, DEFAULT_INSIGHTS_LIMIT).stream()
                .map(DashboardInsightsService::toInsightResult)
                .toList();

        var goals = loadGoalsPort.findByUserId(userId).stream()
                .map(GoalService::toResult) // mantém seu padrão existente
                .toList();

        log.debug("[DashboardInsightsService] - [getDashboard] -> userId={} insightsReturned={} goalsReturned={}",
                userId, insights.size(), goals.size());

        // metrics ainda não implementado: retorna lista vazia por contrato
        return new InsightsBundleResult(insights, List.of(), goals);
    }

    // Converte Insight (domínio) em InsightResult (DTO de saída) para retorno em APIs/consumidores do caso de uso.
    private static InsightResult toInsightResult(br.com.ecofy.ms_insights.core.domain.Insight i) {
        return new InsightResult(
                i.getId(),
                i.getKey().userId().value(),
                i.getType(),
                i.getScore(),
                i.getTitle(),
                i.getSummary(),
                i.getPayload(),
                i.getCreatedAt()
        );
    }

}
