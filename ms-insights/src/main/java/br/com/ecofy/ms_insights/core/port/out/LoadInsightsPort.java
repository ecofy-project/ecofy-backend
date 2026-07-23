package br.com.ecofy.ms_insights.core.port.out;

import br.com.ecofy.ms_insights.core.domain.Insight;
import br.com.ecofy.ms_insights.core.domain.valueobject.Period;
import br.com.ecofy.ms_insights.core.domain.enums.InsightType;

import java.util.List;
import java.util.UUID;

public interface LoadInsightsPort {
    List<Insight> findRecentForUser(UUID userId, int limit);
    List<Insight> findForUserTypePeriod(UUID userId, InsightType type, Period period);

    PageResult<Insight> findByUserId(UUID userId, int page, int size);
}
