package br.com.ecofy.ms_insights.core.port.in;

import br.com.ecofy.ms_insights.core.application.result.InsightResult;
import br.com.ecofy.ms_insights.core.port.out.PageResult;

import java.util.UUID;

public interface ListInsightsUseCase {
    PageResult<InsightResult> listByUser(UUID userId, int page, int size);
}
